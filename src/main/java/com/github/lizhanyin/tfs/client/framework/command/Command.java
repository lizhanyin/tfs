// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.github.lizhanyin.tfs.client.framework.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.EmptyProgressIndicator;

import com.github.lizhanyin.tfs.runtime.IStatus;
import com.github.lizhanyin.tfs.client.framework.command.exception.ICommandExceptionHandler;
import com.github.lizhanyin.tfs.client.framework.command.exception.MultiCommandExceptionHandler;
import com.github.lizhanyin.tfs.client.framework.command.exception.NullCommandExceptionHandler;

import com.microsoft.tfs.util.Check;

/**
 * <p>
 * A convenience abstract implementation of the <code>ICommand</code> interface.
 * Clients subclass this class and need provide only an implementation of the
 * abstract {@link #doRun(ProgressIndicator)} method.
 * </p>
 *
 * <p>
 * This implementation contains private fields to store the name and cancelable
 * state of this command. By default, subclasses are not cancelable and the
 * command name is the short name of the subclass. Subclasses can override these
 * defaults by calling {@link #setCancellable(boolean)} and
 * setName(String). In addition, this implementation guarantees that
 * the {@link ProgressIndicator} instance passed to subclasses in the
 * {@link #doRun(ProgressIndicator)} method is non-<code>null</code>.
 * </p>
 *
 * <p>
 * This base class does not provide an {@link ICommandExceptionHandler}.
 * Subclasses may override the {@link #getExceptionHandler()} method and return
 * an exception handler if needed.
 * </p>
 *
 * @see ICommand
 */
public abstract class Command extends CancellableCommand implements ICommand {
    private static final Logger log = Logger.getInstance(Command.class);

    private final List<ICommandExceptionHandler> exceptionHandlerList = new ArrayList<>();

    /**
     * Subclasses may add initialization / completion runnables. For example,
     * another base command class may perform the same routines at all
     * initialization. This allows the base class to do this (without
     * implementing doRun() and requiring concrete classes to override yet
     * another run method.)
     */
    private final List<CommandInitializationRunnable> initializationRunnables = new ArrayList<>();

    /**
     * Subclasses must override this method to perform the actual work of this
     * command.
     *
     * @param progressIndicator
     *        A {@link ProgressIndicator} to use (guaranteed to not be
     *        <code>null</code>).
     * @return the outcome of this command run as an {@link IStatus} (see
     *         {@link ICommand#run(ProgressIndicator)})
     * @throws Exception e
     */
    protected abstract IStatus doRun(@NotNull ProgressIndicator progressIndicator) throws Exception;

    /**
     * {@inheritDoc}
     */
    @Override
    public final IStatus run(@Nullable ProgressIndicator progressIndicator) throws Exception {
        if (progressIndicator == null) {
            progressIndicator = new EmptyProgressIndicator();
        }

        progressIndicator.setText(getName());

        try {
            for (final CommandInitializationRunnable initializationRunnable : initializationRunnables) {
                initializationRunnable.initialize(progressIndicator);
            }

            return doRun(progressIndicator);
        } finally {
            /*
             * Run the completion runnables - if there's an exception in any of
             * them, keep processing them and do not propagate the exception.
             */
            for (final CommandInitializationRunnable initializationRunnable : initializationRunnables) {
                try {
                    initializationRunnable.complete(progressIndicator);
                } catch (final Throwable t) {
                    log.info("Caught exception in command completion runnable", t); //$NON-NLS-1$
                }
            }
        }
    }

    /**
     * Sets the exception handler for this command.
     *
     * @param handler
     *        The {@link ICommandExceptionHandler} to use for exceptions.
     */
    protected final void setExceptionHandler(@NotNull final ICommandExceptionHandler handler) {
        setExceptionHandlers(new ICommandExceptionHandler[] { handler });
    }

    /**
     * Sets the list of exception handlers for this command. Exception handlers
     * will be called in order (index 0 first, then index 1), and the first
     * returning an IStatus will make the IStatus for this command.
     *
     * @param handlers
     *        The {@link ICommandExceptionHandler}s to use for exceptions.
     */
    protected final void setExceptionHandlers(@NotNull final ICommandExceptionHandler[] handlers) {
        exceptionHandlerList.clear();
        exceptionHandlerList.addAll(Arrays.asList(handlers));
    }

    /**
     * Appends the given exception handler to the front of the exception handler
     * list. That is, this will be called first on an exception.
     *
     * @param handler
     *        The exception handler to add.
     */
    protected final void addExceptionHandler(@NotNull final ICommandExceptionHandler handler) {
        exceptionHandlerList.add(0, handler);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ICommandExceptionHandler getExceptionHandler() {
        if (exceptionHandlerList.isEmpty()) {
            return new NullCommandExceptionHandler();
        } else {
            final ICommandExceptionHandler[] exceptionHandlers =
                exceptionHandlerList.toArray(new ICommandExceptionHandler[0]);
            return new MultiCommandExceptionHandler(exceptionHandlers);
        }
    }

    /**
     * Adds a {@link CommandInitializationRunnable} that will be called before
     * and after command subclass execution. Runnables will be executed in the
     * order they are added, and if the command subclass's
     * {@link #doRun(ProgressIndicator)} method throws an exception, the cleanup
     * methods will still be executed.
     *
     * @param runnable
     *        The {@link CommandInitializationRunnable} to add.
     */
    public void addCommandInitializationRunnable(@NotNull final CommandInitializationRunnable runnable) {
        Check.notNull(runnable, "runnable"); //$NON-NLS-1$

        initializationRunnables.add(runnable);
    }
}
