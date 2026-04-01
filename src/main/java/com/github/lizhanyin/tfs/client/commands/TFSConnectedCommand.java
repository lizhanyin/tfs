// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.github.lizhanyin.tfs.client.commands;


import com.github.lizhanyin.tfs.client.framework.command.CommandInitializationRunnable;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.microsoft.tfs.core.TFSConnection;
import com.microsoft.tfs.util.Check;

/**
 * An extension of the basic command but that requires you have a valid
 * {@link TFSConnection}. If the connection has not yet been made to the server,
 * or if the last SOAP request failed, the connection will be rebuild in a
 * cancellable and friendly manner.
 */
public abstract class TFSConnectedCommand extends TFSCommand {
    protected TFSConnection connection;

    protected TFSConnectedCommand() {
        addCommandInitializationRunnable(new TFSCommandInitializationRunnable());
    }

    protected TFSConnectedCommand(final TFSConnection connection) {
        this();

        Check.notNull(connection, "connection"); //$NON-NLS-1$

        this.connection = connection;
    }

    protected void setConnection(final TFSConnection connection) {
        Check.notNull(connection, "connection"); //$NON-NLS-1$

        this.connection = connection;
    }

    /**
     * Subclasses may call to ensure that there is a valid connection to the TFS
     * server. It is recommended that all commands that intend to use a server
     * connection call this method. (Commands that take advantage strictly of
     * local workspaces need not call this method.)
     *
     * @param connection
     *        the current {@link TFSConnection} (not <code>null</code>)
     * @param progressIndicator
     *        a progress indicator (or <code>null</code>)
     */
    protected void ensureConnected(final TFSConnection connection, final ProgressIndicator progressIndicator)
        throws Exception {
        Check.notNull(connection, "connection"); //$NON-NLS-1$

        /*
         * If we have a valid connection, simply use it.
         */
        if (connection.hasAuthenticated() && !connection.getConnectivityFailureOnLastWebServiceCall()) {
            return;
        }

        /*
         * Otherwise, the last web service call failed (or we have never even
         * tried.) Try again to authenticate. Do so in a background thread so
         * that users can cancel.
         */
        final TFSCommandConnectionRunnable connectionRunnable = new TFSCommandConnectionRunnable(connection);
        new Thread(connectionRunnable, "TFS Connection").start(); //$NON-NLS-1$

        final boolean commandCancellable = isCancellable();
        setCancellable(true);

        progressIndicator.setText("Connecting to TFS..."); //$NON-NLS-1$

        while (!connectionRunnable.isComplete()) {
            if (progressIndicator.isCanceled()) {
                throw new ProcessCanceledException();
            }

            Thread.sleep(100);
        }

        if (connectionRunnable.getConnectionFailure() != null) {
            throw connectionRunnable.getConnectionFailure();
        }

        setCancellable(commandCancellable);

        /*
         * Recheck cancellation, as it may have occurred before resetting the
         * cancellability state.
         */
        if (progressIndicator.isCanceled()) {
            throw new ProcessCanceledException();
        }

        progressIndicator.setText(getName());
    }

    private class TFSCommandInitializationRunnable implements CommandInitializationRunnable {
        @Override
        public void initialize(final ProgressIndicator progressIndicator) throws Exception {
            if (connection != null) {
                ensureConnected(connection, progressIndicator);
            }
        }

        @Override
        public void complete(final ProgressIndicator progressIndicator) {
        }
    }

    private static class TFSCommandConnectionRunnable implements Runnable {
        private final TFSConnection connection;

        private final Object connectionStatusLock = new Object();
        private boolean connectionComplete = false;
        private Exception connectionFailure = null;

        private TFSCommandConnectionRunnable(final TFSConnection connection) {
            Check.notNull(connection, "connection"); //$NON-NLS-1$

            this.connection = connection;
        }

        @Override
        public void run() {
            Exception failure = null;

            try {
                connection.authenticate();
            } catch (final Exception e) {
                failure = e;
            }

            synchronized (connectionStatusLock) {
                connectionComplete = true;
                connectionFailure = failure;
            }
        }

        public boolean isComplete() {
            synchronized (connectionStatusLock) {
                return connectionComplete;
            }
        }

        public Exception getConnectionFailure() {
            return connectionFailure;
        }
    }
}
