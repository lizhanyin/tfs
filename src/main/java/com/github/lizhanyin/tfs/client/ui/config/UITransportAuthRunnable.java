// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.github.lizhanyin.tfs.client.ui.config;

import com.intellij.openapi.ui.DialogWrapper;
import com.microsoft.tfs.core.httpclient.Credentials;

/**
 * Abstract base class for authentication runnables that display credential
 * dialogs on the EDT. Converted from Eclipse SWT to IntelliJ IDEA Swing platform.
 *
 * <p>In the Eclipse version, this class managed an SWT event loop while waiting
 * for the user to complete a non-blocking dialog. In the IDEA version, the dialog
 * is modal and blocks the EDT until closed, simplifying the threading model.</p>
 */
public abstract class UITransportAuthRunnable implements Runnable {
    private final Object lock = new Object();

    private Credentials credentials;
    private boolean complete = false;

    public Credentials getCredentials() {
        synchronized (lock) {
            return credentials;
        }
    }

    @Override
    public final void run() {
        synchronized (lock) {
            if (complete) {
                return;
            }
        }

        final CredentialsCompleteDialog credentialsDialog = getCredentialsDialog();

        /*
         * Subclasses may return null, indicating that they cannot handle the
         * given credentials (eg, federated credentials but we can't open a web
         * browser.)
         */
        if (credentialsDialog == null) {
            setComplete(null);
            return;
        }

        /*
         * Show the dialog on the EDT. Since this runs on the EDT (via
         * UIHelpers.runOnUIThread), the modal dialog blocks until closed.
         * No separate event loop is needed (unlike SWT).
         */
        final int exitCode = credentialsDialog.show();

        final Credentials creds = (exitCode == DialogWrapper.OK_EXIT_CODE)
            ? credentialsDialog.getCredentials() : null;

        setComplete(creds);
    }

    /**
     * Subclasses must return a {@link CredentialsCompleteDialog} to display,
     * or <code>null</code> if they cannot handle the authentication request.
     */
    protected abstract CredentialsCompleteDialog getCredentialsDialog();

    private void setComplete(final Credentials credentials) {
        synchronized (lock) {
            this.credentials = credentials;
            this.complete = true;

            lock.notifyAll();
        }
    }

    public final boolean isComplete() {
        synchronized (lock) {
            return complete;
        }
    }

    public final void join() throws InterruptedException {
        while (true) {
            synchronized (lock) {
                if (complete) {
                    return;
                }

                lock.wait();
            }
        }
    }
}
