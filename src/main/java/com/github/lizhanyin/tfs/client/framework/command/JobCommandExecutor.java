// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.github.lizhanyin.tfs.client.framework.command;

import com.github.lizhanyin.tfs.runtime.IStatus;
import com.github.lizhanyin.tfs.runtime.Status;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jetbrains.annotations.NotNull;

import com.intellij.openapi.progress.Task;

import com.github.lizhanyin.tfs.TFSClientPlugin;

/**
 * <p>
 * This class implements a non-blocking {@link ICommandExecutor} by making use
 * of the IntelliJ IDEA {@link Task.Backgroundable} framework.
 * </p>
 *
 * <p>
 * The {@link ICommandExecutor#execute(ICommand)} method is implemented by
 * queuing a new {@link Task.Backgroundable} to run the specified {@link ICommand} and then
 * returning immediately.
 * </p>
 *
 * <p>
 * As a non-blocking {@link ICommandExecutor}, this executor returns
 * <code>true</code> from {@link #isAsync()} and returns a {@link FutureStatus}
 * from the {@link #execute(ICommand)} method. The {@link FutureStatus} returned
 * by this executor returns the {@link Task.Backgroundable} produced by executor from the
 * {@link FutureStatus#getAsyncObject()} method.
 * </p>
 *
 * <p>
 * Attributes of the {@link Task.Backgroundable}s created and queued by this executor can be
 * configured by passing an instance of {@link JobOptions} at construction time.
 * The no-args constructor uses default values for all of the configurable
 * {@link Task} attributes - see the {@link JobOptions} class for details.
 * </p>
 *
 * @see ICommandExecutor
 * @see Task.Backgroundable
 * @see FutureStatus
 * @see JobOptions
 */
public class JobCommandExecutor extends CommandExecutor {
    private static final Log log = LogFactory.getLog(JobCommandExecutor.class);

    private final JobOptions jobOptions;

    /**
     * Creates a new {@link JobCommandExecutor} that creates {@link Task.Backgroundable}s
     * configured with default attributes. For control over some of the
     * {@link Task} attributes, use the {@link #JobCommandExecutor(JobOptions)}
     * constructor instead.
     */
    public JobCommandExecutor() {
        this(null);
    }

    /**
     * <p>
     * Creates a new {@link JobCommandExecutor}. {@link Task.Backgroundable}s created and
     * queued by this executor will be configured with the attribute values
     * that are set in the specified {@link JobOptions} instance.
     * </p>
     * <p>
     * Note that this executor does not hold a reference to the specified
     * {@link JobOptions} after this constructor returns - changes to the
     * {@link JobOptions} instance made after passing it to this constructor
     * will not be reflected in this {@link JobCommandExecutor}.
     * </p>
     *
     * @param jobOptions
     *        holds attribute values that are used to configure {@link Task.Backgroundable}s
     *        created by this {@link JobCommandExecutor} (pass <code>null</code>
     *        to use default values for all configurable attributes)
     *
     */
    public JobCommandExecutor(final JobOptions jobOptions) {
        this.jobOptions = new JobOptions(jobOptions);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.github.lizhanyin.tfs.client.ui.ui.shared.command.CommandExecutor#isAsync
     * ()
     */
    @Override
    public boolean isAsync() {
        return true;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.github.lizhanyin.tfs.client.ui.ui.shared.command.CommandExecutor#execute
     * (com.microsoft.tfs.client.command.ICommand)
     */
    @Override
    public IStatus execute(@NotNull final ICommand command) {
        final Task.Backgroundable task = jobOptions.createTaskFor(command, getCommandStartedCallback(), getCommandFinishedCallback());

        task.queue();

        return new TaskFutureStatus(task);
    }

    /**
     * A subclass of {@link AbstractFutureStatus} that implements a
     * {@link FutureStatus} based around using a {@link Task.Backgroundable} as the async
     * object.
     * <p>
     * Delegates {@link #join()} duties to the
     * {@link ExtensionPointAsyncObjectWaiter} to give UI plug-ins a chance to
     * keep UI events going.
     */
    protected static class TaskFutureStatus extends AbstractFutureStatus {
        protected final Task.Backgroundable task;
        protected final JobCommandAdapter taskAdapter;

        private volatile boolean completed = false;
        private IStatus taskResult;
        private final Object taskResultLock = new Object();

        public TaskFutureStatus(final Task.Backgroundable task) {
            super(task);
            this.task = task;
            this.taskAdapter = (task instanceof JobCommandAdapter) ? (JobCommandAdapter) task : null;
        }

        @Override
        public boolean isCompleted() {
            return taskAdapter != null || completed;
        }

        @Override
        public final void join() {
            try {
                // Use the implementation that can defer to extensions
                new ExtensionPointAsyncObjectWaiter().joinTask(task);
            } catch (final InterruptedException e) {
                synchronized (taskResultLock) {
                    taskResult = new Status(Status.ERROR, TFSClientPlugin.PLUGIN_ID, 0, null, e);
                }
            }
        }

        @Override
        protected IStatus getCompletedStatus() {
            synchronized (taskResultLock) {
                if (taskResult == null && taskAdapter != null) {
                    taskResult = taskAdapter.getStatus();
                }
                completed = taskResult != null;
                return taskResult != null ? taskResult : Status.OK_STATUS;
            }
        }
    }
}
