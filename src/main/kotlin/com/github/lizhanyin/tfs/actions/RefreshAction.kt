package com.github.lizhanyin.tfs.actions

import com.github.lizhanyin.tfs.services.TfsService
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.VcsDirtyScopeManager

/**
 * 刷新状态操作
 */
class RefreshAction : AnAction() {

    override fun update(e: AnActionEvent) {
        val project = e.project
        val enabled = project != null && TfsService.getInstance(project).isInitialized
        e.presentation.isEnabledAndVisible = enabled
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        // 标记所有文件为脏，触发变更检测
        val dirtyScopeManager = VcsDirtyScopeManager.getInstance(project)
        dirtyScopeManager.markEverythingDirty()

        com.intellij.notification.NotificationGroupManager.getInstance()
            .getNotificationGroup("TFS Notifications")
            .createNotification(
                "TFS 状态已刷新",
                com.intellij.notification.NotificationType.INFORMATION
            )
            .notify(project)
    }
}
