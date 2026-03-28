// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.github.lizhanyin.tfs.client.ui.tasks;

import java.net.URI;
import java.util.concurrent.atomic.AtomicReference;

import com.github.lizhanyin.tfs.client.Messages;
import com.github.lizhanyin.tfs.client.credentials.IdeaCredentialsManagerFactory;
import com.github.lizhanyin.tfs.client.framework.command.ThreadedCancellableCommand;
import com.github.lizhanyin.tfs.client.framework.helper.UIHelpers;
import com.github.lizhanyin.tfs.client.framework.status.TeamExplorerStatus;
import com.github.lizhanyin.tfs.runtime.IStatus;
import com.github.lizhanyin.tfs.runtime.Status;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.microsoft.tfs.core.config.persistence.DefaultPersistenceStoreProvider;
import com.microsoft.tfs.core.credentials.CachedCredentials;
import com.microsoft.tfs.core.httpclient.DefaultNTCredentials;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.lizhanyin.tfs.client.ui.commands.ConnectCommand;
import com.github.lizhanyin.tfs.settings.TfsServerConfiguration;
import com.microsoft.tfs.core.TFSConnection;
import com.microsoft.tfs.core.httpclient.Credentials;
import com.microsoft.tfs.core.httpclient.UsernamePasswordCredentials;

/**
 * Base class for connection tasks that connect to TFS servers.
 *
 * This task supports two modes:
 * 1. Direct parameters: server URI and credentials passed directly
 * 2. From stored configuration: uses TfsServerConfiguration.ServerConfig to load credentials
 *
 * @threadsafety unknown
 */
public abstract class ConnectTask extends BaseTask  {
    private static final Logger LOG = Logger.getInstance(ConnectTask.class);

    protected final URI serverURI;
    protected final TfsServerConfiguration.ServerConfig serverConfig;
    protected Credentials credentials;

    private final AtomicReference<TFSConnection> connectionRef = new AtomicReference<>();
    private volatile String errorMessage;

    /**
     * Connects to the given server URI.
     *
     * @param project
     *        a valid {@link Project}
     * @param serverURI
     *        the server URI to connect to
     */
    public ConnectTask(@Nullable final Project project, final URI serverURI) {
        this(project, serverURI, null);
    }


    /**
     * Creates a connect task with direct parameters.
     *
     * @param project the IDEA project (may be null)
     * @param serverURI the server URI to connect to
     * @param credentials the credentials to use (may be null for default credentials)
     */
    protected ConnectTask(@Nullable final Project project, @NotNull final URI serverURI, @Nullable final Credentials credentials) {
        super(project);
        this.serverURI = serverURI;
        this.credentials = credentials;
        this.serverConfig = null;
    }

    /**
     * Creates a connect task from stored server configuration.
     * Credentials will be loaded from the configuration.
     *
     * @param project the IDEA project (may be null)
     * @param serverConfig the server configuration containing URL and credentials
     */
    protected ConnectTask(@Nullable final Project project, @NotNull final TfsServerConfiguration.ServerConfig serverConfig) {
        super(project);
        this.serverURI = parseUri(serverConfig.getUrl());
        this.credentials = createCredentialsFromConfig(serverConfig);
        this.serverConfig = serverConfig;
    }

    @Override
    public IStatus run() {
        /* Try to get some credentials */
        if (credentials == null) {
            final CachedCredentials cachedCredentials = IdeaCredentialsManagerFactory.getCredentialsManager(
                    DefaultPersistenceStoreProvider.INSTANCE).getCredentials(serverURI);

            // try to use DefaultNTCredentials when no credentials acquired
            credentials = cachedCredentials != null ? cachedCredentials.toCredentials() : new DefaultNTCredentials();
        }


        IStatus status = Status.CANCEL_STATUS;

        final ConnectCommand connectCommand = getConnectCommand(serverURI, credentials);

        status = getCommandExecutor().execute(new ThreadedCancellableCommand(connectCommand));

        connectCommandFinished(connectCommand);

        if (status.isOK()) {
            connectionRef.set(connectCommand.getConnection());
            status = Status.OK_STATUS;
        }

        return status;
    }

    protected void connectCommandFinished(final ConnectCommand connectCommand) {
    }

    /**
     * Gets the connect command to execute.
     *
     * @param serverURI the server URI
     * @param credentials the credentials (may be null)
     * @return the connect command to execute
     */
    protected abstract ConnectCommand getConnectCommand(URI serverURI, @Nullable Credentials credentials);

    /**
     * Gets the connection result.
     *
     * @return the TFS connection, or null if connection failed
     */
    @Nullable
    public TFSConnection getConnection() {
        return connectionRef.get();
    }

    /**
     * Gets the error message if connection failed.
     *
     * @return the error message, or null if no error
     */
    @Nullable
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Gets the server URI being connected to.
     *
     * @return the server URI
     */
    @NotNull
    public URI getServerURI() {
        return serverURI;
    }

    /**
     * Parses a URL string to URI.
     *
     * @param url the URL string
     * @return the URI
     * @throws IllegalArgumentException if URL is invalid
     */
    @NotNull
    protected static URI parseUri(@NotNull final String url) {
        return URI.create(url);
    }

    /**
     * Creates credentials from server configuration.
     *
     * @param config the server configuration
     * @return the credentials, or null for default NTLM credentials
     */
    @Nullable
    protected static Credentials createCredentialsFromConfig(@NotNull final TfsServerConfiguration.ServerConfig config) {
        final String authType = config.getAuthType();
        final String username = config.getUsername();
        final String password = config.getPassword();

        if (password == null || password.isEmpty()) {
            // No password, use default NTLM credentials
            return null;
        }

        // Use username/password credentials
        if (username != null && !username.isEmpty()) {
            return new UsernamePasswordCredentials(username, password);
        }

        // PAT authentication - username is empty, password is the token
        return new UsernamePasswordCredentials("", password);
    }
}
