package com.github.lizhanyin.tfs.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable

/**
 * TFS 服务器配置持久化存储
 */
@State(
    name = "TfsServerConfiguration",
    storages = [Storage("TfsServerConfiguration.xml")]
)
class TfsServerConfiguration : PersistentStateComponent<TfsServerConfiguration> {

    /**
     * 已保存的服务器列表
     */
    var servers: MutableList<ServerConfig> = mutableListOf()

    companion object {
        /**
         * 获取实例
         */
        @JvmStatic
        fun getInstance(): TfsServerConfiguration {
            return ApplicationManager.getApplication().getService(TfsServerConfiguration::class.java)
        }
    }

    @Nullable
    override fun getState(): TfsServerConfiguration = this

    override fun loadState(@NotNull state: TfsServerConfiguration) {
        XmlSerializerUtil.copyBean(state, this)
    }

    // ============== 辅助方法 ==============

    /**
     * 添加服务器
     */
    fun addServer(@NotNull server: ServerConfig) {
        // 检查是否已存在相同 URL 的服务器
        val existingIndex = servers.indexOfFirst { it.url == server.url }
        if (existingIndex >= 0) {
            servers[existingIndex] = server // 更新现有服务器
        } else {
            servers.add(server)
        }
    }

    /**
     * 移除服务器
     */
    fun removeServer(@NotNull server: ServerConfig) {
        servers.removeIf { it.url == server.url }
    }

    /**
     * 更新服务器
     */
    fun updateServer(@NotNull oldServer: ServerConfig, @NotNull newServer: ServerConfig) {
        val index = servers.indexOfFirst { it.url == oldServer.url }
        if (index >= 0) {
            servers[index] = newServer
        }
    }

    /**
     * 获取服务器配置
     */
    @Nullable
    fun getServer(@NotNull url: String): ServerConfig? {
        return servers.find { it.url == url }
    }

    /**
     * 服务器配置
     */
    class ServerConfig {
        var url: String = ""
        var collection: String = ""
        var name: String = ""
        var username: String? = null
        var password: String? = null  // 注意: 密码会以明文存储，实际应用中应考虑加密
        var domain: String? = null
        var authType: String = "BASIC"

        constructor()

        constructor(url: String, collection: String, name: String) {
            this.url = url
            this.collection = collection
            this.name = name
        }

        override fun toString(): String {
            return if (name.isNotEmpty()) "$name ($url)" else url
        }
    }
}
