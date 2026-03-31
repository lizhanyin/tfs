// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.github.lizhanyin.tfs.client.ui.config;

import com.intellij.openapi.ui.DialogWrapper;
import com.microsoft.tfs.core.httpclient.Credentials;

/**
 * Abstract base class for credential dialogs in IntelliJ IDEA platform.
 * Replaces the Eclipse JFace CredentialsCompleteDialog with a Swing/IDEA equivalent.
 *
 * Subclasses implement {@link #show()} to display a dialog on the EDT,
 * which blocks until the user closes it.
 */
public abstract class CredentialsCompleteDialog {
    private Credentials credentials;
    private int exitCode = DialogWrapper.CANCEL_EXIT_CODE;

    /**
     * Shows the credential dialog. This method blocks (on the EDT) until the
     * dialog is closed by the user.
     *
     * @return the exit code ({@link DialogWrapper#OK_EXIT_CODE} or
     *         {@link DialogWrapper#CANCEL_EXIT_CODE})
     */
    public abstract int show();

    /**
     * Returns the credentials entered by the user, or <code>null</code> if
     * the dialog was cancelled. Only valid after {@link #show()} returns.
     */
    public Credentials getCredentials() {
        return credentials;
    }

    /**
     * Returns the exit code from the dialog.
     */
    public int getReturnCode() {
        return exitCode;
    }

    protected void setCredentials(final Credentials credentials) {
        this.credentials = credentials;
    }

    protected void setExitCode(final int exitCode) {
        this.exitCode = exitCode;
    }
}
