package com.github.lizhanyin.tfs.actions

import com.github.lizhanyin.tfs.services.TfsService
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project

/**
 * 代码标注 (Annotate/Blame) 操作
 */
class AnnotateAction : AnAction() {

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
        val project = e.project ?: return
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return

        // TODO: 实现 Annotate 功能
        // 这需要使用 IntelliJ 的 VcsAnnotationProvider 接口

        com.intellij.notification.NotificationGroupManager.getInstance()
            .getNotificationGroup("TFS Notifications")
            .createNotification(
                "Annotate 功能正在开发中",
                "此功能将在后续版本中实现",
                com.intellij.notification.NotificationType.INFORMATION
            )
            .notify(project)
    }
}
