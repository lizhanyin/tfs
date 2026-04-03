package com.github.lizhanyin.tfs.wizard.dialog

import com.github.lizhanyin.tfs.TfsBundle
import com.github.lizhanyin.tfs.wizard.ImportProjectContext
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.JBUI
import com.microsoft.tfs.core.clients.versioncontrol.WorkspaceLocation
import com.microsoft.tfs.core.clients.versioncontrol.WorkspacePermissionProfile
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace
import com.microsoft.tfs.jni.helpers.LocalHost
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import javax.swing.*

class WorkspaceEditDialog(
    parent: Component?,
    private val context: ImportProjectContext,
    private val existingWorkspace: Workspace?
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

    // 结果
    var createdWorkspace: Workspace? = null
        private set

    init {
        if (existingWorkspace != null) {
            setTitle(TfsBundle.message("WorkspaceEditDialog.EditWorkspaceDialogTitleFormat", existingWorkspace.name))
        } else {
            setTitle(TfsBundle.message("WorkspaceEditDialog.AddWorkspaceDialogTitle"))
        }
        init()
    }

    override fun createCenterPanel(): JComponent {
        nameField = JBTextField()

        // 高级区域
        val advancedBuilder = FormBuilder.createFormBuilder()

        serverField = JBTextField(context.serverUrl ?: "").apply { isEnabled = false }
        ownerField = JBTextField(context.username ?: "").apply { isEnabled = false }
        computerField = JBTextField(LocalHost.getShortName()).apply { isEnabled = false }

        locationCombo = ComboBox<String>().apply {
            addItem(TfsBundle.message("WorkspaceDetailsControl.LocationLocalText"))
            addItem(TfsBundle.message("WorkspaceDetailsControl.LocationServerText"))
            selectedIndex = 0
        }

        fileTimeCombo = ComboBox<String>().apply {
            addItem(TfsBundle.message("WorkspaceDetailsControl.FileTimeCurrentText"))
            addItem(TfsBundle.message("WorkspaceDetailsControl.FileTimeCheckinText"))
            selectedIndex = 0
        }

        permissionCombo = ComboBox<String>().apply {
            addItem(TfsBundle.message("WorkspaceDetailsControl.PrivateWorkspace"))
            addItem(TfsBundle.message("WorkspaceDetailsControl.PublicLimitedWorkspace"))
            addItem(TfsBundle.message("WorkspaceDetailsControl.PublicWorkspace"))
            selectedIndex = 0
        }

        commentArea = JBTextArea(3, 40)

        advancedBuilder.addLabeledComponent(
            TfsBundle.message("WorkspaceDetailsControl.ServerLabelText"), serverField)
        advancedBuilder.addLabeledComponent(
            TfsBundle.message("WorkspaceDetailsControl.OwnerLabelText"), ownerField)
        advancedBuilder.addLabeledComponent(
            TfsBundle.message("WorkspaceDetailsControl.ComputerLabelText"), computerField)
        advancedBuilder.addLabeledComponent(
            TfsBundle.message("WorkspaceDetailsControl.LocationText"), locationCombo)
        advancedBuilder.addLabeledComponent(
            TfsBundle.message("WorkspaceDetailsControl.FileTimeText"), fileTimeCombo)
        advancedBuilder.addLabeledComponent(
            TfsBundle.message("WorkspaceDetailsControl.PermissionsLabel"), permissionCombo)
        advancedBuilder.addSeparator()
        advancedBuilder.addLabeledComponent(
            TfsBundle.message("WorkspaceDetailsControl.CommentLabelText"),
            JScrollPane(commentArea).apply {
                preferredSize = Dimension(400, 60)
            })

        advancedPanel = advancedBuilder.panel
        advancedPanel.isVisible = false

        // 高级切换按钮
        val advancedButton = JButton(TfsBundle.message("WorkspaceEditDialog.AdvancedExpand"))
        advancedButton.addActionListener {
            advancedVisible = !advancedVisible
            advancedPanel.isVisible = advancedVisible
            advancedButton.text = if (advancedVisible)
                TfsBundle.message("WorkspaceEditDialog.AdvancedCollapse")
            else
                TfsBundle.message("WorkspaceEditDialog.AdvancedExpand")
            advancedPanel.revalidate()
            advancedPanel.repaint()
            window?.pack()
        }

        // 填充已有数据
        fillExistingData()

        // 主表单
        val mainBuilder = FormBuilder.createFormBuilder()
        mainBuilder.addLabeledComponent(
            TfsBundle.message("WorkspaceDetailsControl.NameLabelText"), nameField)
        mainBuilder.addComponent(advancedButton)
        mainBuilder.addComponent(advancedPanel)

        val panel = JPanel(BorderLayout())
        panel.add(mainBuilder.panel, BorderLayout.NORTH)
        return panel
    }

    private fun fillExistingData() {
        val ws = existingWorkspace ?: return
        nameField.text = ws.name
        nameField.isEnabled = false // 编辑模式不可改名称

        serverField.text = ws.serverName
        ownerField.text = ws.ownerDisplayName
        computerField.text = ws.computer
        commentArea.text = ws.comment ?: ""

        // 设置位置
        val location = ws.location
        if (location == WorkspaceLocation.LOCAL) {
            locationCombo.selectedIndex = 0
        } else {
            locationCombo.selectedIndex = 1
        }

        // 设置权限
        val profile = ws.permissionsProfile
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

        val permissionProfile = when (permissionCombo.selectedIndex) {
            0 -> WorkspacePermissionProfile.getPrivateProfile()
            1 -> WorkspacePermissionProfile.getPublicLimitedProfile()
            else -> WorkspacePermissionProfile.getPublicProfile()
        }

        createdWorkspace = null

        super.doOKAction()
    }
}
