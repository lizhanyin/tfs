package com.github.lizhanyin.tfs.wizard.step

import com.github.lizhanyin.tfs.settings.TfsServerConfiguration
import com.github.lizhanyin.tfs.wizard.ImportProjectContext
import com.github.lizhanyin.tfs.wizard.dialog.ManageServersDialog
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.CollectionComboBoxModel
import com.intellij.ui.JBColor
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.Window
import java.awt.event.ItemEvent
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingUtilities

/**
 * 服务器选择步骤
 * 第一步：选择要连接的服务器
 */
class ServerSelectionStep(context: ImportProjectContext) :
    AbstractWizardStep(STEP_ID, "服务器选择", context) {

    companion object {
        private const val STEP_ID = "server-selection"
    }

    private lateinit var serverComboBox: ComboBox<TfsServerConfiguration.ServerConfig>
    private lateinit var statusLabel: JLabel

    override fun buildComponent(): JComponent {
        // 首先初始化 statusLabel，因为 createServerPanel() 会用到它
        statusLabel = JLabel(" ")

        val builder = FormBuilder.createFormBuilder()

        // 步骤说明
        builder.addComponent(JLabel("选择要连接到的服务器"))
        builder.addSeparator()

        // 服务器选择面板
        builder.addLabeledComponent("TFS服务器:", createServerPanel())

        builder.addComponent(statusLabel)

        val formPanel = builder.panel
        formPanel.border = JBUI.Borders.empty(10)

        // 使用 BorderLayout 将内容放在顶部
        val panel = JPanel(BorderLayout())
        panel.add(formPanel, BorderLayout.NORTH)

        return panel
    }

    /**
     * 创建服务器选择面板
     */
    private fun createServerPanel(): JPanel {
        val panel = JPanel(BorderLayout(5, 0))

        // 服务器下拉框
        serverComboBox = ComboBox()
        serverComboBox.preferredSize = Dimension(300, serverComboBox.preferredSize.height)
        loadServers()

        serverComboBox.addItemListener { e ->
            if (e.stateChange == ItemEvent.SELECTED) {
                onServerSelected()
            }
        }

        panel.add(serverComboBox, BorderLayout.CENTER)

        // 服务器管理按钮
        val manageButton = JButton("服务器...")
        manageButton.addActionListener { showManageServersDialog() }
        panel.add(manageButton, BorderLayout.EAST)

        return panel
    }

    /**
     * 加载已保存的服务器
     */
    private fun loadServers() {
        val servers = TfsServerConfiguration.getInstance().servers
        serverComboBox.model = CollectionComboBoxModel(servers)

        // 如果有服务器，默认选中第一个
        if (servers.isNotEmpty()) {
            serverComboBox.selectedIndex = 0
            onServerSelected()
        }
    }

    /**
     * 服务器选中时
     */
    private fun onServerSelected() {
        val selected = selectedServer
        if (selected != null) {
            // 保存到上下文
            context.serverUrl = selected.url

            // 恢复凭证信息
            if (selected.authType.isNotEmpty()) {
                try {
                    context.authType = ImportProjectContext.AuthType.valueOf(selected.authType)
                } catch (e: IllegalArgumentException) {
                    context.authType = ImportProjectContext.AuthType.NTLM
                }
            }

            context.username = selected.username
            context.password = selected.password
            context.domain = selected.domain

            // 显示连接信息
            val credStatus = if (!selected.password.isNullOrEmpty()) "（已保存凭证）" else ""
            statusLabel.text = credStatus
            statusLabel.foreground = JBColor.GRAY
        } else {
            statusLabel.text = " "
        }
    }

    /**
     * 显示服务器管理对话框
     */
    private fun showManageServersDialog() {
        val parentWindow = SwingUtilities.getWindowAncestor(serverComboBox)
        val dialog = ManageServersDialog(parentWindow)
        dialog.showAndGet()

        // 重新加载服务器列表
        val previousSelected = selectedServer
        loadServers()

        // 尝试恢复之前选中的服务器
        if (previousSelected != null) {
            for (i in 0 until serverComboBox.itemCount) {
                val server = serverComboBox.getItemAt(i)
                if (server.url == previousSelected.url) {
                    serverComboBox.selectedIndex = i
                    return
                }
            }
        }
    }

    /**
     * 获取选中的服务器
     */
    private val selectedServer: TfsServerConfiguration.ServerConfig?
        get() = serverComboBox.selectedItem as? TfsServerConfiguration.ServerConfig

    override fun isComplete(): Boolean {
        val selected = selectedServer
        return selected != null && selected.url.isNotEmpty()
    }

    override fun getPreferredFocusedComponent(): JComponent = serverComboBox

    override fun onFinish(): Boolean {
        val selected = selectedServer
        if (selected != null) {
            context.serverUrl = selected.url
            context.collectionName = selected.collection

            if (selected.authType.isNotEmpty()) {
                try {
                    context.authType = ImportProjectContext.AuthType.valueOf(selected.authType)
                } catch (e: IllegalArgumentException) {
                    // 使用默认值
                }
            }

            context.username = selected.username
            context.password = selected.password
            context.domain = selected.domain
        }
        return true
    }
}
