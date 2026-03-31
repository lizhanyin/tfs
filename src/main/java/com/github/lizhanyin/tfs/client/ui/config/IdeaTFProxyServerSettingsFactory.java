// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.github.lizhanyin.tfs.client.ui.config;

import java.util.HashMap;
import java.util.Map;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.ide.util.PropertiesComponent;

import com.microsoft.tfs.core.TFProxyServerSettings;
import com.microsoft.tfs.core.config.ConnectionInstanceData;
import com.microsoft.tfs.core.config.tfproxy.DefaultTFProxyServerSettings;
import com.microsoft.tfs.core.config.tfproxy.DefaultTFProxyServerSettingsFactory;

/**
 * Provides {@link TFProxyServerSettings} objects configured from IntelliJ IDEA
 * properties (via {@link PropertiesComponent}). Converted from Eclipse
 * {@code EclipseTFProxyServerSettingsFactory} which used Eclipse preferences.
 *
 * <p>Proxy settings keys:</p>
 * <ul>
 *   <li>{@code tfs.proxy.enabled} - whether TFS proxy is enabled</li>
 *   <li>{@code tfs.proxy.url} - TFS proxy URL</li>
 * </ul>
 */
public class IdeaTFProxyServerSettingsFactory extends DefaultTFProxyServerSettingsFactory {
    private static final Logger log = Logger.getInstance(IdeaTFProxyServerSettingsFactory.class);

    /** Property keys for TFS proxy settings stored in PropertiesComponent */
    public static final String TFS_PROXY_ENABLED = "tfs.proxy.enabled"; //$NON-NLS-1$
    public static final String TFS_PROXY_URL = "tfs.proxy.url"; //$NON-NLS-1$
    public static final String TFS_PROXY_PREFIX = "tfs.proxy."; //$NON-NLS-1$

    private final Map<DefaultTFProxyServerSettings, Runnable> listeners = new HashMap<>();

    public IdeaTFProxyServerSettingsFactory(final ConnectionInstanceData connectionInstanceData) {
        super(connectionInstanceData);
    }

    @Override
    public void dispose(final TFProxyServerSettings proxyServerSettings) {
        // In IDEA, we don't have property change listeners on PropertiesComponent
        // like Eclipse has on Preferences. The listener cleanup is simplified.
        //listeners.remove(proxyServerSettings);
        super.dispose(proxyServerSettings);
    }

    /**
     * {@inheritDoc}
     *
     * <p>The object returned is configured with values from IDEA's
     * {@link PropertiesComponent}.</p>
     */
    @Override
    public TFProxyServerSettings newProxyServerSettings() {
        final DefaultTFProxyServerSettings proxyServerSettings = new DefaultTFProxyServerSettings(null);

        configureProxySettings(proxyServerSettings);

        return proxyServerSettings;
    }

    /**
     * Configures the proxy settings from IDEA properties.
     *
     * @param proxyServerSettings the settings to configure (must not be <code>null</code>)
     */
    private void configureProxySettings(final DefaultTFProxyServerSettings proxyServerSettings) {
        final PropertiesComponent properties = PropertiesComponent.getInstance();

        final boolean useTfsProxy = properties.getBoolean(TFS_PROXY_ENABLED, false);

        if (!useTfsProxy) {
            if (log.isDebugEnabled()) {
                log.debug("TFS proxy preference off: using superclass configuration"); //$NON-NLS-1$
            }

            configureFromSuper(proxyServerSettings);
            return;
        }

        String tfsProxyUrl = properties.getValue(TFS_PROXY_URL);

        if (tfsProxyUrl != null) {
            tfsProxyUrl = tfsProxyUrl.trim();
            if (tfsProxyUrl.isEmpty()) {
                tfsProxyUrl = null;
            }
        }

        if (tfsProxyUrl == null) {
            if (log.isDebugEnabled()) {
                log.debug("TFS proxy URL preference empty: using superclass configuration"); //$NON-NLS-1$
            }

            configureFromSuper(proxyServerSettings);
            return;
        }

        if (log.isDebugEnabled()) {
            log.debug("Setting TFProxyServerSettings to have URL: " + tfsProxyUrl); //$NON-NLS-1$
        }

        proxyServerSettings.setURL(tfsProxyUrl);
    }

    private void configureFromSuper(final DefaultTFProxyServerSettings settings) {
        final TFProxyServerSettings superSettings = super.newProxyServerSettings();

        if (superSettings != null && superSettings.getURL() != null) {
            settings.setURL(superSettings.getURL());
        } else {
            settings.setURL(null);
        }
    }
}
