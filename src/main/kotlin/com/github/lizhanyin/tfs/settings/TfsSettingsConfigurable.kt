package com.github.lizhanyin.tfs.settings

import com.github.lizhanyin.tfs.services.TfsService
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.JBUI
import java.awt.FlowLayout
import javax.swing.*

/**
 * TFS 设置界面
 */
class TfsSettingsConfigurable(private val project: Project) : Configurable {

    private val settings: TfsSettings = TfsSettings.getInstance(project)

    // UI 组件
    private val serverUrlField = JBTextField(40)
    private val collectionField = JBTextField(20)
    private val authTypeCombo = ComboBox(TfsSettings.AuthType.values())
    private val usernameField = JBTextField(20)
    private val passwordField = JBPasswordField()
    private val domainField = JBTextField(20)
    private val tfExePathField = JBTextField(40)
    private val autoDetectCheckbox = JCheckBox("自动检测工作区", true)
    private val refreshIntervalField = JBTextField(5)

    // 测试连接按钮
    private val testConnectionButton = JButton("测试连接")
    private var connectionStatusLabel = JLabel("")

    init {
        // 设置布局
        authTypeCombo.addActionListener {
            updateAuthFieldsVisibility()
        }

        testConnectionButton.addActionListener {
            testConnection()
        }

        // 加载现有设置
        reset()
    }

    override fun getDisplayName(): String = "TFS"

    override fun createComponent(): JComponent {
        updateAuthFieldsVisibility()

        val panel = FormBuilder.createFormBuilder()
            .addLabeledComponent("服务器 URL:", serverUrlField)
            .addLabeledComponent("集合名称:", collectionField)
            .addSeparator()
            .addLabeledComponent("认证方式:", authTypeCombo)
            .addLabeledComponent("用户名:", usernameField)
            .addLabeledComponent("密码:", passwordField)
            .addLabeledComponent("域 (NTLM):", domainField)
            .addSeparator()
            .addLabeledComponent("tf.exe 路径:", createTfExePanel())
            .addLabeledComponent("刷新间隔 (秒):", refreshIntervalField)
            .addComponent(autoDetectCheckbox)
            .addSeparator()
            .addComponent(createTestConnectionPanel())
            .addComponentFillVertically(JPanel(), 0)
            .panel

        panel.border = JBUI.Borders.empty(10)
        return panel
    }

    private fun createTfExePanel(): JPanel {
        val panel = JPanel(FlowLayout(FlowLayout.LEFT, 0, 0))
        panel.add(tfExePathField)

        val browseButton = JButton("浏览...")
        browseButton.addActionListener {
            val fileChooser = JFileChooser()
            fileChooser.fileSelectionMode = JFileChooser.FILES_ONLY
            fileChooser.dialogTitle = "选择 tf.exe 文件"
            if (fileChooser.showOpenDialog(panel) == JFileChooser.APPROVE_OPTION) {
                tfExePathField.text = fileChooser.selectedFile.absolutePath
            }
        }
        panel.add(browseButton)

        return panel
    }

    private fun createTestConnectionPanel(): JPanel {
        val panel = JPanel(FlowLayout(FlowLayout.LEFT, 0, 0))
        panel.add(testConnectionButton)
        panel.add(Box.createHorizontalStrut(10))
        panel.add(connectionStatusLabel)
        return panel
    }

    private fun updateAuthFieldsVisibility() {
        val authType = authTypeCombo.selectedItem as TfsSettings.AuthType

        when (authType) {
            TfsSettings.AuthType.NTLM -> {
                domainField.isEnabled = true
                usernameField.isEnabled = true
                passwordField.isEnabled = true
            }
            TfsSettings.AuthType.BASIC -> {
                domainField.isEnabled = false
                usernameField.isEnabled = true
                passwordField.isEnabled = true
            }
            TfsSettings.AuthType.PAT -> {
                domainField.isEnabled = false
                usernameField.isEnabled = false
                passwordField.isEnabled = true
            }
        }
    }

    private fun testConnection() {
        connectionStatusLabel.text = "正在测试..."
        connectionStatusLabel.foreground = UIManager.getColor("Label.foreground")

        // 临时应用设置
        applyToSettings()

        val tfsService = TfsService.getInstance(project)
        tfsService.reinitialize()

        if (!tfsService.isConfigured()) {
            connectionStatusLabel.text = "请先完成配置"
            connectionStatusLabel.foreground = JBColor.RED
            return
        }

        // 异步测试连接
        com.intellij.openapi.application.ApplicationManager.getApplication().executeOnPooledThread {
            try {
                val workspace = tfsService.detectWorkspace().get()
                com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater {
                    if (workspace != null) {
                        connectionStatusLabel.text = "连接成功! 工作区: ${workspace.name()}"
                        connectionStatusLabel.foreground = java.awt.Color.GREEN.darker()
                    } else {
                        connectionStatusLabel.text = "未检测到工作区"
                        connectionStatusLabel.foreground = JBColor.ORANGE
                    }
                }
            } catch (e: Exception) {
                com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater {
                    connectionStatusLabel.text = "连接失败: ${e.message}"
                    connectionStatusLabel.foreground = JBColor.RED
                }
            }
        }
    }

    override fun isModified(): Boolean {
        return serverUrlField.text != settings.serverUrl ||
                collectionField.text != settings.collectionName ||
                authTypeCombo.selectedItem != settings.authType ||
                usernameField.text != settings.username ||
                String(passwordField.password) != settings.password ||
                domainField.text != settings.domain ||
                tfExePathField.text != settings.tfExePath ||
                autoDetectCheckbox.isSelected != settings.autoDetectWorkspace ||
                refreshIntervalField.text != settings.refreshInterval.toString()
    }

    override fun apply() {
        applyToSettings()

        // 重新初始化服务
        val tfsService = TfsService.getInstance(project)
        tfsService.reinitialize()
    }

    private fun applyToSettings() {
        settings.serverUrl = serverUrlField.text.trim()
        settings.collectionName = collectionField.text.trim()
        settings.authType = authTypeCombo.selectedItem as TfsSettings.AuthType
        settings.username = usernameField.text.trim()
        settings.password = String(passwordField.password)
        settings.domain = domainField.text.trim()
        settings.tfExePath = tfExePathField.text.trim()
        settings.autoDetectWorkspace = autoDetectCheckbox.isSelected

        try {
            settings.refreshInterval = refreshIntervalField.text.trim().toIntOrNull() ?: 30
        } catch (e: NumberFormatException) {
            settings.refreshInterval = 30
        }
    }

    override fun reset() {
        serverUrlField.text = settings.serverUrl
        collectionField.text = settings.collectionName
        authTypeCombo.selectedItem = settings.authType
        usernameField.text = settings.username
        passwordField.text = settings.password
        domainField.text = settings.domain
        tfExePathField.text = settings.tfExePath
        autoDetectCheckbox.isSelected = settings.autoDetectWorkspace
        refreshIntervalField.text = settings.refreshInterval.toString()

        updateAuthFieldsVisibility()
    }
}
