package com.github.lizhanyin.tfs.startup

import com.github.lizhanyin.tfs.TFSClientPlugin
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

        // 初始化插件
        TFSClientPlugin.getDefault(project).start()
    }
}
