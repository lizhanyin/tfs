// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.github.lizhanyin.tfs.client.ui.credentials.internal;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import com.microsoft.tfs.core.util.URIUtils;
import org.jetbrains.annotations.NotNull;

import com.intellij.credentialStore.CredentialAttributes;
import com.intellij.credentialStore.CredentialAttributesKt;
import com.intellij.ide.passwordSafe.PasswordSafe;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.ide.util.PropertiesComponent;
import com.github.lizhanyin.tfs.client.ui.Messages;
import com.microsoft.tfs.core.config.persistence.PersistenceStoreProvider;
import com.microsoft.tfs.core.credentials.CachedCredentials;
import com.microsoft.tfs.core.credentials.CredentialsManager;
import com.microsoft.tfs.core.credentials.CredentialsManagerFactory;
import com.microsoft.tfs.core.httpclient.Credentials;
import com.microsoft.tfs.core.httpclient.UsernamePasswordCredentials;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.StringUtil;

public class IdeaCredentialsManager implements CredentialsManager {
    public static final String GIT_PATH_PREFIX = "/GIT"; //$NON-NLS-1$

    private static final String USER_NAME = "user"; //$NON-NLS-1$
    private static final String PASSWORD = "password"; //$NON-NLS-1$
    private static final String HTTP_SCHEME = "http"; //$NON-NLS-1$
    private static final String HTTPS_SCHEME = "https"; //$NON-NLS-1$
    private static final String ENCODED_SLASH = "\\2f"; //$NON-NLS-1$
    private static final String SLASH = "/"; //$NON-NLS-1$
    private static final String SERVICE_NAME = "TFS"; //$NON-NLS-1$
    private static final String STORED_URIS_KEY = "TFS.StoredCredentialsUris"; //$NON-NLS-1$

    private static final Logger log = Logger.getInstance(IdeaCredentialsManager.class);

    private final String rootPathPrefix;
    private final PersistenceStoreProvider persistenceProvider;

    /*
     * The platform specific credentials manager is used for user name/password
     * credentials only.
     */
    CredentialsManager platformCredentialsManager = null;

    public IdeaCredentialsManager(final PersistenceStoreProvider persistenceProvider) {
        Check.notNull(persistenceProvider, "persistenceProvider"); //$NON-NLS-1$

        this.rootPathPrefix = GIT_PATH_PREFIX;
        this.persistenceProvider = persistenceProvider;
    }

    @Override
    public String getUIMechanismName() {
        return Messages.getString("IdeaCredentialsManager.IntelliJIDEA"); //$NON-NLS-1$
    }

    @Override
    public boolean canWrite() {
        return true;
    }

    @Override
    public boolean isSecure() {
        return true;
    }

    @Override
    public CachedCredentials[] getCredentials() {
        final List<CachedCredentials> credentials = new ArrayList<>(100);

        // Get stored URI list from PropertiesComponent
        final String storedUris = getStoredUris();
        if (StringUtil.isNullOrEmpty(storedUris)) {
            return credentials.toArray(CachedCredentials[]::new);
        }

        final String[] uriArray = storedUris.split("\\|"); //$NON-NLS-1$
        for (final String uriString : uriArray) {
            if (StringUtil.isNullOrEmpty(uriString)) {
                continue;
            }

            try {
                final URI serverURI = URIUtils.newURI(uriString);

                final boolean isHttp;
                //$NON-NLS-1$
                if (StringUtil.isNullOrEmpty(serverURI.getScheme())) {
                    isHttp = false;
                } else isHttp = serverURI.getScheme().equalsIgnoreCase("http") || //$NON-NLS-1$
                        serverURI.getScheme().equalsIgnoreCase("https");

                if (isHttp) {
                    final CachedCredentials cachedCredentials = getCredentials(serverURI);

                    if (cachedCredentials != null) {
                        credentials.add(cachedCredentials);
                    }
                }
            } catch (final Exception e) {
                log.warn("Ignoring the unexpected URI " + uriString + " in the stored credentials list", e); //$NON-NLS-1$ //$NON-NLS-2$
            }
        }

        return credentials.toArray(CachedCredentials[]::new);
    }

