package com.github.lizhanyin.tfs.wizard.dialog

import com.github.lizhanyin.tfs.TfsBundle
import com.github.lizhanyin.tfs.services.TfsConnectionService
import com.github.lizhanyin.tfs.settings.TfsServerConfiguration
import com.github.lizhanyin.tfs.startup.TfsNativeLibraryInitializer
import com.github.lizhanyin.tfs.wizard.ImportProjectContext
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.JBUI
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.event.ItemEvent
import javax.swing.*

/**
 * 添加 Team Foundation Server 对话框
 */
class AddServerDialog(
    parent: Component,
    @field:Nullable private val editingItem: TfsServerConfiguration.ServerConfig?
) : DialogWrapper(parent, false) {

    private lateinit var nameOrUrlField: JBTextField
    private lateinit var pathField: JBTextField
    private lateinit var portField: JBTextField
    private lateinit var httpRadio: JRadioButton
    private lateinit var httpsRadio: JRadioButton
    private lateinit var previewField: JBTextField
    private lateinit var testButton: JButton
    private lateinit var stopButton: JButton
    private lateinit var statusLabel: JLabel

    @Volatile
    private var testingInProgress = false
    private var connectionTested = false

    private var savedUsername: String? = null
    private var savedPassword: String? = null
    private var savedCredentialType: CredentialsDialog.CredentialType = CredentialsDialog.CredentialType.USER_PASSWORD

    init {
        // 确保本地库已初始化
        TfsNativeLibraryInitializer.init()
        title = if (editingItem == null) TfsBundle.message("AddServerDialog.title.add") else TfsBundle.message("AddServerDialog.title.edit")
        init()
    }

    override fun createCenterPanel(): JComponent {
        val builder = FormBuilder.createFormBuilder()

        // 名称或URL输入
        builder.addComponent(JLabel(TfsBundle.message("AddServerDialog.label.nameOrUrl")))
        nameOrUrlField = JBTextField()
        nameOrUrlField.emptyText.text = TfsBundle.message("AddServerDialog.placeholder.nameOrUrl")
        builder.addComponent(nameOrUrlField)

        // 连接信息面板
        val connectionInfoPanel = JPanel().apply {
            border = BorderFactory.createTitledBorder(TfsBundle.message("AddServerDialog.connectionInfo"))
            layout = BorderLayout(5, 5)
        }

        val connectionBuilder = FormBuilder.createFormBuilder()

        // 路径
        pathField = JBTextField("tfs")
        connectionBuilder.addLabeledComponent(TfsBundle.message("AddServerDialog.label.path"), pathField)

        // 端口
        portField = JBTextField("8080")
        connectionBuilder.addLabeledComponent(TfsBundle.message("AddServerDialog.label.port"), portField)

        // 协议选择
        val protocolPanel = JPanel(FlowLayout(FlowLayout.LEFT, 10, 0))
        httpRadio = JRadioButton("HTTP").apply { isSelected = true }
        httpsRadio = JRadioButton("HTTPS")
        val protocolGroup = ButtonGroup()
        protocolGroup.add(httpRadio)
        protocolGroup.add(httpsRadio)
        protocolPanel.add(httpRadio)
        protocolPanel.add(httpsRadio)
        connectionBuilder.addLabeledComponent(TfsBundle.message("AddServerDialog.label.protocol"), protocolPanel)

        val innerPanel = connectionBuilder.panel
        innerPanel.border = JBUI.Borders.empty(0, 10)
        connectionInfoPanel.add(innerPanel, BorderLayout.CENTER)
        builder.addComponent(connectionInfoPanel)

        // 预览URL
        val previewPanel = JPanel(BorderLayout(5, 0))
        previewField = JBTextField().apply { isEditable = false }
        previewPanel.add(previewField, BorderLayout.CENTER)

        // 测试和停止按钮面板
        val buttonPanel = JPanel(FlowLayout(FlowLayout.LEFT, 5, 0))
        testButton = JButton(TfsBundle.message("AddServerDialog.button.test")).apply { addActionListener { testConnection() } }
        stopButton = JButton(TfsBundle.message("AddServerDialog.button.stop")).apply { isEnabled = false; addActionListener { stopTesting() } }
        buttonPanel.add(testButton)
        buttonPanel.add(stopButton)
        previewPanel.add(buttonPanel, BorderLayout.EAST)
        builder.addLabeledComponent(TfsBundle.message("AddServerDialog.label.preview"), previewPanel)

        // 状态标签
        statusLabel = JLabel(" ")
        statusLabel.foreground = JBColor.GRAY
        builder.addComponent(statusLabel)

        // 如果是编辑模式，填充数据
        editingItem?.let { fillFromConfig(it) }

        // 添加监听器更新预览
        val docListener = object : javax.swing.event.DocumentListener {
            override fun insertUpdate(e: javax.swing.event.DocumentEvent?) = updatePreviewAndResetTest()
            override fun removeUpdate(e: javax.swing.event.DocumentEvent?) = updatePreviewAndResetTest()
            override fun changedUpdate(e: javax.swing.event.DocumentEvent?) = updatePreviewAndResetTest()
        }

        nameOrUrlField.document.addDocumentListener(docListener)
        pathField.document.addDocumentListener(docListener)
        portField.document.addDocumentListener(docListener)

        httpRadio.addItemListener { if (it.stateChange == ItemEvent.SELECTED) updatePreviewAndResetTest() }
        httpsRadio.addItemListener { if (it.stateChange == ItemEvent.SELECTED) updatePreviewAndResetTest() }

        // 初始更新预览
        updatePreview()

        val panel = builder.panel
        panel.border = JBUI.Borders.empty(10)
        panel.preferredSize = Dimension(500, panel.preferredSize.height)
        return panel
    }

    private fun updatePreviewAndResetTest() {
        updatePreview()
        connectionTested = false
    }

    override fun createSouthPanel(): JComponent {
        val panel = JPanel(BorderLayout())

        val leftPanel = JPanel(FlowLayout(FlowLayout.LEFT))
        val credentialsButton = JButton(TfsBundle.message("AddServerDialog.button.credentials")).apply { addActionListener { showCredentialsDialog() } }
        leftPanel.add(credentialsButton)

        val rightPanel = JPanel(FlowLayout(FlowLayout.RIGHT))
        rightPanel.add(createJButtonForAction(okAction))
        rightPanel.add(createJButtonForAction(cancelAction))

        panel.add(leftPanel, BorderLayout.WEST)
        panel.add(rightPanel, BorderLayout.EAST)

        return panel
    }

    /**
     * 从配置填充数据
     */
    private fun fillFromConfig(@NotNull config: TfsServerConfiguration.ServerConfig) {
        var url = config.url ?: return

        try {
            when {
                url.startsWith("https://") -> {
                    httpsRadio.isSelected = true
                    httpRadio.isSelected = false
                    url = url.substring(8)
                }
                url.startsWith("http://") -> {
                    httpRadio.isSelected = true
                    httpsRadio.isSelected = false
                    url = url.substring(7)
                }
            }

            val portIndex = url.indexOf(':')
            val pathIndex = url.indexOf('/')

            when {
                portIndex > 0 -> {
                    val host = url.substring(0, portIndex)
                    nameOrUrlField.text = host

                    if (pathIndex > portIndex) {
                        portField.text = url.substring(portIndex + 1, pathIndex)
                        pathField.text = url.substring(pathIndex + 1)
                    } else {
                        portField.text = url.substring(portIndex + 1)
                    }
                }
                pathIndex > 0 -> {
                    nameOrUrlField.text = url.substring(0, pathIndex)
                    pathField.text = url.substring(pathIndex + 1)
                    portField.text = if (httpRadio.isSelected) "80" else "443"
                }
                else -> nameOrUrlField.text = url
            }

            // 恢复凭证信息
            config.authType?.let {
                try {
                    savedCredentialType = CredentialsDialog.CredentialType.valueOf(it)
                } catch (e: IllegalArgumentException) { }
            }
            savedUsername = config.username
            savedPassword = config.password

        } catch (e: Exception) {
            nameOrUrlField.text = config.url
        }
    }

    /**
     * 更新预览URL
     */
    private fun updatePreview() {
        previewField.text = buildUrl()
    }

    /**
     * 构建完整URL
     */
    @NotNull
    private fun buildUrl(): String {
        val nameOrUrl = nameOrUrlField.text.trim()

        // 如果已经是完整URL，直接返回
        if (nameOrUrl.startsWith("http://") || nameOrUrl.startsWith("https://")) {
            return nameOrUrl
        }

        val protocol = if (httpsRadio.isSelected) "https" else "http"
        val port = portField.text.trim()
        val path = pathField.text.trim()

        val url = StringBuilder()
        url.append(protocol).append("://")
        url.append(nameOrUrl)

        // 只有非标准端口才添加端口号
        if (port.isNotEmpty()) {
            val isStandardPort = (httpsRadio.isSelected && port == "443") ||
                                (httpRadio.isSelected && port == "80")
            if (!isStandardPort) {
                url.append(":").append(port)
            }
        }

        if (path.isNotEmpty()) {
            if (!path.startsWith("/")) {
                url.append("/")
            }
            url.append(path)
        }

        return url.toString()
    }

    /**
     * 显示凭证对话框
     */
    private fun showCredentialsDialog() {
        val dialog = CredentialsDialog(contentPane, buildUrl())
        dialog.setCredentials(savedCredentialType, savedUsername, savedPassword)
        if (dialog.showAndGet()) {
            savedCredentialType = dialog.getCredentialType()
            savedUsername = dialog.getUsername()
            savedPassword = dialog.getPassword()
        }
    }

    /**
     * 测试连接
     */
    private fun testConnection() {
        val url = buildUrl()
        if (url.isEmpty() || url == "http://" || url == "https://") {
            statusLabel.text = TfsBundle.message("AddServerDialog.error.emptyUrl")
            statusLabel.foreground = JBColor.RED
            return
        }

        // 检查是否已输入凭证
        if (savedPassword.isNullOrEmpty()) {
            val result = JOptionPane.showConfirmDialog(
                contentPane,
                TfsBundle.message("AddServerDialog.confirm.noCredentials"),
                TfsBundle.message("AddServerDialog.confirm"),
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
            )
            if (result != JOptionPane.YES_OPTION) {
                return
            }
        }

        statusLabel.text = TfsBundle.message("AddServerDialog.status.testing")
        statusLabel.foreground = JBColor.GRAY
        testButton.isEnabled = false
        stopButton.isEnabled = true
        testingInProgress = true

        ProgressManager.getInstance().run(object : Task.Backgroundable(null, TfsBundle.message("AddServerDialog.progress.testing"), true) {
            private var success = false
            private var errorMessage: String? = null

            override fun run(@NotNull indicator: ProgressIndicator) {
                indicator.text = TfsBundle.message("AddServerDialog.progress.connectingTo", url)
                indicator.isIndeterminate = true

                try {
                    val connectionService = ApplicationManager.getApplication().getService(TfsConnectionService::class.java)

                    // 创建临时上下文
                    val testContext = ImportProjectContext().apply {
                        serverUrl = url
                        if (savedCredentialType == CredentialsDialog.CredentialType.PERSONAL_ACCESS_TOKEN) {
                            authType = ImportProjectContext.AuthType.PAT
                            password = savedPassword
                        } else {
                            authType = ImportProjectContext.AuthType.BASIC
                            username = savedUsername
                            password = savedPassword
                        }
                    }

                    success = connectionService.testConnection(testContext)
                } catch (e: Exception) {
                    errorMessage = e.message
                    e.printStackTrace()
                }
            }

            override fun onSuccess() {
                finishTesting()
                if (success) {
                    statusLabel.text = TfsBundle.message("AddServerDialog.status.success")
                    statusLabel.foreground = JBColor(Color(0, 128, 0), Color(0, 128, 0))
                    connectionTested = true
                } else {
                    statusLabel.text = TfsBundle.message("AddServerDialog.status.failed")
                    statusLabel.foreground = JBColor.RED
                }
            }

            override fun onThrowable(@NotNull error: Throwable) {
                finishTesting()
                error.printStackTrace()
                statusLabel.text = TfsBundle.message("AddServerDialog.status.connectionError", error.message ?: "")
                statusLabel.foreground = JBColor.RED
            }

            override fun onFinished() {
                finishTesting()
            }
        })
    }

    /**
     * 停止测试
     */
    private fun stopTesting() {
        testingInProgress = false
        ProgressManager.getInstance().progressIndicator?.cancel()
        finishTesting()
        statusLabel.text = TfsBundle.message("AddServerDialog.status.cancelled")
        statusLabel.foreground = JBColor.GRAY
    }

    /**
     * 完成测试后恢复按钮状态
     */
    private fun finishTesting() {
        SwingUtilities.invokeLater {
            testButton.isEnabled = true
            stopButton.isEnabled = false
            testingInProgress = false
        }
    }

    override fun doOKAction() {
        val url = buildUrl()
        if (url.isEmpty() || url == "http://" || url == "https://") {
            JOptionPane.showMessageDialog(contentPane, TfsBundle.message("AddServerDialog.error.emptyUrl"), TfsBundle.message("AddServerDialog.hint"), JOptionPane.WARNING_MESSAGE)
            return
        }

        if (!connectionTested) {
            val result = JOptionPane.showConfirmDialog(
                contentPane,
                TfsBundle.message("AddServerDialog.confirm.notTested"),
                TfsBundle.message("AddServerDialog.confirm"),
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
            )
            if (result != JOptionPane.YES_OPTION) {
                return
            }
        }

        super.doOKAction()
    }

    /**
     * 获取服务器配置
     */
    @NotNull
    fun getServerConfig(): TfsServerConfiguration.ServerConfig {
        val url = buildUrl()
        val name = nameOrUrlField.text.trim()

        return TfsServerConfiguration.ServerConfig().apply {
            this.url = url
            this.name = if (name.isEmpty()) url else name
            this.collection = "DefaultCollection"
            this.authType = savedCredentialType.name
            this.username = savedUsername
            this.password = savedPassword
        }
    }
}
