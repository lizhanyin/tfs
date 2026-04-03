package com.github.lizhanyin.tfs.services.impl;

import com.github.lizhanyin.tfs.client.catalog.CrossCollectionProjectInfo;
import com.github.lizhanyin.tfs.client.commands.CreateWorkspaceCommand;
import com.github.lizhanyin.tfs.client.framework.command.CommandExecutor;
import com.github.lizhanyin.tfs.client.framework.command.ICommandExecutor;
import com.github.lizhanyin.tfs.client.framework.command.ThreadedCancellableCommand;
import com.github.lizhanyin.tfs.client.ui.framework.UIContext;
import com.github.lizhanyin.tfs.client.ui.framework.command.UICommandFinishedCallbackFactory;
import com.github.lizhanyin.tfs.client.ui.framework.command.WizardContainerCommandExecutor;
import com.github.lizhanyin.tfs.client.ui.tasks.ConnectToConfigurationServerTask;
import com.github.lizhanyin.tfs.client.ui.wizard.WizardCollectionSelectionPage;
import com.github.lizhanyin.tfs.client.ui.wizard.WizardServerSelectionPage;
import com.github.lizhanyin.tfs.client.ui.wizard.WizardWorkspacePage;
import com.github.lizhanyin.tfs.runtime.IStatus;
import com.github.lizhanyin.tfs.services.TfsConnectionService;
import com.github.lizhanyin.tfs.settings.TfsServerConfiguration;
import com.github.lizhanyin.tfs.startup.TfsNativeLibraryInitializer;
import com.github.lizhanyin.tfs.wizard.ImportProjectContext;
import com.intellij.openapi.diagnostic.Logger;
import com.microsoft.tfs.core.TFSConnection;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient;
import com.microsoft.tfs.core.clients.versioncontrol.WorkspaceLocation;
import com.microsoft.tfs.core.clients.versioncontrol.WorkspacePermissionProfile;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Item;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.core.httpclient.Credentials;
import com.microsoft.tfs.core.httpclient.DefaultNTCredentials;
import com.microsoft.tfs.core.httpclient.UsernamePasswordCredentials;
import com.microsoft.tfs.core.util.URIUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * TFS 连接服务实现
 * <p>
 * 参考 Eclipse 插件的认证流程，支持 On-Premises TFS 连接。
 * 可以使用 ImportProjectContext 或存储的 ServerConfig 进行连接。
 *
 * @threadsafety thread-safe
 */
public class TfsConnectionServiceImpl implements TfsConnectionService {
    private static final Logger log = Logger.getInstance(TfsConnectionServiceImpl.class);

    /**
     * 默认连接超时时间（毫秒）
     */
    private static final long DEFAULT_TIMEOUT = 30000;

    public TfsConnectionServiceImpl() {
        // 确保本地库已初始化
        TfsNativeLibraryInitializer.INSTANCE.init();
    }

    // ==================== 测试连接 ====================

    @Override
    public boolean testConnection(@NotNull ImportProjectContext context) {
        try {
            Credentials credentials = createCredentials(context);
            URI uri = URIUtils.newURI(context.getServerUrl());
            final ConnectToConfigurationServerTask task = new ConnectToConfigurationServerTask(
                    context.getUiContext(), uri, credentials);
            IStatus status = task.run();
            return status.isOK();
        } catch (Exception e) {
            log.error("连接测试失败", e);
            return false;
        }
    }

    // ==================== 获取连接 ====================

    @Override
    @NotNull
    public TFSConnection getConnection(@NotNull ImportProjectContext context){
        return new WizardServerSelectionPage(context).openAccount();
    }

    // ==================== 获取团队项目 ====================

    @Override
    public @NotNull List<CrossCollectionProjectInfo> getTeamProjects(@NotNull ImportProjectContext context) throws Exception {
        return new WizardCollectionSelectionPage(context).queryTeamProjects();
    }


    // ==================== 获取工作区列表 ====================

    @Override
    @NotNull
    public List<Workspace> getWorkspaces(@NotNull ImportProjectContext context) throws Exception {
        Workspace[] workspaces = new WizardWorkspacePage(context).queryWorkspace(false);
        return Arrays.asList(workspaces);
    }

    // ==================== 创建工作区 ====================

    @Override
    @NotNull
    public Workspace createWorkspace(@NotNull ImportProjectContext context,
                                     @NotNull String name,
                                     @Nullable String comment,
                                     @NotNull WorkspaceLocation location,
                                     @NotNull WorkspacePermissionProfile permissionProfile) throws Exception {
        final TFSTeamProjectCollection connection = context.getCollection().getCollection();
        final CreateWorkspaceCommand command = new CreateWorkspaceCommand(
                connection,
                null, // workingFolders - 创建时无映射
                name,
                comment,
                location,
                null, // options - 使用默认
                permissionProfile);

        final ICommandExecutor executor = new CommandExecutor();
        final IStatus status = executor.execute(command);
        if (!status.isOK()) {
            throw new Exception("创建工作区失败: " + status.getMessage());
        }
        return command.getWorkspace();
    }

    // ==================== 删除工作区 ====================

    @Override
    public void deleteWorkspace(@NotNull ImportProjectContext context, @NotNull Workspace workspace) throws Exception {
        TFSTeamProjectCollection tpc = null;
        try {
            tpc = smartConnect(context);
            tpc.getVersionControlClient().deleteWorkspace(workspace);
        } finally {
            closeConnection(tpc);
        }
    }