    @Override
    public CachedCredentials getCredentials(final URI serverURI) {
        final String key = getNodePath(serverURI);
        final String userNameKey = key + SLASH + USER_NAME;
        final String passwordKey = key + SLASH + PASSWORD;

        try {
            final PasswordSafe passwordSafe = PasswordSafe.getInstance();
            final CredentialAttributes userNameAttributes = createCredentialAttributes(userNameKey);
            final CredentialAttributes passwordAttributes = createCredentialAttributes(passwordKey);

            final String storedUserName = passwordSafe.getPassword(userNameAttributes);
            final String password = passwordSafe.getPassword(passwordAttributes);

            if (!StringUtil.isNullOrEmpty(storedUserName) && !StringUtil.isNullOrEmpty(password)) {
                log.debug("User name & password credentials created"); //$NON-NLS-1$
                return new CachedCredentials(serverURI, storedUserName, password);
            }
        } catch (final Exception e) {
            log.error("Error reading credentials from the IntelliJ IDEA secure store", e); //$NON-NLS-1$
        }

        final CachedCredentials credentials = getPlatformCredentialsManager().getCredentials(serverURI);
        if (credentials != null) {
            final String credentialsType = credentials.isPatCredentials() ? "PAT" //$NON-NLS-1$
                : credentials.isUsernamePasswordCredentials() ? "User name & password" : "Unexpected"; //$NON-NLS-1$ //$NON-NLS-2$
            log.debug(credentialsType + " credentials created found in the platform credentials manager."); //$NON-NLS-1$
        }

        return credentials;
    }

    @Override
    public boolean setCredentials(final CachedCredentials cachedCredentials) {
        final Credentials credentials = cachedCredentials.toCredentials();
        Check.isTrue(
            credentials instanceof UsernamePasswordCredentials,
            "credentials must be UsernamePasswordCredentials"); //$NON-NLS-1$

        try {
            final String key = getNodePath(cachedCredentials.getURI());
            final String userNameKey = key + SLASH + USER_NAME;
            final String passwordKey = key + SLASH + PASSWORD;

            final PasswordSafe passwordSafe = PasswordSafe.getInstance();
            final CredentialAttributes userNameAttributes = createCredentialAttributes(userNameKey);
            final CredentialAttributes passwordAttributes = createCredentialAttributes(passwordKey);

            if (credentials instanceof UsernamePasswordCredentials) {
                passwordSafe.setPassword(userNameAttributes,
                    ((UsernamePasswordCredentials) credentials).getUsername());
            }
            if (credentials instanceof UsernamePasswordCredentials) {
                passwordSafe.setPassword(passwordAttributes,
                    ((UsernamePasswordCredentials) credentials).getPassword());
            }

            // Add URI to stored list
            addStoredUri(cachedCredentials.getURI());
        } catch (final Exception e) {
            log.error("Error writing credentials to the IntelliJ IDEA secure store", e); //$NON-NLS-1$
        }

        return getPlatformCredentialsManager().setCredentials(cachedCredentials);
    }

    @Override
    public boolean removeCredentials(final URI uri) {
        Check.notNull(uri, "uri"); //$NON-NLS-1$

        try {
            final String key = getNodePath(uri);
            final String userNameKey = key + SLASH + USER_NAME;
            final String passwordKey = key + SLASH + PASSWORD;

            final PasswordSafe passwordSafe = PasswordSafe.getInstance();
            final CredentialAttributes userNameAttributes = createCredentialAttributes(userNameKey);
            final CredentialAttributes passwordAttributes = createCredentialAttributes(passwordKey);

            passwordSafe.setPassword(userNameAttributes, null);
            passwordSafe.setPassword(passwordAttributes, null);

            // Remove URI from stored list
            removeStoredUri(uri);
        } catch (final Exception e) {
            log.error("Error removing credentials from the IntelliJ IDEA secure store", e); //$NON-NLS-1$
        }

        return getPlatformCredentialsManager().removeCredentials(uri);
    }

