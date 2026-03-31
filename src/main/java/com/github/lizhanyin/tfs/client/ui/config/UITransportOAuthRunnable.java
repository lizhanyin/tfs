// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.github.lizhanyin.tfs.client.ui.config;

import java.awt.Window;
import java.net.URI;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.ui.DialogWrapper;

import com.github.lizhanyin.tfs.client.credentials.IdeaCredentialsManagerFactory;
import com.github.lizhanyin.tfs.client.framework.helper.ShellUtils;
import com.github.lizhanyin.tfs.wizard.dialog.CredentialsDialog;
import com.microsoft.tfs.core.config.persistence.DefaultPersistenceStoreProvider;
import com.microsoft.tfs.core.credentials.CachedCredentials;
import com.microsoft.tfs.core.credentials.CredentialsManager;
import com.microsoft.tfs.core.httpclient.Credentials;
import com.microsoft.tfs.core.httpclient.UsernamePasswordCredentials;
import com.microsoft.tfs.util.Check;

/**
 * A {@link UITransportAuthRunnable} that handles OAuth2 authentication.
 *
 * <p>For TFS 2015 on-premises, OAuth device flow is typically not available
 * (it was introduced in TFS 2017/Azure DevOps). This implementation shows
 * a username/password dialog as a fallback, with a note suggesting PAT usage.</p>
 *
 * <p>The Eclipse version used {@code CredentialsHelper.getOAuthCredentials()} and
 * an embedded browser for device flow. In IDEA, this can be extended in the future
 * to use JBCefBrowser for proper OAuth support.</p>
 */
public class UITransportOAuthRunnable extends UITransportAuthRunnable {
    private static final Logger log = Logger.getInstance(UITransportOAuthRunnable.class);

    private final URI serverURI;

    public UITransportOAuthRunnable(final URI serverURI) {
        Check.notNull(serverURI, "serverURI"); //$NON-NLS-1$

        this.serverURI = serverURI;
    }

    @Override
    protected CredentialsCompleteDialog getCredentialsDialog() {
        log.debug("OAuth device flow not available, showing credential dialog as fallback"); //$NON-NLS-1$

        final Window parent = ShellUtils.getBestParent(ShellUtils.getActiveProjectWindow());

        final CredentialsManager credentialsManager =
            IdeaCredentialsManagerFactory.getCredentialsManager(DefaultPersistenceStoreProvider.INSTANCE);

        return new OAuthFallbackCredentialsDialog(parent, serverURI, credentialsManager);
    }

    /**
     * Fallback credential dialog for OAuth scenarios.
     * Shows a standard username/password dialog with a hint about PAT.
     */
    private class OAuthFallbackCredentialsDialog extends CredentialsCompleteDialog {
        private final Window parent;
        private final URI serverURI;
        private final CredentialsManager credentialsManager;

        OAuthFallbackCredentialsDialog(
            final Window parent,
            final URI serverURI,
            final CredentialsManager credentialsManager) {
            this.parent = parent;
            this.serverURI = serverURI;
            this.credentialsManager = credentialsManager;
        }

        @Override
        public int show() {
            final CredentialsDialog dialog = new CredentialsDialog(parent, serverURI.toString());

            // Default to PAT type for OAuth scenarios
            dialog.setCredentials(CredentialsDialog.CredentialType.PERSONAL_ACCESS_TOKEN, null, null);

            dialog.show();

            final int exitCode = dialog.getExitCode();

            if (exitCode == DialogWrapper.OK_EXIT_CODE) {
                final String username = dialog.getUsername();
                final String password = dialog.getPassword();

                final Credentials tfsCredentials;
                if (dialog.getCredentialType() == CredentialsDialog.CredentialType.PERSONAL_ACCESS_TOKEN) {
                    tfsCredentials = new UsernamePasswordCredentials("", password); //$NON-NLS-1$
                } else {
                    tfsCredentials = new UsernamePasswordCredentials(username, password);
                }

                setCredentials(tfsCredentials);
                setExitCode(DialogWrapper.OK_EXIT_CODE);

                // Save credentials
                if (credentialsManager.canWrite()) {
                    try {
                        credentialsManager.setCredentials(
                            new CachedCredentials(serverURI, tfsCredentials));
                        log.debug("OAuth fallback credentials saved"); //$NON-NLS-1$
                    } catch (final Exception e) {
                        log.warn("Failed to save credentials", e); //$NON-NLS-1$
                    }
                }
            } else {
                setExitCode(DialogWrapper.CANCEL_EXIT_CODE);
            }

            return getReturnCode();
        }
    }
}
