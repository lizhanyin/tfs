// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.github.lizhanyin.tfs.client.ui.framework.command;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;

import com.github.lizhanyin.tfs.client.framework.command.ICommand;
import com.github.lizhanyin.tfs.client.framework.command.ICommandFinishedCallback;
import com.github.lizhanyin.tfs.client.framework.command.ICommandStartedCallback;
import com.github.lizhanyin.tfs.client.framework.command.exception.CommandExceptionHandlerUtils;
import com.github.lizhanyin.tfs.runtime.IStatus;
import com.github.lizhanyin.tfs.runtime.Status;

import com.microsoft.tfs.util.Check;

/**
 * <p>
 * This class adapts an instance of {@link ICommand} to the {@link Runnable}
 * interface.
 * </p>
 *
 * <p>
 * The {@link Runnable#run()} interface method is implemented by directly
 * calling the {@link ICommand#run(ProgressIndicator)} method of the command
 * being wrapped by this adapter. Any exception thrown by the command will be
 * converted to an {@link IStatus} by calling
 * {@link CommandExceptionHandlerUtils#handleCommandException(ICommand, Throwable)}.
 * This wrapper can optionally take an {@link ICommandFinishedCallback} that
 * is called back after running the command and producing an {@link IStatus}.
 * After the runnable has finished, the status produced by the command run is
 * available by calling the {@link #getStatus()} method.
 * </p>
 *
 * @see ICommand
 * @see Runnable
 * @see ICommandFinishedCallback
 */
public class RunnableCommandAdapter implements Runnable {
    private static final Logger log = Logger.getInstance(RunnableCommandAdapter.class);

    private final ICommand command;
    private final ProgressIndicator progressIndicator;
    private final ICommandStartedCallback startedCallback;
    private final ICommandFinishedCallback finishedCallback;

    private IStatus status;

    /**
     * Creates a new {@link RunnableCommandAdapter}, adapting the given
     * {@link ICommand} to the {@link Runnable} interface.
     *
     * @param command
     *        the {@link ICommand} to adapt (must not be <code>null</code>)
     * @param progressIndicator
     *        an optional {@link ProgressIndicator} to pass to the
     *        {@link ICommand#run(ProgressIndicator)} method (may be
     *        <code>null</code>)
     * @param startedCallback
     *        an optional {@link ICommandStartedCallback} to call back to before
     *        the command has finished (may be <code>null</code>)
     * @param finishedCallback
     *        an optional {@link ICommandFinishedCallback} to call back to when
     *        the command has finished (may be <code>null</code>)
     */
    public RunnableCommandAdapter(
        @NotNull final ICommand command,
        @Nullable final ProgressIndicator progressIndicator,
        @Nullable final ICommandStartedCallback startedCallback,
        @Nullable final ICommandFinishedCallback finishedCallback) {
        Check.notNull(command, "command"); //$NON-NLS-1$

        this.command = command;
        this.progressIndicator = progressIndicator;
        this.startedCallback = startedCallback;
        this.finishedCallback = finishedCallback;
    }

    @Override
    public void run() {
        status = null;

        if (startedCallback != null) {
            try {
                startedCallback.onCommandStarted(command);
            } catch (final Throwable t) {
                log.error("Command started callback failed", t); //$NON-NLS-1$
            }
        }

        try {
            status = command.run(progressIndicator);
            if (status == null) {
                status = Status.OK_STATUS;
            }
        } catch (final Exception e) {
            status = CommandExceptionHandlerUtils.handleCommandException(command, e);
        }

        if (finishedCallback != null) {
            finishedCallback.onCommandFinished(command, status);
        }
    }

    /**
     * @return the {@link IStatus} produced by the last run of this adapter, or
     *         <code>null</code> if it has never run
     */
    @Nullable
    public IStatus getStatus() {
        return status;
    }
}
