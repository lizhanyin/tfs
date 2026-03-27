package com.github.lizhanyin.tfs.model;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * TFS 工作区模型
 */
public record TfsWorkspace(String name, String owner, String serverUrl, String collectionName,
                           List<WorkingFolder> workingFolders) {

    public TfsWorkspace(@NotNull String name,
                        @NotNull String owner,
                        @NotNull String serverUrl,
                        @Nullable String collectionName,
                        @NotNull List<WorkingFolder> workingFolders) {
        this.name = name;
        this.owner = owner;
        this.serverUrl = serverUrl;
        this.collectionName = collectionName;
        this.workingFolders = workingFolders;
    }

    @Override
    @NotNull
    public String name() {
        return name;
    }

    @Override
    @NotNull
    public String owner() {
        return owner;
    }

    @Override
    @NotNull
    public String serverUrl() {
        return serverUrl;
    }

    @Override
    @Nullable
    public String collectionName() {
        return collectionName;
    }

    @Override
    @NotNull
    public List<WorkingFolder> workingFolders() {
        return workingFolders;
    }

    /**
     * 查找本地路径对应的服务器路径
     */
    @Nullable
    public String getServerPathForLocalPath(@NotNull String localPath) {
        for (WorkingFolder folder : workingFolders) {
            if (localPath.startsWith(folder.getLocalPath())) {
                return folder.getServerPath() + localPath.substring(folder.getLocalPath().length());
            }
        }
        return null;
    }

    /**
     * 工作文件夹映射
     */
    public static class WorkingFolder {
        private final String serverPath;
        private final String localPath;

        public WorkingFolder(@NotNull String serverPath, @NotNull String localPath) {
            this.serverPath = serverPath;
            this.localPath = localPath;
        }

        @NotNull
        public String getServerPath() {
            return serverPath;
        }

        @NotNull
        public String getLocalPath() {
            return localPath;
        }
    }
}
