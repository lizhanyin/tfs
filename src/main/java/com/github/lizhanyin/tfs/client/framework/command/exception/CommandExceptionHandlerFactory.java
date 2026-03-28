// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.github.lizhanyin.tfs.client.framework.command.exception;

import com.github.lizhanyin.tfs.client.framework.command.ICommand;

/**
 * A factory class that deals with {@link ICommandExceptionHandler}s.
 */
public final class CommandExceptionHandlerFactory {
    public static ICommandExceptionHandler getDefaultExceptionHandler(final ICommand command) {
        return new MultiCommandExceptionHandler(new ICommandExceptionHandler[] {
            new StandardCancellationExceptionHandler(),
            new CoreExceptionHandler(),
            new DefaultExceptionHandler(command)
        });
    }
}
