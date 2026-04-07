package com.github.lizhanyin.tfs.wizard.dialog

import com.github.lizhanyin.tfs.TfsBundle
import com.github.lizhanyin.tfs.client.ui.controls.workspaces.WorkspaceData
import com.github.lizhanyin.tfs.services.TfsConnectionService
import com.github.lizhanyin.tfs.wizard.ImportProjectContext
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.JBUI
import com.microsoft.tfs.core.clients.versioncontrol.WorkspaceLocation
import com.microsoft.tfs.core.clients.versioncontrol.WorkspaceOptions
import com.microsoft.tfs.core.clients.versioncontrol.WorkspacePermissionProfile
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.WorkingFolder
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace
import com.microsoft.tfs.jni.helpers.LocalHost
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import javax.swing.*
import javax.swing.table.AbstractTableModel

class WorkspaceEditDialog(
    parent: Component?,
    private val context: ImportProjectContext,
    private val isAdd: Boolean,
    private val workspaceData: WorkspaceData,
    private val workspace: Workspace?
) : DialogWrapper(true) {

    private lateinit var nameField: JBTextField
    private lateinit var serverField: JBTextField
    private lateinit var ownerField: JBTextField
    private lateinit var computerField: JBTextField
    private lateinit var locationCombo: ComboBox<String>
    private lateinit var fileTimeCombo: ComboBox<String>
    private lateinit var permissionCombo: ComboBox<String>
    private lateinit var commentArea: JBTextArea

    // 高级区域面板
    private lateinit var advancedPanel: JPanel
    private var advancedVisible = false
    private lateinit var advancedButton: JButton

    // 原始数据对象
    private lateinit var oldWorkspaceData: WorkspaceData

    // 结果
    var createdWorkspace: Workspace? = null

        private set

    init {
        if (isAdd) {
            setTitle(TfsBundle.message("WorkspaceEditDialog.AddWorkspaceDialogTitle"))
        } else {
            setTitle(TfsBundle.message("WorkspaceEditDialog.EditWorkspaceDialogTitleFormat", workspaceData.workspaceDetails.name))
            oldWorkspaceData = WorkspaceData(workspace)
        }
        setOKButtonText(TfsBundle.message("WorkspaceEditDialog.OKButtonText"))
        setCancelButtonText(TfsBundle.message("WorkspaceEditDialog.CancelButtonText"))
        init()
    }

    override fun createSouthPanel(): JComponent {
        val southPanel = super.createSouthPanel()

        // 在按钮行左侧插入高级按钮
        advancedButton = JButton(TfsBundle.message("WorkspaceEditDialog.AdvancedExpand"))
        advancedButton.addActionListener {
            advancedVisible = !advancedVisible
            advancedPanel.isVisible = advancedVisible
            advancedButton.text = if (advancedVisible)
                TfsBundle.message("WorkspaceEditDialog.AdvancedCollapse")
            else
                TfsBundle.message("WorkspaceEditDialog.AdvancedExpand")
            window?.pack()
        }

        // southPanel 默认是 BorderLayout，按钮在 EAST，我们在 WEST 加入高级按钮
        southPanel.add(advancedButton, BorderLayout.WEST)

        return southPanel
    }

    override fun createCenterPanel(): JComponent {
        nameField = JBTextField()

        // 高级区域
        val advancedBuilder = FormBuilder.createFormBuilder()

        serverField = JBTextField(context.serverUrl ?: "").apply { isEnabled = false }
        ownerField = JBTextField(context.username ?: "").apply { isEnabled = false }
        computerField = JBTextField(LocalHost.getShortName()).apply { isEnabled = false }

        locationCombo = ComboBox<String>(Int.MAX_VALUE).apply {
            addItem(TfsBundle.message("WorkspaceDetailsControl.LocationLocalText"))
            addItem(TfsBundle.message("WorkspaceDetailsControl.LocationServerText"))
            selectedIndex = 0
        }

        fileTimeCombo = ComboBox<String>(Int.MAX_VALUE).apply {
            addItem(TfsBundle.message("WorkspaceDetailsControl.FileTimeCurrentText"))
            addItem(TfsBundle.message("WorkspaceDetailsControl.FileTimeCheckinText"))
            selectedIndex = 0
        }

        permissionCombo = ComboBox<String>(Int.MAX_VALUE).apply {
            addItem(TfsBundle.message("WorkspaceDetailsControl.PrivateWorkspace"))
            addItem(TfsBundle.message("WorkspaceDetailsControl.PublicLimitedWorkspace"))
            addItem(TfsBundle.message("WorkspaceDetailsControl.PublicWorkspace"))
            selectedIndex = 0
        }

        commentArea = JBTextArea(3, 40)

        advancedBuilder.addLabeledComponent(TfsBundle.message("WorkspaceDetailsControl.ServerLabelText"), serverField)
        advancedBuilder.addLabeledComponent(TfsBundle.message("WorkspaceDetailsControl.OwnerLabelText"), ownerField)
        advancedBuilder.addLabeledComponent(TfsBundle.message("WorkspaceDetailsControl.ComputerLabelText"), computerField)
        advancedBuilder.addLabeledComponent(TfsBundle.message("WorkspaceDetailsControl.LocationText"), locationCombo)
        advancedBuilder.addLabeledComponent(TfsBundle.message("WorkspaceDetailsControl.FileTimeText"), fileTimeCombo)
        advancedBuilder.addLabeledComponent(TfsBundle.message("WorkspaceDetailsControl.PermissionsLabel"), permissionCombo)
        advancedBuilder.addSeparator()
        advancedBuilder.addComponent(JLabel(TfsBundle.message("WorkspaceDetailsControl.CommentLabelText")))
        advancedBuilder.addComponent(
            JScrollPane(commentArea).apply {
                preferredSize = Dimension(400, 60)
            })

        advancedPanel = advancedBuilder.panel
        advancedPanel.isVisible = false

        // 填充已有数据
        fillExistingData()

        // 工作文件夹表格
        val workingFolderTable = JBTable(WorkingFolderTableModel(workspaceData.workingFolderDataCollection.createWorkingFolders())).apply {
            setShowGrid(false)
            intercellSpacing = Dimension(0, 0)
            rowHeight = JBUI.scale(24)
            columnModel.getColumn(0).preferredWidth = 80
            columnModel.getColumn(1).preferredWidth = 250
            columnModel.getColumn(2).preferredWidth = 250
            isEnabled = false // 只读浏览
        }

        // 主表单
        val mainBuilder = FormBuilder.createFormBuilder()
        mainBuilder.addLabeledComponent(TfsBundle.message("WorkspaceDetailsControl.NameLabelText"), nameField)
        mainBuilder.addComponent(advancedPanel)
        mainBuilder.addSeparator()
        mainBuilder.addComponent(JLabel(TfsBundle.message("WorkspaceEditControl.WorkingFoldersLabelText")))

        val scrollPane = JScrollPane(workingFolderTable).apply {
            preferredSize = Dimension(580, 200)
        }

        val panel = JPanel(BorderLayout())
        panel.add(mainBuilder.panel, BorderLayout.NORTH)
        panel.add(scrollPane, BorderLayout.CENTER)
        return panel
    }

    private fun fillExistingData() {
        val ws = workspaceData.workspaceDetails
        nameField.text = ws.name
        serverField.text = ws.server
        ownerField.text = ws.owner
        computerField.text = ws.computer
        commentArea.text = ws.comment ?: ""

        // 设置位置
        val location = ws.workspaceLocation
        if (location == WorkspaceLocation.LOCAL) {
            locationCombo.selectedIndex = 0
        } else {
            locationCombo.selectedIndex = 1
        }

        // 设置权限
        val profile = ws.permissionProfile
        if (profile != null) {
            when (profile.builtinIndex) {
                WorkspacePermissionProfile.BUILTIN_PROFILE_INDEX_PRIVATE -> permissionCombo.selectedIndex = 0
                WorkspacePermissionProfile.BUILTIN_PROFILE_INDEX_PUBLIC_LIMITED -> permissionCombo.selectedIndex = 1
                WorkspacePermissionProfile.BUILTIN_PROFILE_INDEX_PUBLIC -> permissionCombo.selectedIndex = 2
            }
        }
    }

    override fun doOKAction() {
        if (nameField.text.isBlank()) {
            setErrorText(TfsBundle.message("WorkspaceDetailsControl.NameLabelText") + " cannot be empty")
            return
        }

        // 解析参数
        val location = if (locationCombo.selectedIndex == 0) WorkspaceLocation.LOCAL else WorkspaceLocation.SERVER
        val options = if (fileTimeCombo.selectedIndex == 0) WorkspaceOptions.NONE else WorkspaceOptions.SET_FILE_TO_CHECKIN

        val permissionProfile = when (permissionCombo.selectedIndex) {
            0 -> WorkspacePermissionProfile.getPrivateProfile()
            1 -> WorkspacePermissionProfile.getPublicLimitedProfile()
            else -> WorkspacePermissionProfile.getPublicProfile()
        }

        workspaceData.workspaceDetails.name = nameField.text
        workspaceData.workspaceDetails.comment = commentArea.text
        workspaceData.workspaceDetails.workspaceLocation = location
        workspaceData.workspaceDetails.workspaceOptions = options
        workspaceData.workspaceDetails.permissionProfile = permissionProfile

        val connectionService = ApplicationManager.getApplication().getService(TfsConnectionService::class.java)
        if (isAdd){
            createdWorkspace = connectionService.createWorkspace(context, workspaceData)
        } else {
            workspace?.let { connectionService.updateWorkspace(context, workspaceData, oldWorkspaceData, it) }
        }

        super.doOKAction()
    }

    // ==================== 工作文件夹表格模型 ====================

    private class WorkingFolderTableModel(folders: Array<out WorkingFolder>?) : AbstractTableModel() {
        private val columnNames = arrayOf(
            TfsBundle.message("WorkingFolderDataTable.ColumnNameStatus"),
            TfsBundle.message("WorkingFolderDataTable.ColumnNameServerFolder"),
            TfsBundle.message("WorkingFolderDataTable.ColumnNameLocalFolder")
        )

        private val data: List<WorkingFolder> = folders?.toList() ?: emptyList()

        override fun getRowCount(): Int = data.size

        override fun getColumnCount(): Int = columnNames.size

        override fun getColumnName(column: Int): String = columnNames[column]

        override fun getValueAt(rowIndex: Int, columnIndex: Int): Any {
            val folder = data[rowIndex]
            return when (columnIndex) {
                0 -> if (folder.type == com.microsoft.tfs.core.clients.versioncontrol.soapextensions.WorkingFolderType.CLOAK)
                    TfsBundle.message("WorkingFolderDataTable.TypeCloaked")
                else
                    TfsBundle.message("WorkingFolderDataTable.TypeActive")
                1 -> folder.serverItem ?: ""
                2 -> folder.localItem ?: TfsBundle.message("WorkingFolderDataTable.Cloaked")
                else -> ""
            }
        }
    }
}
