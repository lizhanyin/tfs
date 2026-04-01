package com.github.lizhanyin.tfs.actions

import com.github.lizhanyin.tfs.services.TfsService
import com.github.lizhanyin.tfs.vcs.TfsVcs
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

/**
 * 获取最新版本操作
 */
class GetLatestAction : AnAction() {

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

//        ProgressManager.getInstance().run(
//            object : Task.Backgroundable(project, "Getting latest version from TFS...", true) {
//                override fun run(indicator: ProgressIndicator) {
//                    val tfsService = TfsService.getInstance(project)
//                    val commandClient = tfsService.commandClient ?: return
//
//                    for (file in files) {
//                        if (indicator.isCanceled) break
//
//                        indicator.text = "Getting ${file.name}..."
//                        val path = file.path
//                        val recursive = file.isDirectory
//
//                        val result = commandClient.getLatest(path, recursive).get()
//
//                        if (!result) {
//                            showError(project, "Failed to get latest version for: ${file.name}")
//                        }
//                    }
//
//                    // 刷新文件系统
//                    for (file in files) {
//                        file.refresh(false, true)
//                    }
//                }
//
//                override fun onSuccess() {
//                    showSuccess(project, "Successfully got latest version")
//                }
//            }
//        )
    }

    private fun showSuccess(project: Project, message: String) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("TFS Notifications")
            .createNotification(message, NotificationType.INFORMATION)
            .notify(project)
    }

    private fun showError(project: Project, message: String) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("TFS Notifications")
            .createNotification(message, NotificationType.ERROR)
            .notify(project)
    }
}
