// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.github.lizhanyin.tfs.client.framework.command;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;

import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.listeners.SingleListenerFacade;

import java.text.MessageFormat;

public abstract class CancellableCommand implements ICommand, ICancellableCommand {
    private static final Logger log = Logger.getInstance(CancellableCommand.class);

    private final SingleListenerFacade cancellableChangedListeners =
        new SingleListenerFacade(ICommandCancellableListener.class);

    private boolean cancellable = false;

    /**
     * Sets the cancellable state of this command. Subsequent calls to
     * {@link ICommand#isCancellable()} will return the argument.
     *
     * @param cancellable
     *        the cancellable state of this command
     */
    protected void setCancellable(final boolean cancellable) {
        final boolean fireEvent = (cancellable != this.cancellable);

        this.cancellable = cancellable;

        if (fireEvent) {
            ((ICommandCancellableListener) cancellableChangedListeners.getListener()).cancellableChanged(cancellable);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isCancellable() {
        return cancellable;
    }

    /**
     * Adds a cancellable changed listener that will be notified when the
     * cancellability of a command changes.
     *
     * @param listener
     *        The {@link ICommandCancellableListener} that is no longer notified
     *        of cancellability changes (not <code>null</code>)
     */
    @Override
    public void addCancellableChangedListener(final ICommandCancellableListener listener) {
        cancellableChangedListeners.addListener(listener);
    }

    /**
     * Removes a cancellable changed listener.
     *
     * @param listener
     *        The {@link ICommandCancellableListener} that is no longer notified
     *        of cancellability changes (not <code>null</code>)
     */
    @Override
    public void removeCancellableChangedListener(final ICommandCancellableListener listener) {
        cancellableChangedListeners.removeListener(listener);
    }

    /**
     * A convenience method that subclasses can call to do one-line cancellation
     * checks. The given {@link ProgressIndicator} is checked for cancellation. If
     * it is canceled, a {@link ProcessCanceledException} is thrown.
     *
     * @param progressIndicator
     *        a {@link ProgressIndicator} to check for cancellation (must not be
     *        <code>null</code>)
     * @throws ProcessCanceledException
     *         if the given {@link ProgressIndicator} was canceled
     */
    protected final void checkForCancellation(final ProgressIndicator progressIndicator) {
        Check.notNull(progressIndicator, "progressIndicator"); //$NON-NLS-1$

        if (progressIndicator.isCanceled()) {
            if (log.isTraceEnabled()) {
                final String messageFormat =
                    "command [{0}] was canceled - throwing ProcessCanceledException"; //$NON-NLS-1$
                final String message = MessageFormat.format(messageFormat, getClass().getName());
                log.trace(message);
            }

            throw new ProcessCanceledException();
        }
    }
}
