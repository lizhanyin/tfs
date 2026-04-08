// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.github.lizhanyin.tfs.client.commands.wizard;

import java.text.MessageFormat;
import java.util.concurrent.atomic.AtomicReference;

import com.github.lizhanyin.tfs.client.commands.TFSConnectedCommand;
import com.intellij.openapi.progress.ProgressIndicator;

import com.github.lizhanyin.tfs.client.Messages;
import com.github.lizhanyin.tfs.runtime.IStatus;
import com.github.lizhanyin.tfs.runtime.Status;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient;
import com.microsoft.tfs.core.clients.versioncontrol.Workstation;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.LocaleUtil;

/**
 * Gets information for all {@link Workspace}s on this computer owned by the
 * authorized user and updates the version control cache file to contain this
 * information.
 * <p>
 * "Local" means the workspace's computer matches this computer name; it doesn't
 * mean a TFS 2012 "Local Workspace".
 */
public class QueryLocalWorkspacesCommand extends TFSConnectedCommand {
    private final VersionControlClient vcClient;
    private volatile Workspace[] workspaces;

    /**
     * Constructs a {@link QueryLocalWorkspacesCommand}.
     *
     * @param connection
     *        the connection to use (must not be <code>null</code>)
     */
    public QueryLocalWorkspacesCommand(final TFSTeamProjectCollection connection) {
        Check.notNull(connection, "connection"); //$NON-NLS-1$

        this.vcClient = connection.getVersionControlClient();

        setConnection(connection);
    }

    @Override
    public String getName() {
        return (Messages.getString("QueryWorkspacesCommand.CommandText")); //$NON-NLS-1$
    }

    @Override
    public String getErrorDescription() {
        return (Messages.getString("QueryWorkspacesCommand.ErrorText")); //$NON-NLS-1$
    }

    @Override
    public String getLoggingDescription() {
        return (Messages.getString("QueryWorkspacesCommand.CommandText", LocaleUtil.ROOT)); //$NON-NLS-1$
    }

    @Override
    protected IStatus doRun(final ProgressIndicator progressIndicator) throws Exception {
        final String messageFormat = Messages.getString("QueryWorkspacesCommand.ProgressMonitorTextFormat"); //$NON-NLS-1$
        final String message = MessageFormat.format(messageFormat, vcClient.getConnection().getName());
        progressIndicator.setText(message);
        progressIndicator.setIndeterminate(true);

        final AtomicReference<Workspace[]> workspacesHolder = new AtomicReference<Workspace[]>();

        Workstation.getCurrent(vcClient.getConnection().getPersistenceStoreProvider()).updateWorkspaceInfoCache(
            vcClient,
            vcClient.getConnection().getAuthorizedTFSUser().toString(),
            workspacesHolder);

        workspaces = workspacesHolder.get();

        if (workspaces == null) {
            workspaces = new Workspace[0];
        }

        return Status.OK_STATUS;
    }

    public Workspace[] getWorkspaces() {
        return workspaces;
    }
}
