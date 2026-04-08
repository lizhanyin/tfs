// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.github.lizhanyin.tfs.client.background;

import com.github.lizhanyin.tfs.client.framework.command.ICommand;
import com.intellij.openapi.progress.ProgressIndicator;

import com.microsoft.tfs.util.Check;

/**
 * An {@link IBackgroundTask} that is backed by a TFS {@link ICommand}.
 */
public class CommandBackgroundTask implements IBackgroundTask {
    private final ICommand command;
    private final ProgressIndicator progressIndicator;

    public CommandBackgroundTask(final ICommand command) {
        this(command, null);
    }

    public CommandBackgroundTask(final ICommand command, final ProgressIndicator progressIndicator) {
        Check.notNull(command, "command"); //$NON-NLS-1$

        this.command = command;
        this.progressIndicator = progressIndicator;
    }

    @Override
    public String getName() {
        return command.getName();
    }

    @Override
    public boolean isCancellable() {
        return progressIndicator != null && command.isCancellable();
    }

    @Override
    public boolean cancel() {
        if (progressIndicator != null) {
            progressIndicator.cancel();
            return progressIndicator.isCanceled();
        }

        return false;
    }
}