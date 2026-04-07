package com.github.lizhanyin.tfs.wizard.step

import com.github.lizhanyin.tfs.TfsBundle
import com.github.lizhanyin.tfs.client.ui.controls.workspaces.WorkspaceData
import com.github.lizhanyin.tfs.services.TfsConnectionService
import com.github.lizhanyin.tfs.wizard.ImportProjectContext
import com.github.lizhanyin.tfs.wizard.dialog.WorkspaceEditDialog
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.ui.Messages
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.JBTable
import com.intellij.util.containers.toArray
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.JBUI
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace
import com.microsoft.tfs.jni.helpers.LocalHost
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.table.AbstractTableModel
import javax.swing.table.DefaultTableCellRenderer


/**
 * 工作区选择步骤（第三步）
 *
 * UI 布局（参考 CLAUDE.md）：
 * ------------------------------------------
 * |  TFS 工作区                              |
 * |    选择要从中导入项目的 TFS 工作区           |
 * |----------------------------------------|
 * |  [tip ] 工作区说明文字                      |
 * |  [table: 名称|计算机|所有者|注释]           |
 * |  [添加...][编辑...][移除...][刷新列表 ]      |
 * ------------------------------------------
 */
class WorkspaceSelectionStep(context: ImportProjectContext) :
    AbstractWizardStep(STEP_ID, TfsBundle.message("WorkspaceSelectionStep.title"), context, TfsBundle.message("WorkspaceSelectionStep.description")) {

    companion object {
        private const val STEP_ID = "workspace-selection"
    }

    private lateinit var table: JBTable
    private lateinit var tableModel: WorkspaceTableModel
    private lateinit var statusLabel: JBLabel
    private lateinit var editButton: JButton
    private lateinit var removeButton: JButton
    private lateinit var refreshButton: JButton

    private lateinit var workspaces: List<Workspace>

    override fun buildComponent(): JComponent {

        // 标题区域（FormBuilder）
        val builder = FormBuilder.createFormBuilder()
        builder.addSeparator()
        val formPanel = builder.panel

        // 内容区域
        val contentPanel = JPanel(BorderLayout(0, JBUI.scale(8)))
        contentPanel.border = JBUI.Borders.empty(10)

        // 顶部：说明文字
        val tipLabel = JBLabel(TfsBundle.message("WorkspaceSelectionStep.Text"))
        tipLabel.foreground = JBColor.gray
        tipLabel.border = JBUI.Borders.emptyBottom(8)
        contentPanel.add(tipLabel, BorderLayout.NORTH)

        // 中间：工作区表格
        contentPanel.add(createTablePanel(), BorderLayout.CENTER)

        // 底部：按钮
        contentPanel.add(createButtonPanel(), BorderLayout.SOUTH)

        // 整体
        val panel = JPanel(BorderLayout())
        panel.add(formPanel, BorderLayout.NORTH)
        panel.add(contentPanel, BorderLayout.CENTER)

        // 自动加载工作区
        loadWorkspaces()

        return panel
    }

    // ==================== UI 构建 ====================

    private fun createTablePanel(): JComponent {
        tableModel = WorkspaceTableModel()
        table = JBTable(tableModel)

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
        table.setShowGrid(false)
        table.intercellSpacing = Dimension(0, 0)
        table.rowHeight = JBUI.scale(24)
        table.columnModel.getColumn(0).preferredWidth = 150
        table.columnModel.getColumn(1).preferredWidth = 100
        table.columnModel.getColumn(2).preferredWidth = 120
        table.columnModel.getColumn(3).preferredWidth = 200

        table.setDefaultRenderer(Any::class.java, object : DefaultTableCellRenderer() {
            override fun getTableCellRendererComponent(
                table: JTable?, value: Any?, selected: Boolean, focus: Boolean, row: Int, column: Int
            ): Component {
                val c = super.getTableCellRendererComponent(table, value, selected, focus, row, column)
                border = JBUI.Borders.empty(2, 6)
                return c
            }
        })

        table.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.clickCount == 2 && table.selectedRow >= 0) {
                    editWorkspace()
                }
            }
        })

        table.selectionModel.addListSelectionListener {
            val hasSelection = table.selectedRow >= 0
            editButton.isEnabled = hasSelection
            removeButton.isEnabled = hasSelection
        }

        statusLabel = JBLabel(TfsBundle.message("WorkspaceSelectionStep.status.loading"))
        statusLabel.foreground = JBColor.gray

        val scrollPane = JBScrollPane(table)
        scrollPane.preferredSize = Dimension(500, 200)

        val panel = JPanel(BorderLayout())
        panel.add(scrollPane, BorderLayout.CENTER)
        return panel
    }

    private fun createButtonPanel(): JComponent {
        val panel = JPanel(BorderLayout())
        panel.border = JBUI.Borders.emptyTop(8)

        val buttonPanel = JPanel()
        buttonPanel.layout = BoxLayout(buttonPanel, BoxLayout.X_AXIS)

        val addButton = JButton(TfsBundle.message("WorkspacesControl.AddButtonText"))
        addButton.addActionListener { addWorkspace() }
        buttonPanel.add(addButton)
        buttonPanel.add(Box.createHorizontalStrut(JBUI.scale(4)))

        editButton = JButton(TfsBundle.message("WorkspacesControl.EditButtonText")).apply {
            isEnabled = false
            addActionListener { editWorkspace() }
        }
        buttonPanel.add(editButton)
        buttonPanel.add(Box.createHorizontalStrut(JBUI.scale(4)))

        removeButton = JButton(TfsBundle.message("WorkspacesControl.RemoveButtonText")).apply {
            isEnabled = false
            addActionListener { removeWorkspace() }
        }
        buttonPanel.add(removeButton)
        buttonPanel.add(Box.createHorizontalStrut(JBUI.scale(4)))
        refreshButton = JButton(TfsBundle.message("WorkspacesControl.RefreshButtonText"))
        refreshButton.addActionListener { loadWorkspaces() }
        buttonPanel.add(refreshButton)

        panel.add(buttonPanel, BorderLayout.WEST)
        panel.add(statusLabel, BorderLayout.EAST)

        return panel
    }

    // ==================== 工作区操作 ====================

    private fun addWorkspace() {
        val parentWindow = SwingUtilities.getWindowAncestor(table)

        /*
         * Compute a default name for the new workspace.
         */
        val existingWorkspaces = workspaces.toArray(arrayOfNulls(0))
        val defaultWorkspaceName = Workspace.computeNewWorkspaceName(LocalHost.getShortName(), existingWorkspaces)
        val workspaceData = WorkspaceData(context.collection?.collection, defaultWorkspaceName)

        val dialog = WorkspaceEditDialog(parentWindow, context, true, workspaceData, null)
        if (dialog.showAndGet()) {
            val ws = dialog.createdWorkspace
            if (ws != null) {
                loadWorkspaces()
            }
        }
    }

    private fun editWorkspace() {
        val selected = tableModel.getSelectedItem() ?: return
        val selectedName = selected.name

        val parentWindow = SwingUtilities.getWindowAncestor(table)
        val workspaceData = WorkspaceData(selected)
        val dialog = WorkspaceEditDialog(parentWindow, context, false, workspaceData, selected)
        if (dialog.showAndGet()) {
            loadWorkspaces(selectedName)
        }
    }

    private fun removeWorkspace() {
        val selected = tableModel.getSelectedItem() ?: return

        val confirmed = Messages.showYesNoDialog(
            TfsBundle.message("WorkspacesControl.SingleDeleteConfirmDialogText"),
            TfsBundle.message("WorkspacesControl.SingleDeleteConfirmDialogTitle"),
            Messages.getQuestionIcon()
        )

        if (confirmed != Messages.YES) return

        ProgressManager.getInstance().run(object : Task.Backgroundable(
            null, TfsBundle.message("WorkspaceSelectionStep.progress.loading"), true
        ) {
            override fun run(indicator: ProgressIndicator) {
                try {
                    val connectionService = ApplicationManager.getApplication()
                        .getService(TfsConnectionService::class.java)
                    connectionService.deleteWorkspace(context, selected)

                    SwingUtilities.invokeLater { loadWorkspaces() }
                } catch (e: Exception) {
                    SwingUtilities.invokeLater {
                        Messages.showErrorDialog(
                            TfsBundle.message("WorkspaceSelectionStep.error.loadingFailed", e.message ?: ""),
                            TfsBundle.message("WorkspaceSelectionStep.title")
                        )
                    }
                }
            }
        })
    }

    // ==================== 数据加载 ====================

    fun loadWorkspaces(selectWorkspaceName: String? = null) {
        if (!context.isServerConfigured()) {
            Messages.showErrorDialog(
                TfsBundle.message("CollectionSelectionStep.error.configureServerFirst"),
                TfsBundle.message("WorkspaceSelectionStep.title")
            )
            return
        }

        statusLabel.text = TfsBundle.message("WorkspaceSelectionStep.status.loading")
        statusLabel.foreground = JBColor.gray
        table.isEnabled = false

        ProgressManager.getInstance().run(object : Task.Backgroundable(
            null, TfsBundle.message("WorkspaceSelectionStep.progress.loading"), true
        ) {
            override fun run(indicator: ProgressIndicator) {
                indicator.text = TfsBundle.message("WorkspaceSelectionStep.progress.connecting")
                indicator.isIndeterminate = true

                try {
                    val connectionService = ApplicationManager.getApplication()
                        .getService(TfsConnectionService::class.java)
                    workspaces = connectionService.getWorkspaces(context)

                    SwingUtilities.invokeLater {
                        tableModel.setData(workspaces)

                        if (workspaces.isEmpty()) {
                            statusLabel.text = TfsBundle.message("WorkspaceSelectionStep.status.noResults")
                            statusLabel.foreground = JBColor.ORANGE
                        } else {
                            statusLabel.text = TfsBundle.message("WorkspaceSelectionStep.status.loaded", workspaces.size)
                            statusLabel.foreground = JBColor.GRAY
                        }

                        table.isEnabled = true
                        if (table.rowCount > 0) {
                            if (selectWorkspaceName != null) {
                                val row = workspaces.indexOfFirst { it.name == selectWorkspaceName }
                                if (row >= 0) {
                                    table.setRowSelectionInterval(row, row)
                                } else {
                                    table.setRowSelectionInterval(0, 0)
                                }
                            } else {
                                table.setRowSelectionInterval(0, 0)
                            }
                        }
                    }
                } catch (e: Exception) {
                    SwingUtilities.invokeLater {
                        table.isEnabled = true
                        Messages.showErrorDialog(
                            TfsBundle.message("WorkspaceSelectionStep.error.loadingFailed", e.message ?: ""),
                            TfsBundle.message("WorkspaceSelectionStep.title")
                        )
                    }
                }
            }
        })
    }

    // ==================== 表格模型 ====================

    private inner class WorkspaceTableModel : AbstractTableModel() {
        private val columnNames = arrayOf(
            TfsBundle.message("WorkspacesTable.ColumnNameName"),
            TfsBundle.message("WorkspacesTable.ColumnNameComputer"),
            TfsBundle.message("WorkspacesTable.Owner"),
            TfsBundle.message("WorkspacesTable.ColumnNameComment")
        )

        private var data: List<Workspace> = emptyList()

        fun setData(workspaces: List<Workspace>) {
            data = workspaces
            fireTableDataChanged()
        }

        fun getSelectedItem(): Workspace? {
            val row = table.selectedRow
            return if (row in data.indices) data[row] else null
        }

        override fun getRowCount(): Int = data.size

        override fun getColumnCount(): Int = columnNames.size

        override fun getColumnName(column: Int): String = columnNames[column]

        override fun getValueAt(rowIndex: Int, columnIndex: Int): Any {
            val ws = data[rowIndex]
            return when (columnIndex) {
                0 -> ws.name
                1 -> ws.computer
                2 -> ws.ownerDisplayName
                3 -> ws.comment ?: ""
                else -> ""
            }
        }
    }

    // ==================== 步骤接口 ====================

    override fun isComplete(): Boolean = table.selectedRow >= 0

    override fun getPreferredFocusedComponent(): JComponent = if (::table.isInitialized) table else super.getPreferredFocusedComponent()!!

    override fun onFinish(): Boolean {
        val selected = tableModel.getSelectedItem()
        if (selected != null) {
            context.workspaceName = selected.name
            context.workspace = selected
        }
        return true
    }
}
