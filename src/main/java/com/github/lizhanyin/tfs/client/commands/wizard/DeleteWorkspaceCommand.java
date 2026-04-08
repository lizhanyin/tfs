// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.github.lizhanyin.tfs.client.commands.wizard;

import java.text.MessageFormat;

import com.github.lizhanyin.tfs.client.commands.TFSConnectedCommand;
import com.intellij.openapi.progress.ProgressIndicator;

import com.github.lizhanyin.tfs.client.Messages;
import com.github.lizhanyin.tfs.runtime.IStatus;
import com.github.lizhanyin.tfs.runtime.Status;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.LocaleUtil;

public class DeleteWorkspaceCommand extends TFSConnectedCommand {
    private final Workspace workspace;

    public DeleteWorkspaceCommand(final Workspace workspace) {
        Check.notNull(workspace, "workspace"); //$NON-NLS-1$

        this.workspace = workspace;

        setConnection(workspace.getClient().getConnection());
    }

    @Override
    public String getName() {
        final String messageFormat = Messages.getString("DeleteWorkspaceCommand.CommandTextFormat"); //$NON-NLS-1$
        return MessageFormat.format(messageFormat, workspace.getName());
    }

    @Override
    public String getErrorDescription() {
        final String messageFormat = Messages.getString("DeleteWorkspaceCommand.ErrorTextFormat"); //$NON-NLS-1$
        return MessageFormat.format(messageFormat, workspace.getName());
    }

    @Override
    public String getLoggingDescription() {
        final String messageFormat = Messages.getString("DeleteWorkspaceCommand.CommandTextFormat", LocaleUtil.ROOT); //$NON-NLS-1$
        return MessageFormat.format(messageFormat, workspace.getName());
    }

    @Override
    protected IStatus doRun(final ProgressIndicator progressIndicator) throws Exception {
        final String messageFormat = Messages.getString("DeleteWorkspaceCommand.ProgressMonitorTextFormat"); //$NON-NLS-1$
        final String message = MessageFormat.format(messageFormat, workspace.getName());
        progressIndicator.setText(message);
        progressIndicator.setIndeterminate(true);

        workspace.getClient().deleteWorkspace(workspace);

        return Status.OK_STATUS;
    }
}
