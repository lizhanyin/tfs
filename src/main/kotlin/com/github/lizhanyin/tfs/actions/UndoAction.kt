package com.github.lizhanyin.tfs.actions

import com.github.lizhanyin.tfs.services.TfsService
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile

/**
 * 撤销变更操作
 */
class UndoAction : AnAction() {

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

        // 确认对话框
        val result = Messages.showYesNoDialog(
            project,
            "确定要撤销所选文件的变更吗？\n此操作将还原到服务器版本。",
            "TFS Undo",
            Messages.getQuestionIcon()
        )

        if (result != Messages.YES) {
            return
        }

//        val tfsService = TfsService.getInstance(project)
//        val commandClient = tfsService.commandClient ?: return
//
//        val filePaths = files.map { it.path }
//
//        // 异步执行撤销
//        commandClient.undo(filePaths, true)
//            .thenAccept { success ->
//                if (success) {
//                    // 刷新文件
//                    files.forEach { it.refresh(false, true) }
//                    showSuccess(project, "撤销成功")
//                } else {
//                    showError(project, "撤销失败")
//                }
//            }
//            .exceptionally { e ->
//                showError(project, "撤销失败: ${e.message}")
//                null
//            }
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
