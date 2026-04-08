// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.github.lizhanyin.tfs.client.server;

import java.text.MessageFormat;

import com.github.lizhanyin.tfs.client.Messages;
import com.github.lizhanyin.tfs.client.server.cache.buildstatus.BuildStatusManager;
import com.github.lizhanyin.tfs.client.server.cache.project.ServerProjectCache;
import com.github.lizhanyin.tfs.client.util.ConnectionHelper;
import com.github.lizhanyin.tfs.client.wit.QueryDocumentService;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;

import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.core.util.ServerURIUtils;
import com.microsoft.tfs.util.Check;
import org.jetbrains.annotations.NotNull;

final public class TFSServer {
    private final Object refreshLock = new Object();
    private volatile boolean isRefreshing = false;

    private final TFSTeamProjectCollection connection;

    private final ServerProjectCache projectCache;
    private final QueryDocumentService queryDocumentService;
    private final BuildStatusManager buildStatusManager;

    /**
     * This is package private and should only be created by the
     * {@link ServerManager}.
     * <p>
     * Creates a new {@link TFSServer} for the given
     * {@link TFSTeamProjectCollection}. If this connection has been established
     * then the server data will be refreshed.
     *
     * @param connection
     *        the TFS {@link Workspace} (not <code>null</code>)
     */
    TFSServer(final TFSTeamProjectCollection connection) {
        this(connection, ConnectionHelper.isConnected(connection));
    }

    /**
     * Creates a new {@link TFSServer} for the given
     * {@link TFSTeamProjectCollection}. If this connection has been established
     * then the server data will be refreshed.
     *
     * @param connection
     *        the TFS {@link Workspace} (not <code>null</code>)
     * @param refresh
     *        <code>true</code> to refresh caches, <code>false</code> otherwise
     */
    private TFSServer(final TFSTeamProjectCollection connection, final boolean refresh) {
        Check.notNull(connection, "connection"); //$NON-NLS-1$

        this.connection = connection;

        projectCache = new ServerProjectCache(connection);
        queryDocumentService = new QueryDocumentService(connection);
        buildStatusManager = new BuildStatusManager(connection);

        if (refresh) {
            refresh(true);
        }
    }

    public String getName() {
        return connection.getName();
    }

    public TFSTeamProjectCollection getConnection() {
        return connection;
    }

    public ServerProjectCache getProjectCache() {
        return projectCache;
    }

    public QueryDocumentService getQueryDocumentService() {
        return queryDocumentService;
    }

    public BuildStatusManager getBuildStatusManager() {
        return buildStatusManager;
    }

    public void refresh() {
        refresh(false);
    }

    public void refresh(final boolean async) {
        if (!async) {
            refresh(null);
        } else if (!isRefreshing) {
            final String messageFormat = Messages.getString("TFSServer.ProgressTitleFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, connection.getName());
            ProgressManager.getInstance().run(new Task.Backgroundable(null, message, false) {
                @Override
                public void run(final @NotNull ProgressIndicator indicator) {
                    refresh(indicator);
                }
            });
        }
    }

    private void refresh(final ProgressIndicator indicator) {
        synchronized (refreshLock) {
            isRefreshing = true;

            try {
                if (indicator != null) {
                    String messageFormat = Messages.getString("TFSServer.ProgressStatusFormat"); //$NON-NLS-1$
                    String message = MessageFormat.format(messageFormat, connection.getName());
                    indicator.setText(message);
                    indicator.setFraction(0.0);
                }

                if (indicator != null) {
                    String messageFormat = Messages.getString("TFSServer.ProgressStepRefreshRegistrationFormat"); //$NON-NLS-1$
                    String message = MessageFormat.format(messageFormat, connection.getName());
                    indicator.setText2(message);
                }
                connection.getRegistrationClient().refresh(true);
                if (indicator != null) {
                    indicator.setFraction(0.2);
                }

                if (indicator != null) {
                    String messageFormat = Messages.getString("TFSServer.ProgressStepRefreshProjectFormat"); //$NON-NLS-1$
                    String message = MessageFormat.format(messageFormat, connection.getName());
                    indicator.setText2(message);
                }
                projectCache.refresh();
                if (indicator != null) {
                    indicator.setFraction(0.4);
                }

                if (indicator != null) {
                    String messageFormat = Messages.getString("TFSServer.ProgressStepRefreshingMetadataFormat"); //$NON-NLS-1$
                    String message = MessageFormat.format(messageFormat, connection.getName());
                    indicator.setText2(message);
                }
                connection.getWorkItemClient().refreshCache();
                if (indicator != null) {
                    indicator.setFraction(0.6);
                }

                /* Ensure the server supported features cache is primed */
                if (indicator != null) {
                    String messageFormat = Messages.getString("TFSServer.ProgressStepRefreshingFeaturesFormat"); //$NON-NLS-1$
                    String message = MessageFormat.format(messageFormat, connection.getName());
                    indicator.setText2(message);
                }
                connection.getVersionControlClient().getServerSupportedFeatures();
                if (indicator != null) {
                    indicator.setFraction(0.8);
                }

                if (indicator != null) {
                    String messageFormat = Messages.getString("TFSServer.ProgressStepRefreshingBuildStatusFormat"); //$NON-NLS-1$
                    String message = MessageFormat.format(messageFormat, connection.getName());
                    indicator.setText2(message);
                }
                buildStatusManager.refresh();
                if (indicator != null) {
                    indicator.setFraction(1.0);
                }
            } finally {
                isRefreshing = false;
            }
        }
    }

    /**
     * Closes this TFSServer connection. Should be called whenever disconnecting
     * from a server.
     */
    public void close() {
        buildStatusManager.stop();
    }

    public boolean connectionsEquivalent(final TFSServer server) {
        if (server == null) {
            return false;
        }

        return connectionsEquivalent(server.getConnection());
    }

    public boolean connectionsEquivalent(final TFSTeamProjectCollection connection) {
        if (connection == null) {
            return false;
        }

        return ServerURIUtils.equals(this.getConnection().getBaseURI(), connection.getBaseURI());
    }
}
