// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.github.lizhanyin.tfs.client.ui.framework.command;

import com.github.lizhanyin.tfs.client.framework.command.*;
import com.microsoft.tfs.util.Check;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;

import com.github.lizhanyin.tfs.runtime.IStatus;
import com.github.lizhanyin.tfs.runtime.Status;

/**
 * A {@link JobCommandExecutor} that understands that IntelliJ IDEA has a UI, and thus
 * can make use of displaying warnings, error dialogs, etc.
 * <p>
 * <em>
 * Always prefer {@link UIJobCommandExecutor} to {@link JobCommandExecutor} in a
 * graphical context.
 * </em>
 * </p>
 * <p>
 * An important feature of this extension to {@link JobCommandExecutor} is its
 * ability to process UI thread messages while waiting for a task to finish, when
 * the thread waiting on the task is the UI thread (see the implementation in
 * {@link UIJobFutureStatus}). The base class, {@link JobCommandExecutor}, does
 * not offer this feature. It waits with a simple blocking join
 * which prevents the running {@link Task.Backgroundable} from doing work on the UI thread.
 * </p>
 */
public class UIJobCommandExecutor extends JobCommandExecutor {
    public UIJobCommandExecutor(@NotNull final Project project) {
        this(project, null);
    }

    public UIJobCommandExecutor(@NotNull final Project project, @Nullable final JobOptions jobOptions) {
        super(createJobOptions(jobOptions));

        Check.notNull(project, "project"); //$NON-NLS-1$
        setCommandFinishedCallback(UICommandFinishedCallbackFactory.getDefaultCallback(project));
    }

    private static JobOptions createJobOptions(@Nullable final JobOptions jobOptions) {
        final JobOptions newJobOptions = new JobOptions(jobOptions);
        newJobOptions.setCommandTaskFactory(new UICommandTaskFactory());

        return newJobOptions;
    }

    /**
     * A default implementation of {@link ICommandJobFactory}, which creates new
     * {@link UIJobCommandAdapter} instances to satisfy the
     * {@link #newTaskFor(ICommand, ICommandStartedCallback, ICommandFinishedCallback)} method.
     */
    private static class UICommandTaskFactory implements ICommandJobFactory {
        /**
         * {@inheritDoc}
         */
        @Override
        public Task.Backgroundable newTaskFor(
                @NotNull final ICommand command,
                @Nullable final ICommandStartedCallback commandStartedCallback,
                @Nullable final ICommandFinishedCallback commandFinishedCallback) {
            return new UIJobCommandAdapter(command, commandStartedCallback, commandFinishedCallback);
        }
    }
}