// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.github.lizhanyin.tfs.client.ui.config;

import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import com.intellij.openapi.diagnostic.Logger;

import com.github.lizhanyin.tfs.client.credentials.IdeaCredentialsManagerFactory;
import com.github.lizhanyin.tfs.client.framework.helper.UIHelpers;
import com.github.lizhanyin.tfs.client.ui.helpers.Browser;
import com.microsoft.tfs.core.config.ConnectionInstanceData;
import com.microsoft.tfs.core.config.EnvironmentVariables;
import com.microsoft.tfs.core.config.auth.DefaultTransportRequestHandler;
import com.microsoft.tfs.core.config.httpclient.ConfigurableHTTPClientFactory;
import com.microsoft.tfs.core.config.persistence.DefaultPersistenceStoreProvider;
import com.microsoft.tfs.core.credentials.CachedCredentials;
import com.microsoft.tfs.core.credentials.CredentialsManager;
import com.microsoft.tfs.core.httpclient.Cookie;
import com.microsoft.tfs.core.httpclient.CookieCredentials;
import com.microsoft.tfs.core.httpclient.Credentials;
import com.microsoft.tfs.core.httpclient.DefaultNTCredentials;
import com.microsoft.tfs.core.httpclient.Header;
import com.microsoft.tfs.core.httpclient.HttpClient;
import com.microsoft.tfs.core.httpclient.URI;
import com.microsoft.tfs.core.httpclient.URIException;
import com.microsoft.tfs.core.httpclient.UsernamePasswordCredentials;
import com.microsoft.tfs.core.httpclient.cookie.CookiePolicy;
import com.microsoft.tfs.core.httpclient.cookie.CookieSpec;
import com.microsoft.tfs.core.ws.runtime.client.SOAPRequest;
import com.microsoft.tfs.core.ws.runtime.client.SOAPService;
import com.microsoft.tfs.core.ws.runtime.exceptions.FederatedAuthException;
import com.microsoft.tfs.core.ws.runtime.exceptions.FederatedAuthFailedException;
import com.microsoft.tfs.core.ws.runtime.exceptions.UnauthorizedException;
import com.microsoft.tfs.util.StringUtil;

/**
 * A {@link DefaultTransportRequestHandler} for the UI client, capable of handling
 * federated authentication exceptions by prompting the user to reauthenticate.
 * Converted from Eclipse SWT to IntelliJ IDEA platform.
 *
 * @threadsafety unknown
 */
public class UITransportRequestHandler extends DefaultTransportRequestHandler {
    private static final Logger log = Logger.getInstance(UITransportRequestHandler.class);

    /*
     * Only one authentication runnable (per mechanism) should run at a time.
     * Serialize them.
     */
    private final Object runnableLock = new Object();
    private UITransportAuthRunnable dialogRunnable = null;

