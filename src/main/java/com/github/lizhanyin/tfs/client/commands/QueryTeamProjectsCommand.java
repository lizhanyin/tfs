// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.github.lizhanyin.tfs.client.commands;

import com.github.lizhanyin.tfs.client.Messages;
import com.github.lizhanyin.tfs.runtime.IStatus;
import com.github.lizhanyin.tfs.runtime.Status;
import com.intellij.openapi.progress.ProgressIndicator;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.commonstructure.CommonStructureClient;
import com.microsoft.tfs.core.clients.commonstructure.ProjectInfo;
import com.microsoft.tfs.util.LocaleUtil;
import org.jetbrains.annotations.NotNull;

/**
 * Command to query the Classification Service and obtain information about all
 * the Team Projects.
 */
public class QueryTeamProjectsCommand extends TFSConnectedCommand {
    private final TFSTeamProjectCollection connection;

    private ProjectInfo[] projects = null;

    public QueryTeamProjectsCommand(final TFSTeamProjectCollection connection) {
        this.connection = connection;

        setConnection(connection);
    }

    @Override
    public String getName() {
        return (Messages.getString("QueryTeamProjectsCommand.CommandText")); //$NON-NLS-1$
    }

    @Override
    public String getErrorDescription() {
        return (Messages.getString("QueryTeamProjectsCommand.ErrorText")); //$NON-NLS-1$
    }

    @Override
    public String getLoggingDescription() {
        return (Messages.getString("QueryTeamProjectsCommand.CommandText", LocaleUtil.ROOT)); //$NON-NLS-1$
    }

    @Override
    protected IStatus doRun(@NotNull final ProgressIndicator progressIndicator) throws Exception {
        progressIndicator.setText(
            Messages.getString("QueryTeamProjectsCommand.ProgressMonitorTitle")); //$NON-NLS-1$

        final CommonStructureClient cssClient =
            (CommonStructureClient) connection.getClient(CommonStructureClient.class);
        projects = cssClient.listProjects();

        return Status.OK_STATUS;
    }

    public ProjectInfo[] getProjects() {
        return projects;
    }
}