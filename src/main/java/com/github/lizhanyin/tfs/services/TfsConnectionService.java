package com.github.lizhanyin.tfs.services;

import com.github.lizhanyin.tfs.client.catalog.CrossCollectionProjectInfo;
import com.github.lizhanyin.tfs.wizard.ImportProjectContext;
import com.microsoft.tfs.core.TFSConnection;
import com.microsoft.tfs.core.clients.versioncontrol.WorkspaceLocation;
import com.microsoft.tfs.core.clients.versioncontrol.WorkspaceOptions;
import com.microsoft.tfs.core.clients.versioncontrol.WorkspacePermissionProfile;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.WorkingFolder;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
     * 获取工作区列表
     *
     * @param context 导入项目上下文
     * @return 工作区信息列表
     * @throws Exception 如果连接或查询失败
     */
    @NotNull
    List<Workspace> getWorkspaces(@NotNull ImportProjectContext context) throws Exception;

    /**
     * 创建工作区
     *
     * @param context             导入项目上下文
     * @param name                工作区名称
     * @param comment             注释（可为 null）
     * @param location            工作区位置
     * @param permissionProfile   权限配置
     * @return 创建的工作区
     * @throws Exception 如果创建失败
     */
    @NotNull
    Workspace createWorkspace(@NotNull ImportProjectContext context,
                              @NotNull String name,
                              @Nullable String comment,
                              @NotNull WorkspaceLocation location,
                              @NotNull WorkspacePermissionProfile permissionProfile) throws Exception;

    /**
     * 删除工作区
     *
     * @param context   导入项目上下文
     * @param workspace 要删除的工作区
     * @throws Exception 如果删除失败
     */
    void deleteWorkspace(@NotNull ImportProjectContext context, @NotNull Workspace workspace) throws Exception;

    /**
     * 获取团队项目列表
     *
     * @param context 导入项目上下文
     * @return 团队项目名称列表
     * @throws Exception 如果连接或查询失败
     */
    @NotNull List<CrossCollectionProjectInfo> getTeamProjects(@NotNull ImportProjectContext context) throws Exception;

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

    /**
     * 工作区信息
     */
    class WorkspaceInfo {
        private final String name;
        private final String computer;
        private final String owner;
        private final String comment;

        public WorkspaceInfo(String name, String computer, String owner, String comment) {
            this.name = name;
            this.computer = computer;
            this.owner = owner;
            this.comment = comment;
        }

        public String getName() { return name; }
        public String getComputer() { return computer; }
        public String getOwner() { return owner; }
        public String getComment() { return comment; }
    }
}
