package com.github.lizhanyin.tfs.services.impl;

import com.github.lizhanyin.tfs.client.catalog.CrossCollectionProjectInfo;
import com.github.lizhanyin.tfs.client.catalog.TeamProjectCollectionInfo;
import com.github.lizhanyin.tfs.client.commands.QueryProjectCollectionsCommand;
import com.github.lizhanyin.tfs.client.commands.QueryTeamProjectsCommand;
import com.github.lizhanyin.tfs.client.credentials.IdeaCredentialsManagerFactory;
import com.github.lizhanyin.tfs.client.framework.command.CommandExecutor;
import com.github.lizhanyin.tfs.client.framework.command.ICommandExecutor;
import com.github.lizhanyin.tfs.client.framework.command.ThreadedCancellableCommand;
import com.github.lizhanyin.tfs.client.ui.framework.UIContext;
import com.github.lizhanyin.tfs.client.ui.framework.command.UICommandFinishedCallbackFactory;
import com.github.lizhanyin.tfs.client.ui.framework.command.WizardContainerCommandExecutor;
import com.github.lizhanyin.tfs.client.ui.tasks.ConnectToConfigurationServerTask;
import com.github.lizhanyin.tfs.runtime.IStatus;
import com.github.lizhanyin.tfs.services.TfsConnectionService;
import com.github.lizhanyin.tfs.settings.TfsServerConfiguration;
import com.github.lizhanyin.tfs.startup.TfsNativeLibraryInitializer;
import com.github.lizhanyin.tfs.wizard.ImportProjectContext;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.microsoft.tfs.core.TFSConfigurationServer;
import com.microsoft.tfs.core.TFSConnection;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.commonstructure.ProjectInfo;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Item;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.config.persistence.DefaultPersistenceStoreProvider;
import com.microsoft.tfs.core.credentials.CachedCredentials;
import com.microsoft.tfs.core.credentials.CredentialsManager;
import com.microsoft.tfs.core.httpclient.CookieCredentials;
import com.microsoft.tfs.core.httpclient.Credentials;
import com.microsoft.tfs.core.httpclient.DefaultNTCredentials;
import com.microsoft.tfs.core.httpclient.UsernamePasswordCredentials;
import com.microsoft.tfs.core.util.ServerURIUtils;
import com.microsoft.tfs.core.util.URIUtils;
import com.microsoft.tfs.util.Platform;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.util.ArrayList;
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

    @Override
    @NotNull
    public TFSConnection getConnection(@NotNull ImportProjectContext context){
        final URI serverURI = context.getServerUri();

        return openAccount(context, serverURI, null);
    }

    private TFSConnection openAccount(final ImportProjectContext context, final URI accountUrl, final Credentials credentials) {

        final Credentials accountCredentials = getAccountCredentials(accountUrl, credentials);

        final ICommandExecutor noErrorDialogCommandExecutor = getCommandExecutor(context.getUiContext());
        noErrorDialogCommandExecutor.setCommandFinishedCallback(
                UICommandFinishedCallbackFactory.getDefaultNoErrorDialogCallback());

        final ConnectToConfigurationServerTask connectTask =
                new ConnectToConfigurationServerTask(context.getUiContext(), accountUrl, accountCredentials);
        connectTask.setCommandExecutor(noErrorDialogCommandExecutor);
        final IStatus status = connectTask.run();

        final TFSConnection connection;
        if (status.isOK()) {
            connection = connectTask.getConnection();
            updateCredentials(accountUrl, connection.getCredentials());
        } else {
            /* Connection cancelled */
            connection = null;
        }

        return connection;
    }

    private Credentials getAccountCredentials(final URI accountUrl, final Credentials proposedCredentials) {
        if (proposedCredentials == null) {
            final CredentialsManager credentialsManager =
                    IdeaCredentialsManagerFactory.getCredentialsManager(DefaultPersistenceStoreProvider.INSTANCE);
            final CachedCredentials cachedCredentials = credentialsManager.getCredentials(accountUrl);

            if (cachedCredentials != null) {
                return cachedCredentials.toCredentials();
            } else {
                /*
                 * For on-premises servers, simply use empty
                 * UsernamePasswordCredentials (to force a username/password
                 * dialog.) For hosted servers, use default NT credentials at
                 * all (to avoid the username/password dialog.)
                 */
                return ServerURIUtils.isHosted(accountUrl) || Platform.isCurrentPlatform(Platform.WINDOWS)
                        ? new DefaultNTCredentials() : new UsernamePasswordCredentials("", null); //$NON-NLS-1$
            }
        } else if (proposedCredentials instanceof CookieCredentials) {
            return ((CookieCredentials) proposedCredentials).setDomain(accountUrl.getHost());
        } else {
            return proposedCredentials;
        }
    }

    private void updateCredentials(final URI accountUrl, final Credentials credentials) {
        final CredentialsManager credentialsManager =
                IdeaCredentialsManagerFactory.getCredentialsManager(DefaultPersistenceStoreProvider.INSTANCE);

        // PasswordSafe.setPassword() 是慢操作，不允许在 EDT 上执行
        com.intellij.openapi.application.ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                if (credentials != null && !(credentials instanceof DefaultNTCredentials)) {
                    log.debug("Save the new Cookie Credentials in the Eclipse secure storage for future sessions."); //$NON-NLS-1$
                    credentialsManager.setCredentials(new CachedCredentials(accountUrl, credentials));
                } else {
                    credentialsManager.removeCredentials(accountUrl);
                }
            } catch (final Exception e) {
                log.error("Error writing credentials to the IntelliJ IDEA secure store", e); //$NON-NLS-1$
            }
        });
    }


    private ICommandExecutor getCommandExecutor(UIContext uiContext) {
        // 优先使用 UIContext 中携带的 ProgressIndicator
        if (uiContext.getProgressIndicator() != null) {
            return new WizardContainerCommandExecutor(uiContext);
        }

        // 尝试获取当前线程的 ProgressIndicator（在 Task.Modal/Backgroundable 内）
        final ProgressIndicator currentIndicator = ProgressManager.getInstance().getProgressIndicator();
        if (currentIndicator != null) {
            return new WizardContainerCommandExecutor(UIContext.of(
                    uiContext.getProject(), uiContext.getParentComponent(), currentIndicator));
        }

        // 无 ProgressIndicator 时使用不需要进度的执行器
        return new CommandExecutor();
    }

    // ==================== 获取团队项目 ====================

    @Override
    public @NotNull List<CrossCollectionProjectInfo> getTeamProjects(@NotNull ImportProjectContext context) throws Exception {
        List<CrossCollectionProjectInfo> projects = new ArrayList<>(100);

        TFSConnection connection = context.getTfsConn();

        TFSConfigurationServer configurationServer = null;
        if (connection instanceof TFSConfigurationServer) {
            configurationServer = (TFSConfigurationServer) connection;
        } else if (connection instanceof TFSTeamProjectCollection) {
            configurationServer = ((TFSTeamProjectCollection) connection).getConfigurationServer();
        }

        if (configurationServer == null) {
            log.error(new IllegalArgumentException("Unexpected connection type: " + connection.getClass().getName())); //$NON-NLS-1$
            return projects;
        }

        final List<TFSTeamProjectCollection> collections = new ArrayList<>(5);
        final QueryProjectCollectionsCommand queryCommand = new QueryProjectCollectionsCommand(configurationServer);

        final IStatus status = getCommandExecutor(context.getUiContext()).execute(new ThreadedCancellableCommand(queryCommand));
        if (!status.isOK()) {
            return projects;
        }

        final TeamProjectCollectionInfo[] projectCollections = queryCommand.getProjectCollections();
        for (final TeamProjectCollectionInfo collectionInfo : projectCollections) {
            try {
                collections.add(
                        configurationServer.getTeamProjectCollection(collectionInfo.getIdentifier()));
            } catch (final Exception e) {
                log.warn("Failed to get Team Project Collection: " + collectionInfo.getDisplayName()); //$NON-NLS-1$
                log.warn(e);
            }
        }

        // For each collection get the list of projects
        for (final TFSTeamProjectCollection collection : collections) {
            final QueryTeamProjectsCommand queryCommand2 = new QueryTeamProjectsCommand(collection);
            final IStatus status2 = getCommandExecutor(context.getUiContext()).execute(new ThreadedCancellableCommand(queryCommand2));
            if (!status2.isOK()) {
                continue;
            }
            final ProjectInfo[] projectInfos = queryCommand2.getProjects();
            for (final ProjectInfo info : projectInfos) {
                final CrossCollectionProjectInfo pi = new CrossCollectionProjectInfo(
                        collection,
                        info.getName(),
                        info.getURI(),
                        collection.getName(),
                        collection.getBaseURI().getHost());
                projects.add(pi);
            }
        }
        return projects;
    }

    // ==================== 获取项目项 ====================

    @Override
    @NotNull
    public List<ProjectItemInfo> getProjectItems(@NotNull ImportProjectContext context, @NotNull String teamProject) throws Exception {
        TFSTeamProjectCollection tpc = null;
        try {
            tpc = smartConnect(context);
            return doGetProjectItems(tpc, teamProject);
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

    // ==================== 私有辅助方法 ====================

    private List<String> getTeamProjectNames(TFSTeamProjectCollection tpc) {
        List<String> projects = new ArrayList<>();
        for (com.microsoft.tfs.core.clients.workitem.project.Project project : tpc.getWorkItemClient().getProjects()) {
            projects.add(project.getName());
        }
        return projects;
    }

    private List<ProjectItemInfo> doGetProjectItems(TFSTeamProjectCollection tpc, String teamProject) {
        VersionControlClient vcClient = tpc.getVersionControlClient();
        List<ProjectItemInfo> items = new ArrayList<>();

        String serverPath = "$/" + teamProject;
        Item[] tfsItems = vcClient.getItems(serverPath, RecursionType.ONE_LEVEL).getItems();

        if (tfsItems != null) {
            for (Item item : tfsItems) {
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
