// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.github.lizhanyin.tfs.client.server;

public interface ServerManagerProvider {
    public static final String EXTENSION_POINT_ID = "com.microsoft.tfs.client.common.ServerManagerProvider"; //$NON-NLS-1$

    public ServerManager getServerManager();
}