    @Override
    public boolean removeCredentials(final CachedCredentials cachedCredentials) {
        Check.notNull(cachedCredentials, "cachedCredentials"); //$NON-NLS-1$
        Check.notNull(cachedCredentials.getURI(), "cachedCredentials.getURI()"); //$NON-NLS-1$

        return removeCredentials(cachedCredentials.getURI());
    }

    @NotNull
    private CredentialAttributes createCredentialAttributes(@NotNull final String key) {
        return new CredentialAttributes(CredentialAttributesKt.generateServiceName(SERVICE_NAME, key));
    }

    private String getNodePath(final URI serverURI) {
        Check.notNull(serverURI, "serverURI"); //$NON-NLS-1$
        Check.isTrue(serverURI.isAbsolute(), "URI has to be absolute"); //$NON-NLS-1$
        Check.notNull(serverURI.getHost(), "serverURI.getHost()"); //$NON-NLS-1$

        final StringBuilder sb = new StringBuilder(rootPathPrefix);
        sb.append(SLASH);

        final String scheme = serverURI.getScheme();
        sb.append(scheme.toLowerCase());
        sb.append(':');

        sb.append(ENCODED_SLASH);
        sb.append(ENCODED_SLASH);

        final String host = serverURI.getHost().toLowerCase();
        sb.append(host);

        sb.append(':');
        if (serverURI.getPort() < 0) {
            if (scheme.equalsIgnoreCase(HTTP_SCHEME)) {
                sb.append("80"); //$NON-NLS-1$
            } else if (scheme.equalsIgnoreCase(HTTPS_SCHEME)) {
                sb.append("443"); //$NON-NLS-1$
            }
        } else {
            sb.append(serverURI.getPort());
        }

        return sb.toString();
    }

    private CredentialsManager getPlatformCredentialsManager() {
        Check.notNull(persistenceProvider, "persistenceProvider"); //$NON-NLS-1$

        /*
         * Windows always uses CredMan for credential storage, however this is
         * completely handled by the OS, so we can NullCredentialManager.
         *
         * Mac OS uses Keychain for credential storage.
         *
         * Unix uses PersistenceStoreCredentialsManager.
         */
        if (platformCredentialsManager == null) {
            platformCredentialsManager = CredentialsManagerFactory.getCredentialsManager(persistenceProvider);
        }

        return platformCredentialsManager;
    }

    /**
     * Get stored URIs from PropertiesComponent
     */
    private String getStoredUris() {
        return PropertiesComponent.getInstance().getValue(STORED_URIS_KEY);
    }

    /**
     * Add a URI to the stored list
     */
    private void addStoredUri(final URI uri) {
        if (uri == null) {
            return;
        }

        final String uriString = uri.toString();
        final String storedUris = getStoredUris();

        if (StringUtil.isNullOrEmpty(storedUris)) {
            PropertiesComponent.getInstance().setValue(STORED_URIS_KEY, uriString);
        } else if (!storedUris.contains(uriString)) {
            PropertiesComponent.getInstance().setValue(STORED_URIS_KEY, storedUris + "|" + uriString); //$NON-NLS-1$
        }
    }

    /**
     * Remove a URI from the stored list
     */
    private void removeStoredUri(final URI uri) {
        if (uri == null) {
            return;
        }

        final String uriString = uri.toString();
        final String storedUris = getStoredUris();

        if (StringUtil.isNullOrEmpty(storedUris)) {
            return;
        }

        final StringBuilder newUris = new StringBuilder();
        final String[] uriArray = storedUris.split("\\|"); //$NON-NLS-1$
        for (final String storedUri : uriArray) {
            if (!storedUri.equals(uriString)) {
                if (!newUris.isEmpty()) {
                    newUris.append("|"); //$NON-NLS-1$
                }
                newUris.append(storedUri);
            }
        }

        if (newUris.isEmpty()) {
            PropertiesComponent.getInstance().unsetValue(STORED_URIS_KEY);
        } else {
            PropertiesComponent.getInstance().setValue(STORED_URIS_KEY, newUris.toString());
        }
    }
}
