// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.github.lizhanyin.tfs.client.ui.framework.command;

import com.github.lizhanyin.tfs.client.framework.command.ICommand;
import com.github.lizhanyin.tfs.client.framework.command.ICommandFinishedCallback;
import com.github.lizhanyin.tfs.client.framework.command.helpers.CommandFinishedCallbackHelpers;
import com.github.lizhanyin.tfs.client.framework.status.UncaughtCommandExceptionStatus;
import com.github.lizhanyin.tfs.runtime.IStatus;
import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;


/**
 * This is a command finished callback suitable for logging messages.
 *
 * This will log statuses with the severity {@link IStatus#ERROR} as errors
 * and statuses with the severity {@link IStatus#WARNING} as warnings.
 * All other messages are logged as info.
 */
public class ConsoleWriterCommandFinishedCallback implements ICommandFinishedCallback {
    /**
     * The minimum severity to log (inclusive.)
     */
    private final int minimumSeverity = IStatus.INFO;

    /**
     * The maximum severity to log (inclusive.) Use this to
     * ignore IStatus.CANCEL going to the log (value 8.)
     */
    private final int maximumSeverity = IStatus.ERROR;

    /**
     * Always log uncaught exceptions, regardless of severity.
     */
    private final boolean alwaysLogUncaughtExceptions = true;

    private static final Logger log = Logger.getInstance(ConsoleWriterCommandFinishedCallback.class);

    @Override
    public void onCommandFinished(@NotNull final ICommand command, @NotNull final IStatus status) {
        if ((status.getSeverity() >= minimumSeverity && status.getSeverity() <= maximumSeverity)
            || (alwaysLogUncaughtExceptions && status instanceof UncaughtCommandExceptionStatus)) {

            final String message = CommandFinishedCallbackHelpers.getMessageForStatus(status);

            if (status.getSeverity() == IStatus.ERROR) {
                if (status.getException() != null) {
                    log.error(message, status.getException());
                } else {
                    log.error(message);
                }
            } else if (status.getSeverity() == IStatus.WARNING) {
                if (status.getException() != null) {
                    log.warn(message, status.getException());
                } else {
                    log.warn(message);
                }
            } else {
                if (status.getException() != null) {
                    log.info(message, status.getException());
                } else {
                    log.info(message);
                }
            }

            if (status instanceof UncaughtCommandExceptionStatus && status.getException() != null) {
                log.info(status.getException().getLocalizedMessage());
            }
        }
    }
}
