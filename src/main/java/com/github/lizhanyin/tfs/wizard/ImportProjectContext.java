package com.github.lizhanyin.tfs.wizard;

import java.util.ArrayList;
import java.util.List;

/**
 * 导入项目向导上下文
 * 用于在向导步骤之间共享数据
 */
public class ImportProjectContext {

    // 服务器信息
    private String serverUrl;
    private String collectionName;
    private String username;
    private String password;
    private String domain;
    private AuthType authType = AuthType.NTLM;

    // 团队项目
    private String teamProject;

    // 选中的项目/分支
    private List<String> selectedProjects = new ArrayList<>();

    // 本地路径
    private String localPath;

    // ============== Getters and Setters ==============

    public String getServerUrl() {
        return serverUrl;
    }

    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public String getCollectionName() {
        return collectionName;
    }

    public void setCollectionName(String collectionName) {
        this.collectionName = collectionName;
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

    public AuthType getAuthType() {
        return authType;
    }

    public void setAuthType(AuthType authType) {
        this.authType = authType;
    }

    public String getTeamProject() {
        return teamProject;
    }

    public void setTeamProject(String teamProject) {
        this.teamProject = teamProject;
    }

    public List<String> getSelectedProjects() {
        return selectedProjects;
    }

    public void setSelectedProjects(List<String> selectedProjects) {
        this.selectedProjects = selectedProjects;
    }

    public String getLocalPath() {
        return localPath;
    }

    public void setLocalPath(String localPath) {
        this.localPath = localPath;
    }

    // ============== 辅助方法 ==============

    /**
     * 获取完整的集合 URL
     */
    public String getCollectionUrl() {
        if (serverUrl == null || serverUrl.isEmpty()) {
            return "";
        }
        String baseUrl = serverUrl.replaceAll("/$", "");
        if (collectionName != null && !collectionName.isEmpty()) {
            return baseUrl + "/" + collectionName;
        }
        return baseUrl;
    }

    /**
     * 检查服务器配置是否完整
     */
    public boolean isServerConfigured() {
        return serverUrl != null && !serverUrl.isEmpty();
    }

    /**
     * 检查是否已选择团队项目
     */
    public boolean isTeamProjectSelected() {
        return teamProject != null && !teamProject.isEmpty();
    }

    /**
     * 认证类型枚举
     */
    public enum AuthType {
        NTLM("Windows 集成认证 (NTLM)"),
        BASIC("基本认证"),
        PAT("个人访问令牌");

        private final String displayName;

        AuthType(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }
}
