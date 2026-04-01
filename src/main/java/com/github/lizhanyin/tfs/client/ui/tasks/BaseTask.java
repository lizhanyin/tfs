// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.github.lizhanyin.tfs.client.ui.tasks;

import com.github.lizhanyin.tfs.client.ui.Task;
import com.github.lizhanyin.tfs.client.ui.framework.UIContext;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nullable;

import com.github.lizhanyin.tfs.client.framework.command.CommandExecutor;
import com.github.lizhanyin.tfs.client.framework.command.ICommandExecutor;
import com.github.lizhanyin.tfs.client.ui.framework.command.UICommandExecutorFactory;

public abstract class BaseTask implements Task {
    private final UIContext uiContext;

    private ICommandExecutor commandExecutor;

    public BaseTask(@Nullable final UIContext uiContext) {
        this.uiContext = uiContext != null ? uiContext : new UIContext(null);

        if (uiContext != null && uiContext.getProject() != null) {
            commandExecutor = UICommandExecutorFactory.newUIJobCommandExecutor(uiContext.getProject());
        } else {
            // Fallback to synchronous executor when no project is available
            // (e.g. during server configuration in the import wizard)
            commandExecutor = new CommandExecutor();
        }
    }

    @Nullable
    protected Project getProject() {
        return uiContext.getProject();
    }

    @Nullable
    protected UIContext getUIContext() {
        return uiContext;
    }

    @Override
    public void setCommandExecutor(final ICommandExecutor commandExecutor) {
        this.commandExecutor = commandExecutor;
    }

    @Override
    public ICommandExecutor getCommandExecutor() {
        return commandExecutor;
    }
}
