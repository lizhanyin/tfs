// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.github.lizhanyin.tfs.client.ui.framework.command;

import com.github.lizhanyin.tfs.client.framework.command.ExtendedStatus;
import com.github.lizhanyin.tfs.client.framework.command.ICommand;
import com.github.lizhanyin.tfs.client.framework.command.ICommandFinishedCallback;
import com.github.lizhanyin.tfs.client.framework.command.helpers.CommandFinishedCallbackHelpers;
import com.github.lizhanyin.tfs.client.ui.framework.UIContext;
import com.github.lizhanyin.tfs.runtime.IStatus;
import com.github.lizhanyin.tfs.runtime.Status;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.awt.Component;

/**
 * <p>
 * An {@link ICommandFinishedCallback} implementation that displays an
 * error dialog if the {@link IStatus} produced by running a command
 * meets certain criteria.
 * </p>
 *
 * <p>
 * If the status produced by running an {@link ICommand} is an
 * {@link ExtendedStatus}, then the
 * {@link ExtendedStatus#SHOW_MESSAGE_IN_DIALOG} flag is used to determine
 * whether to show a dialog. Otherwise, an error dialog is
 * displayed only if the status has a severity of {@link IStatus#ERROR}.
 * </p>
 *
 * @see ICommand
 * @see ExtendedStatus
 */
public class ErrorDialogCommandFinishedCallback implements ICommandFinishedCallback {
    private final UIContext uiContext;

    /**
     * Creates a new {@link ErrorDialogCommandFinishedCallback}. The specified
     * {@link UIContext} is used as the context when showing the error dialog.
     *
     * @param uiContext
     *        the {@link UIContext} (may be <code>null</code>)
     */
    public ErrorDialogCommandFinishedCallback(@Nullable final UIContext uiContext) {
        this.uiContext = uiContext;
    }

    @Override
    public void onCommandFinished(@NotNull final ICommand command, @NotNull final IStatus status) {
        boolean showErrorDialog = false;
        boolean suppressException = false;

        if (status instanceof ExtendedStatus) {
            showErrorDialog = ((ExtendedStatus) status).hasFlags(ExtendedStatus.SHOW_MESSAGE_IN_DIALOG);
            suppressException = ((ExtendedStatus) status).hasFlags(ExtendedStatus.MESSAGE_FROM_EXCEPTION);
        } else {
            showErrorDialog = status.getSeverity() == IStatus.ERROR;
        }

        if (showErrorDialog) {
            final String title = command.getErrorDescription();

            IStatus displayStatus = status;
            if (suppressException) {
                displayStatus = new Status(
                    status.getSeverity(),
                    status.getPlugin(),
                    status.getCode(),
                    status.getMessage(),
                    null);
            }

            final String message = CommandFinishedCallbackHelpers.getMessageForStatus(displayStatus);

            final Icon icon;
            if (status.getSeverity() == IStatus.ERROR) {
                icon = Messages.getErrorIcon();
            } else if (status.getSeverity() == IStatus.WARNING) {
                icon = Messages.getWarningIcon();
            } else {
                icon = Messages.getInformationIcon();
            }

            final String dialogTitle = title != null ? title : "TFS";
            final Icon dialogIcon = icon;

            // Show dialog on EDT
            if (ApplicationManager.getApplication().isDispatchThread()) {
                showMessageDialog(message, dialogTitle, dialogIcon);
            } else {
                ApplicationManager.getApplication().invokeLater(() -> {
                    showMessageDialog(message, dialogTitle, dialogIcon);
                });
            }
        }
    }

    private void showMessageDialog(final String message, final String title, final Icon icon) {
        if (uiContext == null) {
            Messages.showMessageDialog(message, title, icon);
            return;
        }

        final Component parent = uiContext.getParentComponent();
        if (parent != null) {
            Messages.showMessageDialog(parent, message, title, icon);
            return;
        }

        final Project project = uiContext.getProject();
        if (project != null) {
            Messages.showMessageDialog(project, message, title, icon);
            return;
        }

        Messages.showMessageDialog(message, title, icon);
    }
}
