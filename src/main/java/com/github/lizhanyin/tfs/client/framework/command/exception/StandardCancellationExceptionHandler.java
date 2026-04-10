// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.github.lizhanyin.tfs.client.framework.command.exception;


import com.github.lizhanyin.tfs.TFSClientPlugin;
import com.github.lizhanyin.tfs.runtime.IStatus;
import com.github.lizhanyin.tfs.runtime.OperationCanceledException;
import com.github.lizhanyin.tfs.runtime.Status;
import com.microsoft.tfs.core.ws.runtime.exceptions.TransportRequestHandlerCanceledException;

/**
 * An {@link ICommandExceptionHandler} that handles {@link InterruptedException}
 * s and {@link OperationCanceledException}s. They are handled by returning an
 * {@link IStatus} with severity {@link IStatus#CANCEL}.
 *
 * @see ICommandExceptionHandler
 * @see InterruptedException
 * @see OperationCanceledException
 */
public class StandardCancellationExceptionHandler implements ICommandExceptionHandler {
    @Override
    public IStatus onException(final Throwable t) {
        if (t instanceof InterruptedException
            || t instanceof OperationCanceledException
            || t instanceof TransportRequestHandlerCanceledException) {
            return new Status(IStatus.CANCEL, TFSClientPlugin.PLUGIN_ID, 0, null, t);
        }

        return null;
    }
}
