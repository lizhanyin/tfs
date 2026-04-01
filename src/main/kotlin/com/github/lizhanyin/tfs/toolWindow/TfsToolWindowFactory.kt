package com.github.lizhanyin.tfs.toolWindow

import com.github.lizhanyin.tfs.services.TfsService
import com.github.lizhanyin.tfs.settings.TfsSettings
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.content.ContentFactory
import java.awt.BorderLayout
import java.awt.FlowLayout
import javax.swing.JButton
import javax.swing.SwingConstants

/**
 * TFS 工具窗口工厂
 */
class TfsToolWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val panel = TfsToolWindowPanel(project)
        val content = ContentFactory.getInstance().createContent(panel, null, false)
        toolWindow.contentManager.addContent(content)
    }

    override fun shouldBeAvailable(project: Project): Boolean = true

    /**
     * TFS 工具窗口面板
     */
    private class TfsToolWindowPanel(private val project: Project) : JBPanel<JBPanel<*>>(BorderLayout()) {

        private val statusLabel = JBLabel("未连接", SwingConstants.CENTER)
        private val workspaceLabel = JBLabel("", SwingConstants.CENTER)
        private val refreshButton = JButton("刷新")

        init {
            setupUI()
            updateStatus()
        }

        private fun setupUI() {
            // 顶部状态面板
            val topPanel = JBPanel<JBPanel<*>>(BorderLayout())
            topPanel.add(statusLabel, BorderLayout.CENTER)
            add(topPanel, BorderLayout.NORTH)

            // 中间内容面板
            val centerPanel = JBPanel<JBPanel<*>>(BorderLayout())
            centerPanel.add(workspaceLabel, BorderLayout.CENTER)
            add(centerPanel, BorderLayout.CENTER)

            // 底部按钮面板
            val buttonPanel = JBPanel<JBPanel<*>>(FlowLayout(FlowLayout.CENTER))
            refreshButton.addActionListener { updateStatus() }
            buttonPanel.add(refreshButton)
            buttonPanel.add(JButton("设置").apply {
                addActionListener {
                    // 打开设置
                    com.intellij.openapi.options.ShowSettingsUtil.getInstance()
                        .showSettingsDialog(project, "TFS")
                }
            })
            add(buttonPanel, BorderLayout.SOUTH)
        }

        private fun updateStatus() {
            val tfsService = TfsService.getInstance(project)
            val settings = TfsSettings.getInstance(project)

            if (!settings.isConfigured()) {
                statusLabel.text = "未配置 - 请在设置中配置 TFS 连接"
                workspaceLabel.text = ""
                return
            }

            if (!tfsService.isInitialized) {
                statusLabel.text = "正在初始化..."
                tfsService.initialize()
            }

            val workspace = tfsService.currentWorkspace
            if (workspace != null) {
                statusLabel.text = "已连接"
                workspaceLabel.text = "<html><b>工作区:</b> ${workspace.name()}<br><b>服务器:</b> ${workspace.serverUrl()}</html>"
            } else {
                statusLabel.text = "未检测到工作区"
                workspaceLabel.text = "请确保项目目录在 TFS 工作区映射中"

//                // 尝试检测工作区
//                tfsService.detectWorkspace()
//                    .thenAccept { ws ->
//                        if (ws != null) {
//                            com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater {
//                                statusLabel.text = "已连接"
//                                workspaceLabel.text = "<html><b>工作区:</b> ${ws.name()}<br><b>服务器:</b> ${ws.serverUrl()}</html>"
//                            }
//                        }
//                    }
            }
        }
    }
}
