// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.github.lizhanyin.tfs.client.framework.command;

import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.intellij.openapi.progress.Task;

import com.microsoft.tfs.util.Check;

/**
 * <p>
 * A {@link JobOptions} is a simple data holder class that holds a set of
 * configuration options used to create IntelliJ {@link Task}s. A
 * {@link JobOptions}s instance is passed to a {@link JobCommandExecutor} during
 * construction, and that executor uses the option values when creating new
 * {@link Task}s.
 * </p>
 *
 * <p>
 * To configure {@link Task} attributes, call one of the public setter methods on
 * an instance of this class. If an attribute is not set, it holds a default
 * value that is usually appropriate for most {@link Task}s. The default values
 * are public API documented in this class.
 * </p>
 *
 * @see Task
 * @see JobCommandExecutor
 */
public class JobOptions {
    /**
     * The default scheduling delay, equal to <code>0</code> (no delay).
     */
    public static final long DEFAULT_DELAY = 0;

    /**
     * The default <i>canBeCancelled</i> attribute, equal to <code>true</code>.
     */
    public static final boolean DEFAULT_CANCELABLE = true;

    /**
     * The default {@link ICommandJobFactory}, which makes use of the
     * {@link JobCommandExecutor} class to create new {@link Task} instances.
     */
    public static final ICommandJobFactory DEFAULT_COMMAND_TASK_FACTORY = new DefaultCommandTaskFactory();

    private long delay = DEFAULT_DELAY;
    private boolean cancelable = DEFAULT_CANCELABLE;
    private ICommandJobFactory commandTaskFactory = DEFAULT_COMMAND_TASK_FACTORY;
    private final Map<String, Object> properties = new HashMap<>();

    /**
     * Creates a new {@link JobOptions} that holds default values for all
     * configuration data.
     */
    public JobOptions() {
    }

    /**
     * Creates a new {@link JobOptions}. If the specified {@link JobOptions}
     * instance is not <code>null</code>, all of the other instance's
     * configuration data will be copied into this instance.
     *
     * @param other
     *        another {@link JobOptions} instance to copy configuration data
     *        from, or <code>null</code>
     */
    public JobOptions(@Nullable final JobOptions other) {
        if (other == null) {
            return;
        }

        delay = other.delay;
        cancelable = other.cancelable;
        commandTaskFactory = other.commandTaskFactory;
        properties.putAll(other.properties);
    }

    /**
     * Creates a new {@link Task.Backgroundable} instance, using the {@link ICommandJobFactory}
     * held by this {@link JobOptions}.
     *
     * @param command
     *        an {@link ICommand} (must not be <code>null</code>)
     * @param commandStartedCallback
     *        an {@link ICommandStartedCallback} (may be <code>null</code>)
     * @param commandFinishedCallback
     *        an {@link ICommandFinishedCallback} (may be <code>null</code>)
     * @return a new {@link Task.Backgroundable} instance as per the {@link ICommandJobFactory}
     *         contract
     */
    public Task.Backgroundable createTaskFor(
        @NotNull final ICommand command,
        @Nullable final ICommandStartedCallback commandStartedCallback,
        @Nullable final ICommandFinishedCallback commandFinishedCallback) {
        return commandTaskFactory.newTaskFor(command, commandStartedCallback, commandFinishedCallback);
    }

    /**
     * @return the delay value currently held by this {@link JobOptions}
     */
    public long getDelay() {
        return delay;
    }

    /**
     * Sets the scheduling delay value of this {@link JobOptions} instance. The
     * default value is {@link #DEFAULT_DELAY}.
     *
     * @param delay
     *        a scheduling delay value in milliseconds
     * @return this {@link JobOptions} instance for method chaining
     */
    @NotNull
    public JobOptions setDelay(final long delay) {
        this.delay = delay;
        return this;
    }

    /**
     * @return the cancelable option value currently held by this {@link JobOptions}
     */
    public boolean isCancelable() {
        return cancelable;
    }

    /**
     * Sets the cancelable option value of this {@link JobOptions} instance. The
     * default value is {@link #DEFAULT_CANCELABLE}.
     *
     * @param cancelable
     *        a cancelable value
     * @return this {@link JobOptions} instance for method chaining
     */
    @NotNull
    public JobOptions setCancelable(final boolean cancelable) {
        this.cancelable = cancelable;
        return this;
    }

    /**
     * @return the {@link ICommandJobFactory} currently held by this
     *         {@link JobOptions}
     */
    @NotNull
    public ICommandJobFactory getCommandTaskFactory() {
        return commandTaskFactory;
    }

    /**
     * Sets the command task factory of this {@link JobOptions} instance. The
     * default value is {@link #DEFAULT_COMMAND_TASK_FACTORY}. When creating new
     * {@link Task}s, the factory is used to obtain a {@link Task} instance from
     * an {@link ICommand}.
     *
     * @param commandTaskFactory
     *        an {@link ICommandJobFactory} (must not be <code>null</code>)
     * @return this {@link JobOptions} instance for method chaining
     */
    @NotNull
    public JobOptions setCommandTaskFactory(@NotNull final ICommandJobFactory commandTaskFactory) {
        Check.notNull(commandTaskFactory, "commandTaskFactory"); //$NON-NLS-1$

        this.commandTaskFactory = commandTaskFactory;
        return this;
    }

    /**
     * Sets a property that can be used by tasks created using this {@link JobOptions}.
     *
     * @param key
     *        the property key (must not be <code>null</code>)
     * @param value
     *        the property value
     */
    public void setProperty(@NotNull final String key, @Nullable final Object value) {
        Check.notNull(key, "key"); //$NON-NLS-1$

        properties.put(key, value);
    }

    /**
     * Gets a property value.
     *
     * @param key
     *        the property key
     * @return the property value, or <code>null</code> if not set
     */
    @Nullable
    public Object getProperty(@NotNull final String key) {
        return properties.get(key);
    }

    /**
     * A default implementation of {@link ICommandJobFactory}, which creates new
     * {@link JobCommandExecutor} instances to satisfy the
     * {@link #newTaskFor(ICommand, ICommandStartedCallback, ICommandFinishedCallback)} method.
     */
    private static class DefaultCommandTaskFactory implements ICommandJobFactory {
        @Override
        public Task.Backgroundable newTaskFor(
            @NotNull final ICommand command,
            @Nullable final ICommandStartedCallback commandStartedCallback,
            @Nullable final ICommandFinishedCallback commandFinishedCallback) {
            return new JobCommandExecutor(command, commandStartedCallback, commandFinishedCallback);
        }
    }
}
