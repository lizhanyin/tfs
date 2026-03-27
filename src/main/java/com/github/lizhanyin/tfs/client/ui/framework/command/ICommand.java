// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.github.lizhanyin.tfs.client.ui.framework.command;

import com.intellij.openapi.progress.ProgressIndicator;

import com.github.lizhanyin.tfs.client.ui.framework.command.Command;
import com.github.lizhanyin.tfs.client.ui.framework.command.ICommandExecutor ;

/**
 * <p>
 * An {@link ICommand} represents a task to be performed.
 * </p>
 *
 * <p>
 * Commands are implemented by directly implementing the {@link ICommand}
 * interface, or more often by subclassing a common base class, such as
 * {@link Command}.
 * </p>
 *
 * <p>
 * Note that commands are generally stateful, and are generally not designed to
 * be run by more than one thread at a time. A command implementation usually
 * takes its "input" in its constructor, and returns its "output" to clients
 * through public accessor methods not defined in this interface.
 * </p>
 *
 * <p>
 * Commands can be run simply by calling the <code>run()</code> method directly.
 * More often, clients use an {@link ICommandExecutor} to execute a command.
 * Using a command executor provides many advantages. See the documentation for
 * {@link ICommandExecutor} for more details.
 * </p>
 *
 * @see ICommandExecutor
 */
public interface ICommand {
    /**
     * <p>
     * The "do work" method of an {@link ICommand}.
     * </p>
     *
     * <p>
     * The <code>progressIndicator</code> argument, if not <code>null</code>,
     * should be used to report the progress of this command as it runs. Callers
     * of this method are <b>not</b> required to supply a progress indicator.
     * {@link ICommand} implementations <b>must not</b> expect the
     * <code>progressIndicator</code> argument to be non-<code>null</code>.
     * However, if the argument is non-<code>null</code>, the command should
     * ideally make use of the supplied progress indicator.
     * </p>
     *
     * <p>
     * This method returns a boolean to indicate success or failure.
     * Returns <code>true</code> if the command completed successfully,
     * <code>false</code> otherwise.
     * </p>
     *
     * <p>
     * If a command returns early because it has been cancelled (either through
     * the supplied {@link ProgressIndicator} or through some other means) it
     * should throw {@link com.intellij.openapi.progress.ProcessCanceledException}.
     * </p>
     *
     * <p>
     * A command may throw any exception from this method. Clients will have to
     * decide how such exceptions should be handled.
     * </p>
     *
     * @param progressIndicator
     *        an optional {@link ProgressIndicator} for the command to use
     * @return <code>true</code> if the command completed successfully
     * @throws Exception
     *         if an error occurs during execution
     * @throws com.intellij.openapi.progress.ProcessCanceledException
     *         if the command was cancelled
     */
    boolean run(ProgressIndicator progressIndicator) throws Exception;

    /**
     * <p>
     * Called to determine whether or not this {@link ICommand} is cancellable.
     * An {@link ICommand} is considered cancellable if it checks the
     * <code>isCanceled()</code> property of its {@link ProgressIndicator} when
     * run and returns early if <code>isCanceled()</code> returns
     * <code>true</code>.
     * </p>
     *
     * <p>
     * This method is often used in order to decide how to render a UI when
     * running an {@link ICommand}. For example, the UI may have a cancel button
     * that allows the user to stop the command before it finishes. The return
     * value from this method could be used to decide visibility or enablement
     * of such a cancel button.
     * </p>
     *
     * @return <code>true</code> if this {@link ICommand} is cancellable as
     *         described above
     */
    boolean isCancellable();

    /**
     * Called to obtain an end user readable name for this {@link ICommand}. For
     * example, this name could be used in the UI when displaying the progress
     * of a running command.
     *
     * @return a descriptive name for this {@link ICommand} (must not be
     *         <code>null</code>)
     */
    String getName();

    /**
     * Called to obtain an "error description" which is used if this command
     * fails. For example, "An error occurred while checking out files".
     *
     * @return a descriptive name for the error case of this {@link ICommand}
     *         (must not be <code>null</code>)
     */
    String getErrorDescription();

    /**
     * Called to obtain a "logging description" which is logged to the product
     * log file when this command runs. This may be more informative than the
     * "name" (used for UI presentation). On null, no message should be logged.
     *
     * @return a descriptive piece of information for the log (or
     *         <code>null</code>)
     */
    String getLoggingDescription();
}
