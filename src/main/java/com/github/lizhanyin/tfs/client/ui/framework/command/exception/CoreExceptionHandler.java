// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.github.lizhanyin.tfs.client.ui.framework.command.exception;

import com.github.lizhanyin.tfs.runtime.CoreException;
import com.github.lizhanyin.tfs.runtime.IStatus;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * An {@link ICommandExceptionHandler} that handles {@link CoreException}s. They
 * are handled by returning their {@link IStatus} available from
 * {@link CoreException#getStatus()}.
 *
 * @see ICommandExceptionHandler
 * @see CoreException
 */
public class CoreExceptionHandler implements ICommandExceptionHandler {
    private static final Log log = LogFactory.getLog(CoreExceptionHandler.class);

    /*
     * (non-Javadoc)
     *
     * @seecom.microsoft.tfs.client.common.ui.shared.command.exception.
     * CommandExceptionHandler#doHandleException(java.lang.Throwable,
     * com.microsoft.tfs.client.common.ui.shared.command.ICommand)
     */
    @Override
    public IStatus onException(final Throwable t) {
        log.error(t.getMessage(), t);

        if (t instanceof CoreException) {
            return ((CoreException) t).getStatus();
        }

        return null;
    }
}
