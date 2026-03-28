// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.github.lizhanyin.tfs.client.ui.framework.command;

import com.github.lizhanyin.tfs.client.framework.command.ICommandExecutor;
import com.github.lizhanyin.tfs.client.framework.command.JobOptions;
import com.github.lizhanyin.tfs.client.framework.command.ThreadCommandExecutor;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


/**
 * <p>
 * {@link UICommandExecutorFactory} provides access to the most commonly used
 * {@link ICommandExecutor} implementations. To get an {@link ICommandExecutor}
 * from this factory call one of the <code>new*</code> methods, passing the
 * appropriate parameters (if any).
 * </p>
 *
 * <p>
 * There's nothing wrong with using the {@link ICommandExecutor} implementations
 * directly instead of using this factory. However, by using the factory, you
 * don't need to remember the individual implementation class names and you can
 * use code completion in the IDE to help choose an appropriate executor. In
 * addition, using this factory provides a layer of indirection that means less
 * code has to change as improved {@link ICommandExecutor} implementations are
 * written. Also, clients should not cast the {@link ICommandExecutor}s returned
 * by this factory to a concrete implementation. The implementations returned by
 * this factory are not guaranteed to remain the same.
 * </p>
 *
 * @see ICommandExecutor
 */
public class UICommandExecutorFactory {
    /**
     * Creates a new asynchronous {@link ICommandExecutor} that makes use of the
     * IntelliJ IDEA task framework and can process UI thread messages while waiting
     * for tasks to finish.
     *
     * @param project
     *        the IntelliJ {@link Project} (must not be <code>null</code>)
     * @return a new {@link ICommandExecutor} as described above
     */
    public static ICommandExecutor newUIJobCommandExecutor(@NotNull final Project project) {
        return new UIJobCommandExecutor(project);
    }

    /**
     * <p>
     * Creates a new asynchronous {@link ICommandExecutor} that makes use of the
     * IntelliJ IDEA task framework and can process UI thread messages while waiting
     * for tasks to finish. Certain attributes of the {@link Task.Backgroundable}s
     * that are created by the executor can be controlled through the specified
     * {@link JobOptions} parameter.
     * </p>
     *
     * <p>
     * Note that no reference is held to the given {@link JobOptions} object
     * after this method returns. This means that any changes made to the
     * {@link JobOptions} after calling this method will not impact the returned
     * executor.
     * </p>
     *
     * @param project
     *        the IntelliJ {@link Project} (must not be <code>null</code>)
     * @param jobOptions
     *        a {@link JobOptions} instance containing values used to configure
     *        new {@link Task.Backgroundable}s, or <code>null</code> to use default
     *        configuration
     * @return a new {@link ICommandExecutor} as described above
     */
    public static ICommandExecutor newUIJobCommandExecutor(
            @NotNull final Project project,
            @Nullable final JobOptions jobOptions) {
        return new UIJobCommandExecutor(project, jobOptions);
    }

    /**
     * Creates a new asynchronous {@link ICommandExecutor} that uses background
     * threads to execute commands.
     *
     * @return a new {@link ICommandExecutor} as described above
     */
    public static ICommandExecutor newThreadCommandExecutor() {
        return new ThreadCommandExecutor();
    }
}
