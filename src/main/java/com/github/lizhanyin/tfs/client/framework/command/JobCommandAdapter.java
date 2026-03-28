// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.github.lizhanyin.tfs.client.framework.command;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;

import com.github.lizhanyin.tfs.runtime.IStatus;
import com.github.lizhanyin.tfs.runtime.Status;
import com.github.lizhanyin.tfs.client.framework.command.exception.CommandExceptionHandlerUtils;

/**
 * <p>
 * This class adapts an instance of {@link ICommand} to the {@link Task.Backgroundable} class.
 * </p>
 *
 * <p>
 * The {@link Task.Backgroundable#run(ProgressIndicator)} method is implemented by directly
 * calling the {@link ICommand#run(ProgressIndicator)} method of the command
 * being wrapped by this adapter. Any exception thrown by the command will be
 * converted to an {@link IStatus} by calling
 * {@link CommandExceptionHandlerUtils#handleCommandException(ICommand, Throwable)}.
 * This wrapper can optionally take an {@link ICommandFinishedCallback} that
 * is called back after running the command and producing an {@link IStatus}.
 * </p>
 *
 * @see ICommand
 * @see Task.Backgroundable
 * @see ICommandFinishedCallback
 */
public class JobCommandAdapter extends Task.Backgroundable {
    private static final Logger log = Logger.getInstance(JobCommandAdapter.class);

    private final ICommand command;
    private final ICommandStartedCallback startedCallback;
    private final ICommandFinishedCallback finishedCallback;

    private final Object statusLock = new Object();
    private IStatus status;

    public JobCommandAdapter(@NotNull final ICommand command) {
        this(command, null, null);
    }

    /**
     * Creates a new {@link JobCommandAdapter}, adapting the given
     * {@link ICommand} to the {@link Task.Backgroundable} class.
     *
     * @param command
     *        the {@link ICommand} to adapt (must not be <code>null</code>)
     * @param startedCallback
     *        an optional {@link ICommandStartedCallback} to call back to before
     *        the command has started (may be <code>null</code>)
     * @param finishedCallback
     *        an optional {@link ICommandFinishedCallback} to call back to when
     *        the command has finished (may be <code>null</code>)
     */
    public JobCommandAdapter(
            @NotNull final ICommand command,
            @Nullable final ICommandStartedCallback startedCallback,
            @Nullable final ICommandFinishedCallback finishedCallback) {
        super(null, command.getName(), true);

        this.command = command;
        this.startedCallback = startedCallback;
        this.finishedCallback = finishedCallback;
    }

    @Override
    public void run(@NotNull final ProgressIndicator indicator) {
        IStatus result;

        if (startedCallback != null) {
            try {
                startedCallback.onCommandStarted(command);
            } catch (final Throwable t) {
                log.error("Command started callback failed", t); //$NON-NLS-1$
            }
        }

        try {
            result = command.run(indicator);
            if (result == null) {
                result = Status.OK_STATUS;
            }
        } catch (final Exception e) {
            result = CommandExceptionHandlerUtils.handleCommandException(command, e);
        }

        synchronized (statusLock) {
            this.status = result;
        }
    }

    @Override
    public void onSuccess() {
        if (finishedCallback != null) {
            finishedCallback.onCommandFinished(command, getStatus());
        }
    }

    @Override
    public void onThrowable(@NotNull final Throwable error) {
        synchronized (statusLock) {
            this.status = CommandExceptionHandlerUtils.handleCommandException(command, error);
        }

        if (finishedCallback != null) {
            finishedCallback.onCommandFinished(command, getStatus());
        }
    }

    /**
     * Gets the status of the command execution.
     *
     * @return the command execution status
     */
    @NotNull
    public IStatus getStatus() {
        synchronized (statusLock) {
            return status != null ? status : Status.OK_STATUS;
        }
    }
}
