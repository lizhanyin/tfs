// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.github.lizhanyin.tfs.client.ui.framework.commands;

import com.github.lizhanyin.tfs.client.ui.framework.command.ICommand;
import com.microsoft.tfs.core.TFSConnection;

/**
 * Base connect command interface.
 *
 * @threadsafety unknown
 */
public interface ConnectCommand extends ICommand {
    TFSConnection getConnection();
}
