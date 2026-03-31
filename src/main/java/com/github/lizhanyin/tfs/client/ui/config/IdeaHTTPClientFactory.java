// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.github.lizhanyin.tfs.client.ui.config;

import java.net.URI;
import java.net.URISyntaxException;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.ide.util.PropertiesComponent;

import com.microsoft.tfs.core.config.ConnectionInstanceData;
import com.microsoft.tfs.core.config.httpclient.DefaultHTTPClientFactory;
import com.microsoft.tfs.core.httpclient.Credentials;
import com.microsoft.tfs.core.httpclient.HostConfiguration;
import com.microsoft.tfs.core.httpclient.HttpClient;
import com.microsoft.tfs.core.httpclient.HttpState;
import com.microsoft.tfs.core.httpclient.UsernamePasswordCredentials;
import com.microsoft.tfs.core.httpclient.auth.AuthScope;

/**
 * An {@link DefaultHTTPClientFactory} that configures HTTP client proxy settings
 * from IntelliJ IDEA's {@link PropertiesComponent}. Converted from Eclipse
 * {@code LegacyHTTPClientFactory} which used Eclipse preferences.
 *
 * <p>HTTP proxy property keys:</p>
 * <ul>
 *   <li>{@code http.proxy.enabled} - whether HTTP proxy is enabled</li>
 *   <li>{@code http.proxy.url} - HTTP proxy URL (host:port)</li>
 *   <li>{@code http.proxy.username} - proxy username</li>
 *   <li>{@code http.proxy.password} - proxy password</li>
 *   <li>{@code http.proxy.accept.untrusted.certificates} - accept untrusted SSL certs</li>
 * </ul>
 */
public class IdeaHTTPClientFactory extends DefaultHTTPClientFactory {
    private static final Logger log = Logger.getInstance(IdeaHTTPClientFactory.class);

    /** Property keys for HTTP proxy settings stored in PropertiesComponent */
    public static final String HTTP_PROXY_ENABLED = "http.proxy.enabled"; //$NON-NLS-1$
    public static final String HTTP_PROXY_URL = "http.proxy.url"; //$NON-NLS-1$
    public static final String HTTP_PROXY_USERNAME = "http.proxy.username"; //$NON-NLS-1$
    public static final String HTTP_PROXY_PASSWORD = "http.proxy.password"; //$NON-NLS-1$
    public static final String HTTP_PROXY_PREFIX = "http.proxy."; //$NON-NLS-1$
    public static final String ACCEPT_UNTRUSTED_CERTIFICATES = "http.proxy.accept.untrusted.certificates"; //$NON-NLS-1$

    public IdeaHTTPClientFactory(final ConnectionInstanceData connectionInstanceData) {
        super(connectionInstanceData);
    }

    @Override
    public void configureClientProxy(
        final HttpClient httpClient,
        final HostConfiguration hostConfiguration,
        final HttpState httpState,
        final ConnectionInstanceData connectionInstanceData) {
        configureProxy(httpClient);
    }

    @Override
    protected boolean shouldAcceptUntrustedCertificates(final ConnectionInstanceData connectionInstanceData) {
        final PropertiesComponent properties = PropertiesComponent.getInstance();

        if (properties.getBoolean(ACCEPT_UNTRUSTED_CERTIFICATES, false)) {
            return true;
        }

        // Let the base class test for environment variables, sysprops, etc.
        return super.shouldAcceptUntrustedCertificates(connectionInstanceData);
    }

    private void configureProxy(final HttpClient httpClient) {
        final PropertiesComponent properties = PropertiesComponent.getInstance();

        final HostConfiguration hostConfiguration = httpClient.getHostConfiguration();

        final boolean useHttpProxy = properties.getBoolean(HTTP_PROXY_ENABLED, false);

        if (!useHttpProxy) {
            if (log.isDebugEnabled()) {
                log.debug("HTTP proxy preference off: setting client to have no proxy"); //$NON-NLS-1$
            }

            hostConfiguration.setProxyHost(null);
            return;
        }

        String httpProxyUrl = properties.getValue(HTTP_PROXY_URL);

        if (httpProxyUrl != null) {
            httpProxyUrl = httpProxyUrl.trim();
            if (httpProxyUrl.isEmpty()) {
                httpProxyUrl = null;
            }
        }

        if (httpProxyUrl == null) {
            if (log.isDebugEnabled()) {
                log.debug("HTTP proxy URL preference empty: setting client to have no proxy"); //$NON-NLS-1$
            }

            hostConfiguration.setProxyHost(null);
            return;
        }

        URI uri;
        try {
            uri = new URI(httpProxyUrl);
        } catch (final URISyntaxException e) {
            log.warn("Illegal proxy URL: " + httpProxyUrl + ", setting client to have no proxy", e); //$NON-NLS-1$ //$NON-NLS-2$

            hostConfiguration.setProxyHost(null);
            return;
        }

        if (log.isDebugEnabled()) {
            log.debug("Setting client to have proxy: host=" + uri.getHost() + ", port=" + uri.getPort()); //$NON-NLS-1$ //$NON-NLS-2$
        }

        hostConfiguration.setProxy(uri.getHost(), uri.getPort());

        final HttpState httpState = httpClient.getState();

        String httpProxyUsername = properties.getValue(HTTP_PROXY_USERNAME);
        String httpProxyPassword = properties.getValue(HTTP_PROXY_PASSWORD);

        if (httpProxyUsername != null) {
            httpProxyUsername = httpProxyUsername.trim();
            if (httpProxyUsername.isEmpty()) {
                httpProxyUsername = null;
            }
        }

        if (httpProxyPassword != null) {
            httpProxyPassword = httpProxyPassword.trim();
        }

        if (httpProxyUsername == null) {
            if (log.isDebugEnabled()) {
                log.debug("Setting client to have no proxy credentials"); //$NON-NLS-1$
            }

            httpState.clearProxyCredentials();
            return;
        }

        if (log.isDebugEnabled()) {
            log.debug("Setting client to have proxy credentials: " + httpProxyUsername); //$NON-NLS-1$
        }

        final Credentials proxyCredentials = new UsernamePasswordCredentials(httpProxyUsername, httpProxyPassword);
        httpState.setProxyCredentials(AuthScope.ANY, proxyCredentials);
    }
}
