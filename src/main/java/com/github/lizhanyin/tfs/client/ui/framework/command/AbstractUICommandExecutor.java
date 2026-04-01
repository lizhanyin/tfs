// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.github.lizhanyin.tfs.client.ui.framework.command;

import com.github.lizhanyin.tfs.client.framework.command.CommandExecutor;
import com.github.lizhanyin.tfs.client.framework.command.ICommandFinishedCallback;
import com.github.lizhanyin.tfs.client.ui.framework.UIContext;
import com.microsoft.tfs.util.Check;

/**
 * This is an abstract command executor class. It exists only to add the various
 * UI-related {@link ICommandFinishedCallback}s.
 */
public abstract class AbstractUICommandExecutor extends CommandExecutor {
    private final UIContext uiContext;

    public AbstractUICommandExecutor(final UIContext uiContext) {
        Check.notNull(uiContext, "uiContext"); //$NON-NLS-1$

        this.uiContext = uiContext;
        setCommandFinishedCallback(UICommandFinishedCallbackFactory.getDefaultCallback(uiContext));
    }

    public UIContext getUIContext() {
        return uiContext;
    }
}