    /**
     * Creates a {@link UITransportRequestHandler} that updates the given profile
     * with new auth data using the given {@link ConfigurableHTTPClientFactory}'s
     * credential update methods.
     *
     * @param connectionInstanceData the connection instance data to update
     * @param clientFactory the factory to use to apply credentials
     */
    public UITransportRequestHandler(
        final ConnectionInstanceData connectionInstanceData,
        final ConfigurableHTTPClientFactory clientFactory) {
        super(connectionInstanceData, clientFactory);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Status prepareRequest(final SOAPService service, final SOAPRequest request, final AtomicBoolean cancel) {
        final ConnectionInstanceData connectionInstanceData = getConnectionInstanceData();
        final Credentials credentials = connectionInstanceData.getCredentials();

        log.debug("Preparing request with credentials: " + (credentials == null ? "null" //$NON-NLS-1$ //$NON-NLS-2$
            : credentials.getClass().getName()));

        if (credentials == null || !(credentials instanceof UsernamePasswordCredentials)) {
            return Status.CONTINUE;
        }

        final UsernamePasswordCredentials oldCredentials = (UsernamePasswordCredentials) credentials;

        /*
         * If the user has provided UsernamePasswordCredentials with an empty
         * password, this cannot be correct, so prompt them.
         */
        if (StringUtil.isNullOrEmpty(oldCredentials.getPassword())) {
            log.debug("UsernamePasswordCredentials with an empty password detected"); //$NON-NLS-1$
            final Credentials newCredentials = getCredentials(
                new UITransportUsernamePasswordAuthRunnable(
                    connectionInstanceData.getServerURI(),
                    connectionInstanceData.getCredentials()));

            if (newCredentials == null) {
                log.debug("New UsernamePasswordCredentials not provided. Cancelling the request"); //$NON-NLS-1$
                cancel.set(true);
                return Status.CONTINUE;
            }

            log.debug("New UsernamePasswordCredentials provided"); //$NON-NLS-1$

            // Apply the credentials data to the existing client.
            connectionInstanceData.setCredentials(newCredentials);

            getClientFactory().configureClientCredentials(
                service.getClient(),
                service.getClient().getState(),
                connectionInstanceData);
        }

        return Status.CONTINUE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Status handleSuccess(final SOAPService service, final SOAPRequest request) {
        final Header[] cookieHeaders = request.getPostMethod().getResponseHeaders("Set-Cookie"); //$NON-NLS-1$
        final ArrayList<Cookie> fedAuthCookies = new ArrayList<Cookie>();

        if (cookieHeaders.length > 0) {
            log.debug("Request succeeded - Set-Cookie headers found in the response"); //$NON-NLS-1$
        }

        final URI uri;
        final String domain;
        try {
            uri = request.getPostMethod().getURI();
            domain = uri.getHost();
        } catch (final URIException e) {
            log.error("Incorrect URI", e); //$NON-NLS-1$
            return Status.CONTINUE;
        }

        int port = uri.getPort();
        if (port < 0) {
            if ("https".equalsIgnoreCase(uri.getScheme())) //$NON-NLS-1$
            {
                port = 443;
            } else {
                port = 80;
            }
        }

        /* Parse cookies according to RFC2109 */
        final CookieSpec cookieParser = CookiePolicy.getCookieSpec(CookiePolicy.RFC_2109);

        for (final Header cookieHeader : cookieHeaders) {
            log.debug(cookieHeader.getName() + ": " + cookieHeader.getValue()); //$NON-NLS-1$

            try {
                final Cookie[] cookies = cookieParser.parse(domain, port, "/", true, cookieHeader); //$NON-NLS-1$

                for (final Cookie cookie : cookies) {
                    if (cookie.getName().startsWith("FedAuth")) //$NON-NLS-1$
                    {
                        fedAuthCookies.add(cookie);
                    }
                }
            } catch (final Exception e) {
                log.warn(MessageFormat.format("Could not parse authentication cookie {0}", cookieHeader.getValue()), e); //$NON-NLS-1$
            }
        }

        if (fedAuthCookies.size() > 0) {
            final ConnectionInstanceData connectionInstanceData = getConnectionInstanceData();

            final CookieCredentials newCredentials =
                new CookieCredentials(fedAuthCookies.toArray(new Cookie[fedAuthCookies.size()]));

            if (!(connectionInstanceData.getCredentials() instanceof CookieCredentials)
                || !newCredentials.equals(connectionInstanceData.getCredentials())) {
                log.debug("New Cookie Credentials created"); //$NON-NLS-1$

                // Apply the credentials data to the existing client.
                log.debug("Apply the new Cookie Credentials to the existing client."); //$NON-NLS-1$
                connectionInstanceData.setCredentials(newCredentials);

                log.debug("Save the new Cookie Credentials to the existing Client Factory for future clients."); //$NON-NLS-1$
                getClientFactory().configureClientCredentials(
                    service.getClient(),
                    service.getClient().getState(),
                    connectionInstanceData);

                final CredentialsManager credentialsManager =
                    IdeaCredentialsManagerFactory.getCredentialsManager(DefaultPersistenceStoreProvider.INSTANCE);
                try {
                    log.debug("Save the new Cookie Credentials in the IDEA secure storage for future sessions."); //$NON-NLS-1$
                    credentialsManager.setCredentials(new CachedCredentials(uri.toJavaNetUri(), newCredentials));
                } catch (final URISyntaxException e) {
                    log.error("Incorrect URI", e); //$NON-NLS-1$
                }
            }
        }

        return Status.CONTINUE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Status handleException(
        final SOAPService service,
        final SOAPRequest request,
        final Exception exception,
        final AtomicBoolean cancel) {
        final ConnectionInstanceData connectionInstanceData = getConnectionInstanceData();

        log.info("Authentication requested: " + exception.getMessage()); //$NON-NLS-1$

        /*
         * Super method handles FederatedAuthException with service credentials
         * if credentials were specified. Try that first.
         */
        if (super.handleException(service, request, exception, cancel) == Status.COMPLETE) {
            log.debug("DefaultTransportAuthHandler handled auth exception for us"); //$NON-NLS-1$
            return Status.COMPLETE;
        }

        log.debug("DefaultTransportAuthHandler did not handle auth exception"); //$NON-NLS-1$

        final UITransportAuthRunnable dialogRunnable;

        /*
         * For a federated authentication exception, always raise the login to
         * ACS or OAuth credentials dialog.
         */
        if (exception instanceof FederatedAuthException) {
            log.debug("FederatedAuthException has been raised."); //$NON-NLS-1$

            cleanupSavedCredentials(service.getClient());

            if (EnvironmentVariables.getBoolean(EnvironmentVariables.USE_OAUTH_LIBRARY, true)) {
                dialogRunnable = new UITransportOAuthRunnable(connectionInstanceData.getServerURI());
            } else {
                dialogRunnable = new UITransportFederatedFallbackAuthRunnable(
                    connectionInstanceData.getServerURI(),
                    connectionInstanceData.getCredentials(),
                    (FederatedAuthException) exception);
            }
        }
        /*
         * For failed username/password or PAT credentials, raise the UI dialog
         * if the service recommends prompting.
         */
        else if (exception instanceof UnauthorizedException && service.isPromptForCredentials()) {
            log.debug("UnauthorizedException has been raised."); //$NON-NLS-1$

            final Credentials usedCredentials = connectionInstanceData.getCredentials();
            final java.net.URI serverUrl = connectionInstanceData.getServerURI();
            final Header[] authHeaders = request.getPostMethod().getResponseHeaders("WWW-Authenticate"); //$NON-NLS-1$
            boolean isHosted = true;
            for (final Header header : authHeaders) {
                if (header.getValue().equals("NTLM")) { //$NON-NLS-1$
                    isHosted = false;
                    break;
                }
                if (header.getValue().equals("Negotiate")) { //$NON-NLS-1$
                    isHosted = false;
                    break;
                }
                if (header.getValue().equals("Federated")) { //$NON-NLS-1$
                    break;
                }
            }

            if (EnvironmentVariables.getBoolean(EnvironmentVariables.USE_OAUTH_LIBRARY, true)
                && usedCredentials != null
                && isHosted
                && new CachedCredentials(serverUrl, usedCredentials).isPatCredentials()) {
                // PAT token is probably expired. Remove it from IDEA
                // secure storage and retry.
                final CredentialsManager credentialsManager = IdeaCredentialsManagerFactory.getCredentialsManager();
                credentialsManager.removeCredentials(serverUrl);
                dialogRunnable = new UITransportOAuthRunnable(serverUrl);
            } else {
                dialogRunnable = new UITransportUsernamePasswordAuthRunnable(
                    serverUrl,
                    usedCredentials,
                    (UnauthorizedException) exception);
            }
        }
        /*
         * The Cookie Credentials used are incorrect. They are either corrupted
         * in storage or expired. Cleanup the storage and retry from scratch.
         */
        else if (exception instanceof FederatedAuthFailedException) {
            cleanupSavedCredentials(service.getClient());
            return Status.CONTINUE;
        } else {
            log.debug("Unknown authentication type or shouldn't prompt for this service."); //$NON-NLS-1$
            return Status.CONTINUE;
        }

        log.debug("Prompt for credentials"); //$NON-NLS-1$
        final Credentials credentials = getCredentials(dialogRunnable);

        log.debug(
            "The dialog returned credentials: " + (credentials == null ? "null" : credentials.getClass().getName())); //$NON-NLS-1$ //$NON-NLS-2$

        if (credentials == null) {
            log.info("Credentials dialog has been cancelled by the user."); //$NON-NLS-1$
            cancel.set(true);
            return Status.CONTINUE;
        }

        // Apply the credentials data to the existing client.
        log.debug("Apply the new credentials to the existing client."); //$NON-NLS-1$
        connectionInstanceData.setCredentials(credentials);

        log.debug("Save the new credentials to the existing Client Factory for future clients in this session."); //$NON-NLS-1$
        getClientFactory().configureClientCredentials(
            service.getClient(),
            service.getClient().getState(),
            connectionInstanceData);

        return Status.COMPLETE;
    }

    private void cleanupSavedCredentials(final HttpClient client) {
        log.debug("If any credentials were used they failed. Clean up saved credentials for the host"); //$NON-NLS-1$

        final ConnectionInstanceData connectionInstanceData = getConnectionInstanceData();

        client.getState().clearCookies();
        client.getState().clearCredentials();

        final CredentialsManager credentialsManager =
            IdeaCredentialsManagerFactory.getCredentialsManager(DefaultPersistenceStoreProvider.INSTANCE);
        credentialsManager.removeCredentials(connectionInstanceData.getServerURI());

        connectionInstanceData.setCredentials(new DefaultNTCredentials());

        getClientFactory().configureClientCredentials(client, client.getState(), connectionInstanceData);

        Browser.clearSessions();
    }

    /**
     * If there is no {@link Runnable} executing to get the users credentials,
     * then the current runnable is executed.
     *
     * @param dialogRunnable the runnable to execute to obtain credentials
     * @return the credentials, or <code>null</code> if the user cancelled
     */
    private Credentials getCredentials(final UITransportAuthRunnable dialogRunnable) {
        boolean ownsRunnable = false;
        UITransportAuthRunnable runnable;

        try {
            /*
             * If there is a runnable currently executing, we'll simply use it.
             * If there is not, we'll set up the runnable we were given.
             */
            synchronized (runnableLock) {
                if (this.dialogRunnable == null) {
                    this.dialogRunnable = dialogRunnable;
                    ownsRunnable = true;
                }

                runnable = this.dialogRunnable;
            }

            /*
             * If we need to start our runnable, do so on the UI thread.
             * In IDEA, UIHelpers.runOnUIThread(false, ...) uses invokeAndWait,
             * which blocks the calling thread until the EDT finishes.
             * The runnable shows a modal dialog which blocks the EDT until
             * closed, so the call is fully synchronous.
             */
            if (ownsRunnable) {
                UIHelpers.runOnUIThread(false, runnable);
            } else {
                /*
                 * Otherwise, wait for the currently executing runnable to
                 * complete.
                 */
                while (!runnable.isComplete()) {
                    /*
                     * If we're the EDT, we should not be here (invokeAndWait
                     * would have thrown). Simply sleep and wait.
                     */
                    try {
                        Thread.sleep(100);
                    } catch (final InterruptedException e) {
                        log.warn("Interrupted waiting for credentials dialog"); //$NON-NLS-1$
                    }
                }
            }

            return runnable.getCredentials();
        } finally {
            /*
             * Clear the auth runnable so that subsequent authentication
             * attempts can succeed.
             */
            if (ownsRunnable) {
                synchronized (runnableLock) {
                    this.dialogRunnable = null;
                }
            }
        }
    }
}
