// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.github.lizhanyin.tfs.client.ui.config;

import java.net.URI;

import com.microsoft.tfs.core.httpclient.Credentials;
import com.microsoft.tfs.core.ws.runtime.exceptions.FederatedAuthException;
import com.microsoft.tfs.core.ws.runtime.exceptions.UnauthorizedException;
import com.microsoft.tfs.util.Check;

/**
 * A {@link UITransportAuthRunnable} that first attempts federated authentication,
 * falling back to username/password if federated auth is not available.
 * Converted from Eclipse to IntelliJ IDEA.
 */
public class UITransportFederatedFallbackAuthRunnable extends UITransportAuthRunnable {
    private final URI serverURI;
    private final Credentials credentials;
    private final FederatedAuthException exception;

    public UITransportFederatedFallbackAuthRunnable(
        final URI serverURI,
        final Credentials credentials,
        final FederatedAuthException exception) {
        Check.notNull(serverURI, "serverURI"); //$NON-NLS-1$
        Check.notNull(credentials, "credentials"); //$NON-NLS-1$
        Check.notNull(exception, "exception"); //$NON-NLS-1$

        this.serverURI = serverURI;
        this.credentials = credentials;
        this.exception = exception;
    }

    @Override
    protected CredentialsCompleteDialog getCredentialsDialog() {
        // Try federated auth first
        final UITransportFederatedAuthRunnable subRunnable =
            new UITransportFederatedAuthRunnable(serverURI, exception);

        if (subRunnable.isAvailable()) {
            return subRunnable.getCredentialsDialog();
        }

        // Fall back to username/password authentication
        final UnauthorizedException unauthorizedException =
            new UnauthorizedException(exception.getServerURI(), credentials);

        return new UITransportUsernamePasswordAuthRunnable(
            serverURI,
            credentials,
            unauthorizedException).getCredentialsDialog();
    }
}
