// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.github.lizhanyin.tfs.client.ui.tasks;

import com.github.lizhanyin.tfs.client.ui.Task;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nullable;

import com.github.lizhanyin.tfs.client.framework.command.CommandExecutor;
import com.github.lizhanyin.tfs.client.framework.command.ICommandExecutor;
import com.github.lizhanyin.tfs.client.ui.framework.command.UICommandExecutorFactory;

public abstract class BaseTask implements Task {
    private final Project project;

    private ICommandExecutor commandExecutor;

    public BaseTask(@Nullable final Project project) {
        this.project = project;

        if (project != null) {
            commandExecutor = UICommandExecutorFactory.newUIJobCommandExecutor(project);
        } else {
            // Fallback to synchronous executor when no project is available
            // (e.g. during server configuration in the import wizard)
            commandExecutor = new CommandExecutor();
        }
    }

    @Nullable
    protected Project getProject() {
        return project;
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
