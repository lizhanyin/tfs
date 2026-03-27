package com.github.lizhanyin.tfs.startup

import com.intellij.ide.AppLifecycleListener
import com.intellij.openapi.diagnostic.Logger

/**
 * TFS 插件生命周期监听器
 * 在应用启动时立即初始化本地库
 */
class TfsAppLifecycleListener : AppLifecycleListener {

    private val LOG = Logger.getInstance(TfsAppLifecycleListener::class.java)

    override fun appFrameCreated(commandLineArgs: MutableList<String>) {
        LOG.info("TFS: App lifecycle listener - appFrameCreated")
        // 在应用框架创建时立即初始化本地库
        TfsNativeLibraryInitializer.init()
    }

    override fun appStarted() {
        LOG.info("TFS: App lifecycle listener - appStarted")
        // 确保本地库已初始化
        TfsNativeLibraryInitializer.init()
    }
}
