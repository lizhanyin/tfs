// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.github.lizhanyin.tfs.client.ui.framework.command;

import com.github.lizhanyin.tfs.client.framework.command.JobCommandAdapter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;

import com.github.lizhanyin.tfs.client.framework.command.ICommand;
import com.github.lizhanyin.tfs.client.framework.command.ICommandFinishedCallback;
import com.github.lizhanyin.tfs.client.framework.command.ICommandStartedCallback;
import com.github.lizhanyin.tfs.runtime.IStatus;
import com.github.lizhanyin.tfs.runtime.Status;

/**
 * <p>
 * This class adapts an instance of {@link ICommand} to the {@link Task.Backgroundable} class.
 * It is expected to be used with UI-aware CommandFinishedCallbacks, that are
 * capable of raising error dialogs, etc. Thus the status returned is *ALWAYS*
 * {@link IStatus#OK}. This prevents IntelliJ IDEA from raising another error dialog
 * erroneously.
 * </p>
 *
 * @see JobCommandAdapter
 */
public class UIJobCommandAdapter extends JobCommandAdapter {
    private final Object statusLock = new Object();
    private IStatus commandStatus;

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
    public UIJobCommandAdapter(
            @NotNull final ICommand command,
            @Nullable final ICommandStartedCallback startedCallback,
            @Nullable final ICommandFinishedCallback finishedCallback) {
        super(command, startedCallback, finishedCallback);
    }

    /**
     * Note: to prevent IntelliJ IDEA from raising spurious failure dialogs, the
     * status return is *ALWAYS* {@link IStatus#OK}. To get the status of the
     * command execution, call {@link #getCommandStatus()}.
     */
    @Override
    public void run(@NotNull final ProgressIndicator indicator) {
        super.run(indicator);

        synchronized (statusLock) {
            this.commandStatus = getStatus();
        }
    }

    /**
     * Gets the status of the command execution.
     *
     * @return the command execution status
     */
    @NotNull
    public IStatus getCommandStatus() {
        /*
         * Synchronized for visibility - the thread calling run() will be a
         * background thread.
         */
        synchronized (statusLock) {
            return commandStatus != null ? commandStatus : Status.OK_STATUS;
        }
    }
}
