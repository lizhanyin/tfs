package com.github.lizhanyin.tfs.services;

import com.github.lizhanyin.tfs.client.catalog.CrossCollectionProjectInfo;
import com.github.lizhanyin.tfs.settings.TfsServerConfiguration;
import com.github.lizhanyin.tfs.wizard.ImportProjectContext;
import com.microsoft.tfs.core.TFSConnection;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * TFS 连接服务接口
 *
 * 提供连接到 TFS 服务器、测试连接、获取团队项目和项目项的功能。
 *
 * 支持两种参数模式：
 * 1. 使用 ImportProjectContext 上下文
 * 2. 使用 TfsServerConfiguration.ServerConfig 存储的配置
 */
public interface TfsConnectionService {

    /**
     * 测试与 TFS 服务器的连接
     *
     * @param context 导入项目上下文
     * @return 是否连接成功
     */
    boolean testConnection(@NotNull ImportProjectContext context);

    /**
     * 获取 TFS 服务器的连接
     * @param context 导入项目上下文
     * @return conn
     */
    TFSConnection getConnection(@NotNull ImportProjectContext context);

    /**
     * 获取团队项目列表
     *
     * @param context 导入项目上下文
     * @return 团队项目名称列表
     * @throws Exception 如果连接或查询失败
     */
    @NotNull List<CrossCollectionProjectInfo> getTeamProjects(@NotNull ImportProjectContext context) throws Exception;

    /**
     * 使用存储的服务器配置获取团队项目列表
     *
     * @param serverConfig 服务器配置
     * @return 团队项目名称列表
     * @throws Exception 如果连接或查询失败
     */
    @NotNull
    @Deprecated
    List<String> getTeamProjects(@NotNull TfsServerConfiguration.ServerConfig serverConfig) throws Exception;

    /**
     * 获取服务器项目结构（文件夹和文件）
     *
     * @param context     导入项目上下文
     * @param teamProject 团队项目名称
     * @return 项目项列表
     * @throws Exception 如果连接或查询失败
     */
    @NotNull
    List<ProjectItemInfo> getProjectItems(@NotNull ImportProjectContext context, @NotNull String teamProject) throws Exception;

    /**
     * 获取服务器项目结构（文件夹和文件）
     *
     * @param serverConfig 服务器配置
     * @param teamProject  团队项目名称
     * @return 项目项列表
     * @throws Exception 如果连接或查询失败
     */
    @NotNull
    @Deprecated
    List<ProjectItemInfo> getProjectItems(@NotNull TfsServerConfiguration.ServerConfig serverConfig, @NotNull String teamProject) throws Exception;

    /**
     * 获取子项目
     *
     * @param context    导入项目上下文
     * @param parentPath 父路径
     * @return 子项目列表
     * @throws Exception 如果连接或查询失败
     */
    @NotNull
    List<ProjectItemInfo> getChildItems(@NotNull ImportProjectContext context, @NotNull String parentPath) throws Exception;

    /**
     * 获取子项目
     *
     * @param serverConfig 服务器配置
     * @param parentPath   父路径
     * @return 子项目列表
     * @throws Exception 如果连接或查询失败
     */
    @NotNull
    @Deprecated
    List<ProjectItemInfo> getChildItems(@NotNull TfsServerConfiguration.ServerConfig serverConfig, @NotNull String parentPath) throws Exception;

    /**
     * 项目项信息
     */
    class ProjectItemInfo {
        private final String serverPath;
        private final ItemType itemType;

        public ProjectItemInfo(String serverPath, ItemType itemType) {
            this.serverPath = serverPath;
            this.itemType = itemType;
        }

        public String getServerPath() {
            return serverPath;
        }

        public ItemType getItemType() {
            return itemType;
        }

        public boolean isFolder() {
            return itemType == ItemType.FOLDER;
        }

        public String getName() {
            if (serverPath == null) return "";
            int lastSlash = serverPath.lastIndexOf('/');
            return lastSlash >= 0 ? serverPath.substring(lastSlash + 1) : serverPath;
        }
    }

    /**
     * 项目项类型
     */
    enum ItemType {
        FILE,
        FOLDER
    }
}
