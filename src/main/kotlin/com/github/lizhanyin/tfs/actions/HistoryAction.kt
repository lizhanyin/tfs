package com.github.lizhanyin.tfs.actions

import com.github.lizhanyin.tfs.services.TfsService
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

/**
 * 查看历史记录操作
 */
class HistoryAction : AnAction() {

    override fun update(e: AnActionEvent) {
        val project = e.project
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE)

        val enabled = project != null &&
                file != null &&
                !file.isDirectory &&
                TfsService.getInstance(project).isInitialized

        e.presentation.isEnabledAndVisible = enabled
    }

    override fun actionPerformed(e: AnActionEvent) {
//        val project = e.project ?: return
//        val file = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
//
//        val tfsService = TfsService.getInstance(project)
//        val commandClient = tfsService.commandClient ?: return
//
//        // 获取历史记录
//        commandClient.getHistory(file.path, false, 50)
//            .thenAccept { history ->
//                // TODO: 显示历史记录对话框
//                showHistoryDialog(project, file, history)
//            }
//            .exceptionally { e ->
//                showError(project, "获取历史记录失败: ${e.message}")
//                null
//            }
    }

    private fun showHistoryDialog(project: Project, file: VirtualFile, history: String) {
        // 简单显示，实际应该创建一个专门的对话框
        com.intellij.openapi.ui.Messages.showMultilineInputDialog(
            project,
            "文件: ${file.name}",
            "TFS History",
            history,
            null,
            null
        )
    }

    private fun showError(project: Project, message: String) {
        com.intellij.notification.NotificationGroupManager.getInstance()
            .getNotificationGroup("TFS Notifications")
            .createNotification(message, com.intellij.notification.NotificationType.ERROR)
            .notify(project)
    }
}
