// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.github.lizhanyin.tfs.client.ui.framework.command;

import java.lang.reflect.InvocationTargetException;

import com.github.lizhanyin.tfs.client.framework.command.CommandExecutor;
import com.github.lizhanyin.tfs.client.framework.command.ICommand;
import com.github.lizhanyin.tfs.client.framework.command.ICommandExecutor;
import com.github.lizhanyin.tfs.client.ui.framework.UIContext;
import com.github.lizhanyin.tfs.runtime.IStatus;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;


/**
 * <p>
 * An abstract {@link ICommandExecutor} base class that is used to build command
 * executors that run commands using IntelliJ's {@link ProgressManager}.
 * </p>
 *
 * <p>
 * Subclasses need only provide an implementation of
 * {@link #getProgressIndicator(ICommand)}.
 * This method is called each time a command is executed and should return a
 * valid {@link ProgressIndicator} to use when running the command.
 * </p>
 *
 * @see ICommandExecutor
 * @see ProgressIndicator
 */
public abstract class AbstractRunnableContextCommandExecutor extends AbstractUICommandExecutor {
    private static final Logger log = Logger.getInstance(CommandExecutor.class);

    protected AbstractRunnableContextCommandExecutor(final UIContext uiContext) {
        super(uiContext);
    }

    /**
     * Subclasses must implement this method and return a
     * {@link ProgressIndicator} to run the given {@link ICommand} with. This
     * method is called every time a command is executed with this executor.
     *
     * @param command
     *        the {@link ICommand} about to be executed (never <code>null</code>
     *        )
     * @return a {@link ProgressIndicator} to use (must not be <code>null</code>
     *         )
     */
    protected abstract ProgressIndicator getProgressIndicator(ICommand command);

    /*
     * (non-Javadoc)
     *
     * @see
     * com.github.lizhanyin.tfs.client.framework.command.CommandExecutor#execute
     * (com.github.lizhanyin.tfs.client.framework.command.ICommand)
     */
    @Override
    public IStatus execute(final ICommand command) {
        final ProgressIndicator indicator = getProgressIndicator(command);

        IStatus status = null;

        try {
            getCommandStartedCallback().onCommandStarted(command);
        } catch (final Throwable t) {
            log.error("Command started callback failed", t); //$NON-NLS-1$
        }

        try {
            status = command.run(indicator);
            if (status == null) {
                status = com.github.lizhanyin.tfs.runtime.Status.OK_STATUS;
            }
        } catch (final Exception e) {
            Throwable t = e;

            /*
             * InvocationTargetException should be unwrapped.
             */
            if (t instanceof InvocationTargetException) {
                t = ((InvocationTargetException) t).getCause();
            }

            status = handleCommandException(command, t);
        }

        getCommandFinishedCallback().onCommandFinished(command, status);

        return status;
    }
}
