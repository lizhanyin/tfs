// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.github.lizhanyin.tfs.client.ui.config;

import java.util.Locale;
import java.util.TimeZone;

import com.intellij.openapi.diagnostic.Logger;

import com.microsoft.tfs.core.config.ConnectionInstanceData;
import com.microsoft.tfs.core.config.httpclient.ConfigurableHTTPClientFactory;
import com.microsoft.tfs.core.config.httpclient.HTTPClientFactory;
import com.microsoft.tfs.core.config.tfproxy.TFProxyServerSettingsFactory;
import com.microsoft.tfs.core.config.webservice.DefaultWebServiceFactory;
import com.microsoft.tfs.core.config.webservice.WebServiceFactory;

public class UIClientConnectionAdvisor extends CommonClientConnectionAdvisor {
    private static final Logger log = Logger.getInstance(UIClientConnectionAdvisor.class);

    /**
     * Creates a {@link UIClientConnectionAdvisor} that uses the current default
     * {@link Locale} and {@link TimeZone} for all
     * {@link ConnectionInstanceData}s.
     */
    public UIClientConnectionAdvisor() {
        super(Locale.getDefault(), TimeZone.getDefault());
    }

    @Override
    public HTTPClientFactory getHTTPClientFactory(final ConnectionInstanceData instanceData) {
        log.debug("Returning IdeaHTTPClientFactory"); //$NON-NLS-1$

        return new IdeaHTTPClientFactory(instanceData);
    }

    @Override
    public TFProxyServerSettingsFactory getTFProxyServerSettingsFactory(final ConnectionInstanceData instanceData) {
        return new IdeaTFProxyServerSettingsFactory(instanceData);
    }

    @Override
    public WebServiceFactory getWebServiceFactory(final ConnectionInstanceData instanceData) {
        /*
         * Handle federated authentication with a GUI.
         */
        return new DefaultWebServiceFactory(
            getLocale(instanceData),
            new UITransportRequestHandler(
                instanceData,
                (ConfigurableHTTPClientFactory) getHTTPClientFactory(instanceData)));
    }
}
