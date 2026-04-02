package com.github.lizhanyin.tfs.wizard

import com.github.lizhanyin.tfs.TfsBundle
import com.github.lizhanyin.tfs.client.catalog.CrossCollectionProjectInfo
import com.github.lizhanyin.tfs.client.ui.framework.UIContext
import com.microsoft.tfs.core.TFSConnection
import java.net.URI

/**
 * 导入项目向导上下文
 * 用于在向导步骤之间共享数据
 */
class ImportProjectContext {

    // UI 上下文（类似 SWT Shell）
    var uiContext: UIContext = UIContext(null)
    var tfsConn : TFSConnection? = null
    // 服务器信息
    var serverUrl: String? = null
    var username: String? = null
    var password: String? = null
    var domain: String? = null
    var authType: AuthType = AuthType.BASIC

    // 团队项目
    var collection: CrossCollectionProjectInfo? = null

    // 选中的工作区
    var workspaceName: String? = null

    // 选中的项目/分支
    val selectedProjects: MutableList<String> = mutableListOf()

    // 本地路径
    var localPath: String? = null

    /**
     * 获取完整的集合 URL
     */
    val collectionUrl: String
        get() {
            val url = serverUrl ?: return ""
            val baseUrl = url.trimEnd('/')
            val collection = collection
            return if (collection == null) baseUrl else "$baseUrl/$collection"
        }

    /**
     * 检查服务器配置是否完整
     */
    fun isServerConfigured(): Boolean = !serverUrl.isNullOrEmpty()

    /**
     * 检查是否已选择团队项目
     */
    fun isTeamProjectSelected(): Boolean = collection != null

    fun getServerUri(): URI = URI(serverUrl ?: "")

    /**
     * 认证类型枚举
     */
    enum class AuthType(private val key: String) {
//        NTLM("Windows 集成认证 (NTLM)"),
        BASIC("ImportProjectContext.auth.basic"),
        PAT("ImportProjectContext.auth.pat");

        val displayName: String get() = TfsBundle.message(key)

        override fun toString(): String = displayName
    }
}
