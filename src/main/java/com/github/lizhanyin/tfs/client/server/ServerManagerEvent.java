// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.github.lizhanyin.tfs.client.server;

import java.io.Serial;
import java.util.EventObject;

public class ServerManagerEvent extends EventObject {
    @Serial
    private static final long serialVersionUID = 1L;

    private final TFSServer server;

    public ServerManagerEvent(final ServerManager source, final TFSServer server) {
        super(source);

        this.server = server;
    }

    public ServerManager getServerManager() {
        return (ServerManager) getSource();
    }

    public TFSServer getServer() {
        return server;
    }
}