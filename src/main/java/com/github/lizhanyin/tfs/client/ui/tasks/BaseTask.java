// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.github.lizhanyin.tfs.client.ui.tasks;

import com.github.lizhanyin.tfs.client.ui.Task;
import com.intellij.openapi.project.Project;

import com.github.lizhanyin.tfs.client.framework.command.ICommandExecutor;
import com.github.lizhanyin.tfs.client.ui.framework.command.UICommandExecutorFactory;
import com.microsoft.tfs.util.Check;

public abstract class BaseTask implements Task {
    private final Project project;

    private ICommandExecutor commandExecutor;

    public BaseTask(final Project project) {
        Check.notNull(project, "project"); //$NON-NLS-1$

        this.project = project;
        commandExecutor = UICommandExecutorFactory.newUICommandExecutor(project);
    }

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
