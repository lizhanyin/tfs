package com.github.lizhanyin.tfs.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * TFS 服务器配置持久化存储
 */
@State(
    name = "TfsServerConfiguration",
    storages = {@Storage("TfsServerConfiguration.xml")}
)
public class TfsServerConfiguration implements PersistentStateComponent<TfsServerConfiguration> {

    /**
     * 已保存的服务器列表
     */
    private List<ServerConfig> servers = new ArrayList<>();

    /**
     * 获取实例
     */
    public static TfsServerConfiguration getInstance() {
        return ApplicationManager.getApplication().getService(TfsServerConfiguration.class);
    }

    @Nullable
    @Override
    public TfsServerConfiguration getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull TfsServerConfiguration state) {
        XmlSerializerUtil.copyBean(state, this);
    }

    // ============== Getters and Setters ==============

    public List<ServerConfig> getServers() {
        return servers;
    }

    public void setServers(List<ServerConfig> servers) {
        this.servers = servers != null ? servers : new ArrayList<>();
    }

    // ============== 辅助方法 ==============

    /**
     * 添加服务器
     */
    public void addServer(@NotNull ServerConfig server) {
        // 检查是否已存在相同 URL 的服务器
        for (int i = 0; i < servers.size(); i++) {
            if (servers.get(i).getUrl().equals(server.getUrl())) {
                servers.set(i, server); // 更新现有服务器
                return;
            }
        }
        servers.add(server);
    }

    /**
     * 移除服务器
     */
    public void removeServer(@NotNull ServerConfig server) {
        servers.removeIf(s -> s.getUrl().equals(server.getUrl()));
    }

    /**
     * 更新服务器
     */
    public void updateServer(@NotNull ServerConfig oldServer, @NotNull ServerConfig newServer) {
        int index = -1;
        for (int i = 0; i < servers.size(); i++) {
            if (servers.get(i).getUrl().equals(oldServer.getUrl())) {
                index = i;
                break;
            }
        }
        if (index >= 0) {
            servers.set(index, newServer);
        }
    }

    /**
     * 获取服务器配置
     */
    @Nullable
    public ServerConfig getServer(@NotNull String url) {
        for (ServerConfig server : servers) {
            if (server.getUrl().equals(url)) {
                return server;
            }
        }
        return null;
    }

    /**
     * 服务器配置
     */
    public static class ServerConfig {
        private String url;
        private String collection;
        private String name;
        private String username;
        private String password; // 注意: 密码会以明文存储，实际应用中应考虑加密
        private String domain;
        private String authType = "NTLM";

        public ServerConfig() {
        }

        public ServerConfig(String url, String collection, String name) {
            this.url = url;
            this.collection = collection;
            this.name = name;
        }

        // ============== Getters and Setters ==============

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getCollection() {
            return collection;
        }

        public void setCollection(String collection) {
            this.collection = collection;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getDomain() {
            return domain;
        }

        public void setDomain(String domain) {
            this.domain = domain;
        }

        public String getAuthType() {
            return authType;
        }

        public void setAuthType(String authType) {
            this.authType = authType;
        }

        @Override
        public String toString() {
            if (name != null && !name.isEmpty()) {
                return name + " (" + url + ")";
            }
            return url;
        }
    }
}
