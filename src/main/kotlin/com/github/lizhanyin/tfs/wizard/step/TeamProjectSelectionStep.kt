package com.github.lizhanyin.tfs.wizard.step

import com.github.lizhanyin.tfs.services.TfsConnectionService
import com.github.lizhanyin.tfs.wizard.ImportProjectContext
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.ui.CollectionComboBoxModel
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Color
import java.awt.FlowLayout
import javax.swing.JButton
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingUtilities

/**
 * 团队项目选择步骤
 */
class TeamProjectSelectionStep(context: ImportProjectContext) :
    AbstractWizardStep(STEP_ID, "选择团队项目", context) {

    companion object {
        private const val STEP_ID = "team-project-selection"
    }

    private lateinit var teamProjectComboBox: JComboBox<String>
    private lateinit var statusLabel: JBLabel
    private lateinit var refreshButton: JButton
    private lateinit var serverInfoLabel: JLabel

    private val teamProjects = mutableListOf<String>()

    override fun buildComponent(): JComponent {
        val builder = FormBuilder.createFormBuilder()

        // 服务器信息
        serverInfoLabel = JLabel(" ")
        updateServerInfo()
        builder.addLabeledComponent("已连接到:", serverInfoLabel)
        builder.addSeparator()
        builder.addLabeledComponent("团队项目:", createTeamProjectPanel())
        builder.addSeparator()
        builder.addComponent(createStatusPanel())

        val panel = builder.panel
        panel.border = JBUI.Borders.empty(10)

        return panel
    }

    /**
     * 更新服务器信息显示
     */
    private fun updateServerInfo() {
        val info = buildString {
            append("<html><b>")
            append(context.serverUrl)
            val collection = context.collectionName
            if (!collection.isNullOrEmpty()) {
                append("/").append(collection)
            }
            append("</b></html>")
        }
        if (::serverInfoLabel.isInitialized) {
            serverInfoLabel.text = info
        }
    }

    /**
     * 创建团队项目选择面板
     */
    private fun createTeamProjectPanel(): JComponent {
        val panel = JPanel(BorderLayout(5, 0))

        teamProjectComboBox = JComboBox()
        teamProjectComboBox.isEnabled = false
        updateTeamProjectComboBox()
        panel.add(teamProjectComboBox, BorderLayout.CENTER)

        val buttonPanel = JPanel(FlowLayout(FlowLayout.LEFT, 5, 0))

        refreshButton = JButton("刷新")
        refreshButton.addActionListener { loadTeamProjects() }
        buttonPanel.add(refreshButton)

        panel.add(buttonPanel, BorderLayout.EAST)

        return panel
    }

    /**
     * 创建状态面板
     */
    private fun createStatusPanel(): JComponent {
        val panel = JPanel(BorderLayout())
        statusLabel = JBLabel("点击\"刷新\"加载团队项目列表")
        panel.add(statusLabel, BorderLayout.CENTER)
        return panel
    }

    /**
     * 加载团队项目列表
     */
    private fun loadTeamProjects() {
        if (!context.isServerConfigured()) {
            statusLabel.text = "请先配置服务器连接"
            statusLabel.foreground = Color.RED
            return
        }

        statusLabel.text = "正在加载团队项目..."
        statusLabel.foreground = Color.BLACK
        refreshButton.isEnabled = false
        teamProjectComboBox.isEnabled = false

        ProgressManager.getInstance().run(object : Task.Backgroundable(null, "加载团队项目", true) {
            override fun run(indicator: ProgressIndicator) {
                indicator.text = "正在连接到 TFS 服务器..."
                indicator.isIndeterminate = true

                try {
                    val connectionService = ApplicationManager.getApplication().getService(TfsConnectionService::class.java)
                    val projects = connectionService.getTeamProjects(context)

                    SwingUtilities.invokeLater {
                        teamProjects.clear()
                        teamProjects.addAll(projects)
                        updateTeamProjectComboBox()

                        if (projects.isEmpty()) {
                            statusLabel.text = "未找到团队项目"
                            statusLabel.foreground = Color.ORANGE
                        } else {
                            statusLabel.text = "已加载 ${projects.size} 个团队项目"
                            statusLabel.foreground = Color(0, 128, 0)
                        }

                        refreshButton.isEnabled = true
                        teamProjectComboBox.isEnabled = true
                    }

                } catch (e: Exception) {
                    SwingUtilities.invokeLater {
                        statusLabel.text = "加载失败: ${e.message}"
                        statusLabel.foreground = Color.RED
                        refreshButton.isEnabled = true
                    }
                }
            }
        })
    }

    /**
     * 更新团队项目下拉框
     */
    private fun updateTeamProjectComboBox() {
        teamProjectComboBox.model = CollectionComboBoxModel(teamProjects)
        if (teamProjects.isNotEmpty()) {
            teamProjectComboBox.selectedIndex = 0
        }
    }

    override fun isComplete(): Boolean = teamProjectComboBox.selectedItem != null

    override fun getPreferredFocusedComponent(): JComponent = teamProjectComboBox

    override fun onFinish(): Boolean {
        val selected = teamProjectComboBox.selectedItem as? String
        if (selected != null) {
            context.teamProject = selected
        }
        return true
    }
}
