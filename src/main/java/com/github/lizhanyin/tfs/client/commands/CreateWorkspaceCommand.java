// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.github.lizhanyin.tfs.client.commands;

import java.text.MessageFormat;

import com.intellij.openapi.progress.ProgressIndicator;

import com.github.lizhanyin.tfs.client.Messages;
import com.github.lizhanyin.tfs.runtime.IStatus;
import com.github.lizhanyin.tfs.runtime.Status;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient;
import com.microsoft.tfs.core.clients.versioncontrol.WorkspaceLocation;
import com.microsoft.tfs.core.clients.versioncontrol.WorkspaceOptions;
import com.microsoft.tfs.core.clients.versioncontrol.WorkspacePermissionProfile;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.WorkingFolder;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.LocaleUtil;

public class CreateWorkspaceCommand extends TFSConnectedCommand {
    private final VersionControlClient vcClient;
    private final WorkingFolder[] workingFolders;
    private final String workspaceName;
    private final String comment;
    private final WorkspaceLocation location;
    private final WorkspaceOptions options;
    private final WorkspacePermissionProfile permissionProfile;

    private volatile Workspace workspace;

    public CreateWorkspaceCommand(
        final TFSTeamProjectCollection connection,
        final WorkingFolder[] workingFolders,
        final String workspaceName,
        final String comment,
        final WorkspaceLocation location,
        final WorkspaceOptions options,
        final WorkspacePermissionProfile permissionProfile) {
        Check.notNull(connection, "connection"); //$NON-NLS-1$
        Check.notNull(workspaceName, "workspaceName"); //$NON-NLS-1$

        this.vcClient = connection.getVersionControlClient();
        this.workingFolders = workingFolders;
        this.workspaceName = workspaceName;
        this.comment = comment;
        this.location = location;
        this.options = options;
        this.permissionProfile = permissionProfile;

        setConnection(connection);
    }

    @Override
    public String getName() {
        final String messageFormat = Messages.getString("CreateWorkspaceCommand.CommandTextFormat"); //$NON-NLS-1$
        return MessageFormat.format(messageFormat, workspaceName);
    }

    @Override
    public String getErrorDescription() {
        final String messageFormat = Messages.getString("CreateWorkspaceCommand.ErrorTextFormat"); //$NON-NLS-1$
        return MessageFormat.format(messageFormat, workspaceName);
    }

    @Override
    public String getLoggingDescription() {
        final String messageFormat = Messages.getString("CreateWorkspaceCommand.CommandTextFormat", LocaleUtil.ROOT); //$NON-NLS-1$
        return MessageFormat.format(messageFormat, workspaceName);
    }

    @Override
    protected IStatus doRun(final ProgressIndicator progressIndicator) throws Exception {
        final String messageFormat = Messages.getString("CreateWorkspaceCommand.ProgressMonitorTextFormat"); //$NON-NLS-1$
        final String message = MessageFormat.format(messageFormat, workspaceName);
        progressIndicator.setText(message);
        progressIndicator.setIndeterminate(true);

        workspace = vcClient.createWorkspace(
            workingFolders,
            workspaceName,
            comment == null ? "" : comment, //$NON-NLS-1$
            location,
            options,
            permissionProfile);

        return Status.OK_STATUS;
    }

    public Workspace getWorkspace() {
        return workspace;
    }
}
