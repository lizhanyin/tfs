package com.github.lizhanyin.tfs.startup

import com.github.lizhanyin.tfs.services.TfsService
import com.github.lizhanyin.tfs.settings.TfsSettings
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

/**
 * TFS 项目启动活动
 * 在项目加载后初始化 TFS 服务
 */
class TfsStartupActivity : ProjectActivity {

    private val LOG = Logger.getInstance(TfsStartupActivity::class.java)

    override suspend fun execute(project: Project) {
        LOG.info("TFS plugin starting for project: ${project.name}")

        // 初始化 TFS 本地库
        TfsNativeLibraryInitializer.init()

        val settings = TfsSettings.getInstance(project)
        val tfsService = TfsService.getInstance(project)

        if (settings.isConfigured()) {
            // 初始化服务
            tfsService.initialize()

            // 检测工作区
            if (settings.autoDetectWorkspace) {
                tfsService.detectWorkspace()
                    .thenAccept { workspace ->
                        if (workspace != null) {
                            LOG.info("TFS workspace detected: ${workspace.name()}")

                            // 通知用户
                            NotificationGroupManager.getInstance()
                                .getNotificationGroup("TFS Notifications")
                                .createNotification(
                                    "TFS 工作区已连接",
                                    "工作区: ${workspace.name()}\n服务器: ${workspace.serverUrl()}",
                                    NotificationType.INFORMATION
                                )
                                .notify(project)
                        }
                    }
                    .exceptionally { e ->
                        LOG.warn("Failed to detect TFS workspace", e)
                        null
                    }
            }
        } else {
            LOG.info("TFS settings not configured")
        }
    }
}
