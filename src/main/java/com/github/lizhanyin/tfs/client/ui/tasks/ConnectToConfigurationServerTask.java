// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.github.lizhanyin.tfs.client.ui.tasks;

import java.net.URI;

import com.intellij.openapi.diagnostic.Logger;
import com.microsoft.tfs.core.TFSTeamProjectCollection;

import com.github.lizhanyin.tfs.client.ui.framework.UIContext;
import com.github.lizhanyin.tfs.client.ui.commands.ConnectCommand;
import com.github.lizhanyin.tfs.client.ui.commands.ConnectToConfigurationServerCommand;
import com.microsoft.tfs.core.httpclient.Credentials;

/**
 * Connects to a {@link TFSTeamProjectCollection}.
 *
 * @threadsafety unknown
 */
public class ConnectToConfigurationServerTask extends ConnectTask {
    private static final Logger LOG = Logger.getInstance(ConnectToConfigurationServerTask.class);

    /**
     * Connects to the given server URI.
     *
     * @param project
     *        a valid {@link UIContext}
     * @param serverURI
     *        the server URI to connect to
     */
    public ConnectToConfigurationServerTask(final UIContext project, final URI serverURI) {
        super(project, serverURI, null);
    }

    /**
     * Connects to the given server URI.
     *
     * @param project
     *        a valid {@link UIContext}
     * @param serverURI
     *        the server URI to connect to
     * @param credentials
     *        the credentials to connect with (or <code>null</code>)
     */
    public ConnectToConfigurationServerTask(final UIContext project, final URI serverURI, final Credentials credentials) {
        super(project, serverURI, credentials);
    }

    @Override
    protected ConnectCommand getConnectCommand(final URI serverURI, final Credentials credentials) {
        return new ConnectToConfigurationServerCommand(serverURI, credentials);
    }
}
