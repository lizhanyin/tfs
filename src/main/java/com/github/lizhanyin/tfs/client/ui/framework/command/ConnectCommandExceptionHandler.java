// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.github.lizhanyin.tfs.client.ui.framework.command;


import com.github.lizhanyin.tfs.client.TFSCommonClientPlugin;
import com.github.lizhanyin.tfs.client.framework.command.exception.ICommandExceptionHandler;
import com.github.lizhanyin.tfs.runtime.IStatus;
import com.github.lizhanyin.tfs.runtime.Status;
import com.microsoft.tfs.jni.internal.ntlm.NTLMVersionException;

/**
 * Exception handlers for the connect to server commands.
 *
 * @threadsafety unknown
 */
public final class ConnectCommandExceptionHandler implements ICommandExceptionHandler {
    @Override
    public IStatus onException(final Throwable t) {
        if (t instanceof NTLMVersionException) {
            return new Status(IStatus.ERROR, TFSCommonClientPlugin.PLUGIN_ID, 0, t.getLocalizedMessage(), null);
        }

        return null;
    }
}