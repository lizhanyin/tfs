// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.github.lizhanyin.tfs.client.commands;

import com.github.lizhanyin.tfs.client.catalog.TeamProjectCollectionInfo;
import com.github.lizhanyin.tfs.runtime.IStatus;
import com.github.lizhanyin.tfs.runtime.Status;
import com.microsoft.tfs.core.Messages;

import com.intellij.openapi.progress.ProgressIndicator;

import com.microsoft.tfs.core.TFSConfigurationServer;
import com.microsoft.tfs.core.clients.framework.configuration.entities.ProjectCollectionEntity;
import com.microsoft.tfs.core.clients.framework.configuration.entities.TeamFoundationServerEntity;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.LocaleUtil;
import org.jetbrains.annotations.NotNull;

public class QueryProjectCollectionsCommand extends TFSConnectedCommand {

    private final TFSConfigurationServer connection;

    private TeamProjectCollectionInfo[] collections = null;

    public QueryProjectCollectionsCommand(final TFSConfigurationServer connection) {
        Check.notNull(connection, "connection"); //$NON-NLS-1$

        this.connection = connection;

        setConnection(connection);
    }

    @Override
    public String getName() {
        return (Messages.getString("QueryProjectCollectionsCommand.CommandText")); //$NON-NLS-1$
    }

    @Override
    public String getErrorDescription() {
        return (Messages.getString("QueryProjectCollectionsCommand.ErrorText")); //$NON-NLS-1$
    }

    @Override
    public String getLoggingDescription() {
        return (Messages.getString("QueryProjectCollectionsCommand.CommandText", LocaleUtil.ROOT)); //$NON-NLS-1$
    }

    @Override
    protected IStatus doRun(@NotNull final ProgressIndicator progressIndicator) throws Exception {
        progressIndicator.setText(
            Messages.getString("QueryProjectCollectionsCommand.ProgressMonitorText")); //$NON-NLS-1$

        final TeamFoundationServerEntity teamFoundationServer = connection.getTeamFoundationServerEntity(true);

        if (teamFoundationServer != null) {
            final ProjectCollectionEntity[] projectCollections = teamFoundationServer.getProjectCollections();

            if (projectCollections != null) {
                collections = new TeamProjectCollectionInfo[projectCollections.length];

                for (int i = 0; i < projectCollections.length; i++) {
                    collections[i] = new TeamProjectCollectionInfo(
                        projectCollections[i].getInstanceID(),
                        projectCollections[i].getDisplayName(),
                        projectCollections[i].getDescription());
                }
            }
        }

        return Status.OK_STATUS;
    }

    public TeamProjectCollectionInfo[] getProjectCollections() {
        return collections;
    }
}