    // ==================== 获取子项目 ====================

    @Override
    @NotNull
    public List<ProjectItemInfo> getChildItems(@NotNull ImportProjectContext context, @NotNull String parentPath) throws Exception {
        TFSTeamProjectCollection tpc = null;
        try {
            tpc = smartConnect(context);
            return doGetChildItems(tpc, parentPath);
        } finally {
            closeConnection(tpc);
        }
    }

    private List<ProjectItemInfo> doGetChildItems(TFSTeamProjectCollection tpc, String parentPath) {
        VersionControlClient vcClient = tpc.getVersionControlClient();
        List<ProjectItemInfo> items = new ArrayList<>();

        Item[] tfsItems = vcClient.getItems(parentPath, RecursionType.ONE_LEVEL).getItems();

        if (tfsItems != null) {
            for (Item item : tfsItems) {
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
    }

    /**
     * 智能连接到 TFS 服务器
     */
    private @NotNull TFSTeamProjectCollection smartConnect(@NotNull ImportProjectContext context) throws Exception {
        Credentials credentials = createCredentials(context);
        List<String> urlsToTry = getUrls(context);

        return doSmartConnect(urlsToTry, credentials);
    }

    /**
     * 从存储的配置智能连接到 TFS 服务器
     */
    @Deprecated
    private @NotNull TFSTeamProjectCollection smartConnectFromConfig(@NotNull TfsServerConfiguration.ServerConfig serverConfig) throws Exception {
        Credentials credentials = createCredentialsFromConfig(serverConfig);
        List<String> urlsToTry = getUrlsFromConfig(serverConfig);

        return doSmartConnect(urlsToTry, credentials);
    }

    /**
     * 执行智能连接
     */
    private TFSTeamProjectCollection doSmartConnect(List<String> urlsToTry, Credentials credentials) throws Exception {
        Exception lastException = null;
        for (String url : urlsToTry) {
            TFSTeamProjectCollection tpc = null;
            try {
                log.info("尝试连接: " + url);
                tpc = new TFSTeamProjectCollection(URIUtils.newURI(url), credentials);
                tpc.authenticate();
                log.info("连接成功: " + url);
                return tpc;
            } catch (Exception e) {
                log.warn("连接失败 (" + url + "): " + e.getMessage());
                lastException = e;
                closeConnection(tpc);

                if (isAuthException(e)) {
                    throw e;
                }
            }
        }

        throw lastException;
    }

    private static @NotNull List<String> getUrls(@NotNull ImportProjectContext context) {
        String serverUrl = context.getServerUrl();

        if (serverUrl == null || serverUrl.isEmpty()) {
            throw new IllegalArgumentException("服务器 URL 不能为空");
        }

        serverUrl = serverUrl.replaceAll("/$", "");
        List<String> urlsToTry = new ArrayList<>();

        urlsToTry.add(serverUrl);

        if (context.getCollection() != null) {
            urlsToTry.add(serverUrl + "/" + context.getCollection().getCollectionName());
        }

        return urlsToTry;
    }

    @Deprecated
    private static @NotNull List<String> getUrlsFromConfig(@NotNull TfsServerConfiguration.ServerConfig serverConfig) {
        String serverUrl = serverConfig.getUrl();

        if (serverUrl.isEmpty()) {
            throw new IllegalArgumentException("服务器 URL 不能为空");
        }

        serverUrl = serverUrl.replaceAll("/$", "");
        List<String> urlsToTry = new ArrayList<>();

        urlsToTry.add(serverUrl);

        String collection = serverConfig.getCollection();
        if (!collection.isEmpty()) {
            urlsToTry.add(serverUrl + "/" + collection);
        }

        if (!"DefaultCollection".equals(collection)) {
            urlsToTry.add(serverUrl + "/DefaultCollection");
        }
        return urlsToTry;
    }

    private boolean isAuthException(Exception e) {
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

    private Credentials createCredentials(@NotNull ImportProjectContext context) {
        String password = context.getPassword();
        switch (context.getAuthType()) {
//            case NTLM:
//                return new DefaultNTCredentials();
            case PAT:
                return new UsernamePasswordCredentials.PatCredentials(password);
            case BASIC:
            default:
                String username = context.getUsername();
                String domain = context.getDomain();

                if (domain != null && !domain.isEmpty()) {
                    username = domain + "\\" + username;
                }

                return new UsernamePasswordCredentials(username, password);
        }
    }

    @Deprecated
    private static Credentials createCredentialsFromConfig(@NotNull TfsServerConfiguration.ServerConfig serverConfig) {
        final String authType = serverConfig.getAuthType();
        final String username = serverConfig.getUsername();
        final String password = serverConfig.getPassword();
        final String domain = serverConfig.getDomain();

        // PAT authentication
        if ("PAT".equalsIgnoreCase(authType)) {
            if (password != null && !password.isEmpty()) {
                return new UsernamePasswordCredentials("", password);
            }
            return new DefaultNTCredentials();
        }

        // Username/password authentication
        if (username != null && !username.isEmpty()) {
            final String fullUsername;
            if (domain != null && !domain.isEmpty()) {
                fullUsername = domain + "\\" + username;
            } else {
                fullUsername = username;
            }
            return new UsernamePasswordCredentials(fullUsername, password);
        }

        return new DefaultNTCredentials();
    }

    private static void closeConnection(@Nullable TFSConnection connection) {
        if (connection != null) {
            try {
                connection.close();
            } catch (Exception ignored) {
            }
        }
    }
}
