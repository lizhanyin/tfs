package com.github.lizhanyin.tfs.services.impl;

import com.github.lizhanyin.tfs.services.TfsConnectionService;
import com.github.lizhanyin.tfs.startup.TfsNativeLibraryInitializer;
import com.github.lizhanyin.tfs.wizard.ImportProjectContext;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Item;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.httpclient.Credentials;
import com.microsoft.tfs.core.httpclient.DefaultNTCredentials;
import com.microsoft.tfs.core.httpclient.UsernamePasswordCredentials;
import com.microsoft.tfs.core.util.URIUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * TFS 连接服务实现
 * 参考 Eclipse 插件的认证流程，支持 On-Premises TFS 连接
 */
public class TfsConnectionServiceImpl implements TfsConnectionService {
    private static final Log log = LogFactory.getLog(TfsConnectionServiceImpl.class);

    public TfsConnectionServiceImpl() {
        // 确保本地库已初始化
        TfsNativeLibraryInitializer.INSTANCE.init();
    }

    @Override
    public boolean testConnection(@NotNull ImportProjectContext context) {
        TFSTeamProjectCollection tpc = null;
        try {
            // 使用智能连接方式，自动检测服务器类型
            tpc = smartConnect(context);
            return true;
        } catch (Exception e) {
            log.error("连接测试失败", e);
            return false;
        } finally {
            if (tpc != null) {
                try {
                    tpc.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    @Override
    @NotNull
    public List<String> getTeamProjects(@NotNull ImportProjectContext context) throws Exception {
        TFSTeamProjectCollection tpc = null;
        try {
            tpc = smartConnect(context);
            List<String> projects = new ArrayList<>();
            for (com.microsoft.tfs.core.clients.workitem.project.Project project : tpc.getWorkItemClient().getProjects()) {
                projects.add(project.getName());
            }
            return projects;
        } finally {
            if (tpc != null) {
                try {
                    tpc.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    @Override
    @NotNull
    public List<ProjectItemInfo> getProjectItems(@NotNull ImportProjectContext context, @NotNull String teamProject) throws Exception {
        TFSTeamProjectCollection tpc = null;
        try {
            tpc = smartConnect(context);
            VersionControlClient vcClient = tpc.getVersionControlClient();
            List<ProjectItemInfo> items = new ArrayList<>();

            // 获取团队项目根路径下的项目
            String serverPath = "$/" + teamProject;
            Item[] tfsItems = vcClient.getItems(serverPath, RecursionType.ONE_LEVEL).getItems();

            if (tfsItems != null) {
                for (Item item : tfsItems) {
                    // 排除根目录本身
                    if (!item.getServerItem().equals(serverPath)) {
                        items.add(new ProjectItemInfo(
                                item.getServerItem(),
                                item.getItemType() == com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemType.FOLDER
                                        ? ItemType.FOLDER : ItemType.FILE
                        ));
                    }
                }
            }

            return items;
        } finally {
            if (tpc != null) {
                try {
                    tpc.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    @Override
    @NotNull
    public List<ProjectItemInfo> getChildItems(@NotNull ImportProjectContext context, @NotNull String parentPath) throws Exception {
        TFSTeamProjectCollection tpc = null;
        try {
            tpc = smartConnect(context);
            VersionControlClient vcClient = tpc.getVersionControlClient();
            List<ProjectItemInfo> items = new ArrayList<>();

            Item[] tfsItems = vcClient.getItems(parentPath, RecursionType.ONE_LEVEL).getItems();

            if (tfsItems != null) {
                for (Item item : tfsItems) {
                    // 排除当前目录本身
                    if (!item.getServerItem().equals(parentPath)) {
                        items.add(new ProjectItemInfo(
                                item.getServerItem(),
                                item.getItemType() == com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemType.FOLDER
                                        ? ItemType.FOLDER : ItemType.FILE
                        ));
                    }
                }
            }

            return items;
        } finally {
            if (tpc != null) {
                try {
                    tpc.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    /**
     * 智能连接到 TFS 服务器
     * 参考 Eclipse 插件的 ConnectToConfigurationServerCommand 实现
     * 对于 On-Premises TFS，会尝试多种连接方式：
     * 1. 直接连接到服务器 URL（作为项目集合）
     * 2. 如果失败，尝试在 URL 后追加集合名
     */
    private @NotNull TFSTeamProjectCollection smartConnect(@NotNull ImportProjectContext context) throws Exception {
        Credentials credentials = createCredentials(context);
        List<String> urlsToTry = getUrls(context);

        Exception lastException = null;
        for (String url : urlsToTry) {
            TFSTeamProjectCollection tpc = null;
            try {
                log.info("尝试连接: " + url);
                tpc = new TFSTeamProjectCollection(
                        URIUtils.newURI(url),
                        credentials
                );

                // 执行认证
                tpc.authenticate();
                log.info("连接成功: " + url);

                return tpc;
            } catch (Exception e) {
                log.warn("连接失败 (" + url + "): " + e.getMessage());
                lastException = e;

                if (tpc != null) {
                    try {
                        tpc.close();
                    } catch (Exception ignored) {
                    }
                }

                // 认证失败不重试
                if (isAuthException(e)) {
                    throw e;
                }
                // 其他错误（如 404）继续尝试下一种方式
            }
        }

        throw lastException;
    }

    private static @NotNull List<String> getUrls(@NotNull ImportProjectContext context) {
        String serverUrl = context.getServerUrl();

        if (serverUrl == null || serverUrl.isEmpty()) {
            throw new IllegalArgumentException("服务器 URL 不能为空");
        }

        // 清理 URL
        serverUrl = serverUrl.replaceAll("/$", "");

        // 尝试的 URL 列表
        List<String> urlsToTry = new ArrayList<>();

        // 1. 直接使用原始 URL
        urlsToTry.add(serverUrl);

        // 2. 如果有集合名，尝试 URL/集合名
        if (context.getCollectionName() != null && !context.getCollectionName().isEmpty()) {
            urlsToTry.add(serverUrl + "/" + context.getCollectionName());
        }

        // 3. 尝试 DefaultCollection
        if (!"DefaultCollection".equals(context.getCollectionName())) {
            urlsToTry.add(serverUrl + "/DefaultCollection");
        }
        return urlsToTry;
    }

    /**
     * 判断是否为认证异常
     */
    private boolean isAuthException(Exception e) {
        // 检查常见的认证失败情况
        String message = e.getMessage();
        if (message == null) {
            return false;
        }
        message = message.toLowerCase();
        return message.contains("401") ||
               message.contains("unauthorized") ||
               message.contains("access denied") ||
               message.contains("authentication") ||
               message.contains("credential") ||
               e.getClass().getName().contains("Unauthorized") ||
               e.getClass().getName().contains("AccessDenied");
    }

    /**
     * 创建凭据
     */
    private Credentials createCredentials(@NotNull ImportProjectContext context) {
        switch (context.getAuthType()) {
            case NTLM:
                return new DefaultNTCredentials();
            case BASIC:
            case PAT:
            default:
                String username = context.getUsername();
                String password = context.getPassword();
                String domain = context.getDomain();

                /* 如果有域名，组合为 DOMAIN\\username 格式 */
                if (domain != null && !domain.isEmpty()) {
                    username = domain + "\\" + username;
                }

                return new UsernamePasswordCredentials(username, password);
        }
    }
}
