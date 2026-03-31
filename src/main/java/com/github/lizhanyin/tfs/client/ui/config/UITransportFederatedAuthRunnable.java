// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.github.lizhanyin.tfs.client.ui.config;

import java.net.URI;

import com.intellij.openapi.diagnostic.Logger;

import com.microsoft.tfs.core.ws.runtime.exceptions.FederatedAuthException;

/**
 * Handles federated authentication by opening a browser for ADFS/STS login.
 *
 * <p>For TFS 2015 on-premises servers, federated authentication is typically
 * not used (NTLM/Basic auth is the norm). This implementation always returns
 * {@code isAvailable() == false}, causing the fallback to username/password
 * authentication.</p>
 *
 * <p>This can be extended in the future to support federated auth via
 * JBCefBrowser or similar IDEA browser components.</p>
 */
public class UITransportFederatedAuthRunnable extends UITransportAuthRunnable {
    private static final Logger log = Logger.getInstance(UITransportFederatedAuthRunnable.class);

    private final URI serverURI;
    private final FederatedAuthException exception;

    public UITransportFederatedAuthRunnable(final URI serverURI, final FederatedAuthException exception) {
        this.serverURI = serverURI;
        this.exception = exception;
    }

    /**
     * Checks if federated authentication is available for the current platform.
     * Currently returns {@code false} as IDEA platform does not have an embedded
     * browser session management like Eclipse.
     *
     * @return {@code true} if federated auth can be handled
     */
    public boolean isAvailable() {
        log.debug("Federated auth not available for IDEA platform, falling back to username/password"); //$NON-NLS-1$
        return false;
    }

    @Override
    protected CredentialsCompleteDialog getCredentialsDialog() {
        // Not available - callers should check isAvailable() first
        return null;
    }
}
