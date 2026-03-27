package com.github.lizhanyin.tfs.settings

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.XmlSerializerUtil

/**
 * TFS 插件配置状态
 */
@State(
    name = "TfsSettings",
    storages = [Storage("tfs.xml")]
)
class TfsSettings : PersistentStateComponent<TfsSettings> {

    // TFS 服务器 URL
    var serverUrl: String = ""

    // 集合名称
    var collectionName: String = "DefaultCollection"

    // 认证类型
    var authType: AuthType = AuthType.NTLM

    // 用户名 (Basic Auth 时使用)
    var username: String = ""

    // 密码 (Basic Auth 时使用) - 注意: 实际应用中应加密存储
    var password: String = ""

    // 域名 (NTLM 时使用)
    var domain: String = ""

    // tf.exe 路径
    var tfExePath: String = ""

    // 自动检测工作区
    var autoDetectWorkspace: Boolean = true

    // 刷新间隔 (秒)
    var refreshInterval: Int = 30

    override fun getState(): TfsSettings = this

    override fun loadState(state: TfsSettings) {
        XmlSerializerUtil.copyBean(state, this)
    }

    /**
     * 认证类型
     */
    enum class AuthType {
        NTLM,       // Windows 集成认证
        BASIC,      // 基本认证
        PAT         // 个人访问令牌 (TFS 2017+)
    }

    /**
     * 检查配置是否完整
     */
    fun isConfigured(): Boolean {
        return serverUrl.isNotBlank()
    }

    /**
     * 获取完整的集合 URL
     */
    fun getCollectionUrl(): String {
        val baseUrl = serverUrl.trimEnd('/')
        return "$baseUrl/$collectionName"
    }

    companion object {
        @JvmStatic
        fun getInstance(project: Project): TfsSettings {
            return project.service<TfsSettings>()
        }
    }
}
