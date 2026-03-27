package com.github.lizhanyin.tfs.actions

import com.github.lizhanyin.tfs.services.TfsService
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile

/**
 * 提交变更操作
 */
class CheckinAction : AnAction() {

    override fun update(e: AnActionEvent) {
        val project = e.project
        val files = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)

        val enabled = project != null &&
                files != null &&
                files.isNotEmpty() &&
                TfsService.getInstance(project).isInitialized

        e.presentation.isEnabledAndVisible = enabled
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val files = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY) ?: return

        // 弹出提交对话框
        val comment = Messages.showInputDialog(
            project,
            "请输入提交说明:",
            "TFS Check In",
            Messages.getQuestionIcon()
        ) ?: return

        if (comment.isBlank()) {
            Messages.showWarningDialog(
                project,
                "提交说明不能为空",
                "TFS Check In"
            )
            return
        }

        val tfsService = TfsService.getInstance(project)
        val commandClient = tfsService.commandClient ?: return

        val filePaths = files.map { it.path }

        // 异步执行提交
        commandClient.checkin(filePaths, comment, true)
            .thenAccept { success ->
                if (success) {
                    // 刷新文件
                    files.forEach { it.refresh(false, false) }
                    showSuccess(project, "提交成功")
                } else {
                    showError(project, "提交失败")
                }
            }
            .exceptionally { e ->
                showError(project, "提交失败: ${e.message}")
                null
            }
    }

    private fun showSuccess(project: Project, message: String) {
        com.intellij.notification.NotificationGroupManager.getInstance()
            .getNotificationGroup("TFS Notifications")
            .createNotification(message, com.intellij.notification.NotificationType.INFORMATION)
            .notify(project)
    }

    private fun showError(project: Project, message: String) {
        com.intellij.notification.NotificationGroupManager.getInstance()
            .getNotificationGroup("TFS Notifications")
            .createNotification(message, com.intellij.notification.NotificationType.ERROR)
            .notify(project)
    }
}
