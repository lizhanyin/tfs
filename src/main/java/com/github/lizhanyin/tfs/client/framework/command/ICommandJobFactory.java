// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.github.lizhanyin.tfs.client.framework.command;

import com.intellij.openapi.progress.Task;

/**
 * <p>
 * An {@link ICommandJobFactory} is used by the {@link JobCommandExecutor} to
 * create new {@link Task.Backgroundable} instances for {@link ICommand}s.
 * </p>
 */
public interface ICommandJobFactory {
    /**
     * Called to create a new {@link Task.Backgroundable} instance for the specified
     * {@link ICommand} and {@link ICommandFinishedCallback}. The returned
     * {@link Task} should run the {@link ICommand} when the task is run, and it
     * should invoke the {@link ICommandFinishedCallback} after the
     * {@link ICommand} has completed.
     *
     * @param command
     *        an {@link ICommand} (must not be <code>null</code>)
     * @param commandStartedCallback
     *        an {@link ICommandStartedCallback} (may be <code>null</code>)
     * @param commandFinishedCallback
     *        an {@link ICommandFinishedCallback} (may be <code>null</code>)
     * @return a new {@link Task.Backgroundable} instance (must not be <code>null</code>)
     */
    Task.Backgroundable newTaskFor(
        ICommand command,
        ICommandStartedCallback commandStartedCallback,
        ICommandFinishedCallback commandFinishedCallback);
}
