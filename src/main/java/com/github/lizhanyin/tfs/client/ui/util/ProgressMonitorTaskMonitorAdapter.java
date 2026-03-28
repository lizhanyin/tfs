// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.github.lizhanyin.tfs.client.ui.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.intellij.openapi.progress.ProgressIndicator;

import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.tasks.TaskMonitor;

/**
 * {@link ProgressMonitorTaskMonitorAdapter} adapts an instance of
 * {@link ProgressIndicator} to the {@link TaskMonitor} interface. Most of the
 * {@link TaskMonitor} methods map in a very straightforward way to
 * {@link ProgressIndicator}.
 *
 * @see TaskMonitor
 * @see ProgressIndicator
 */
public class ProgressMonitorTaskMonitorAdapter implements TaskMonitor {
    /**
     * The {@link ProgressIndicator} this adapter is wrapping (never
     * <code>null</code>).
     */
    private final ProgressIndicator progressIndicator;

    /**
     * The starting fraction for this monitor (used for sub-task monitors).
     */
    private final double startFraction;

    /**
     * The total fraction allocated to this monitor (used for sub-task monitors).
     */
    private final double totalFraction;

    /**
     * The parent adapter (for sub-task monitors).
     */
    @Nullable
    private final ProgressMonitorTaskMonitorAdapter parent;

    /**
     * Creates a new {@link ProgressMonitorTaskMonitorAdapter}, wrapping the
     * specified {@link ProgressIndicator}.
     *
     * @param progressIndicator
     *        a {@link ProgressIndicator} to wrap (must not be <code>null</code>)
     */
    public ProgressMonitorTaskMonitorAdapter(@NotNull final ProgressIndicator progressIndicator) {
        this(progressIndicator, null, 0.0, 1.0);
    }

    /**
     * Creates a new {@link ProgressMonitorTaskMonitorAdapter} as a sub-task monitor.
     *
     * @param progressIndicator
     *        a {@link ProgressIndicator} to wrap (must not be <code>null</code>)
     * @param parent
     *        the parent adapter
     * @param startFraction
     *        the starting fraction (0.0 to 1.0)
     * @param totalFraction
     *        the total fraction allocated to this monitor
     */
    private ProgressMonitorTaskMonitorAdapter(
        @NotNull final ProgressIndicator progressIndicator,
        @Nullable final ProgressMonitorTaskMonitorAdapter parent,
        final double startFraction,
        final double totalFraction) {
        Check.notNull(progressIndicator, "progressIndicator"); //$NON-NLS-1$

        this.progressIndicator = progressIndicator;
        this.parent = parent;
        this.startFraction = startFraction;
        this.totalFraction = totalFraction;
    }

    @Override
    public void begin(@Nullable final String taskName, final int totalWork) {
        if (taskName != null) {
            progressIndicator.setText(taskName);
        }
        progressIndicator.setFraction(0.0);
    }

    @Override
    public void beginWithUnknownTotalWork(@Nullable final String taskName) {
        if (taskName != null) {
            progressIndicator.setText(taskName);
        }
        // IDEA's ProgressIndicator doesn't have an "unknown" mode
        // Just set the text and proceed
    }

    @Override
    public void done() {
        progressIndicator.setFraction(startFraction + totalFraction);
    }

    @Override
    public boolean isCanceled() {
        return progressIndicator.isCanceled();
    }

    @Override
    public void setCanceled() {
        progressIndicator.cancel();
    }

    @Override
    @NotNull
    public TaskMonitor newSubTaskMonitor(final int amount) {
        // Calculate the fraction for this sub-task
        // This is a simplified approach - assumes parent has allocated work proportionally
        final double subFraction = totalFraction * (amount / 100.0);
        final double subStartFraction = startFraction + subFraction;

        return new ProgressMonitorTaskMonitorAdapter(
            progressIndicator,
            this,
            subStartFraction,
            subFraction);
    }

    @Override
    public void setCurrentWorkDescription(@Nullable final String description) {
        if (description != null) {
            progressIndicator.setText2(description);
        }
    }

    @Override
    public void setTaskName(@Nullable final String taskName) {
        if (taskName != null) {
            progressIndicator.setText(taskName);
        }
    }

    @Override
    public void worked(final int amount) {
        // Update the fraction based on work done
        // This is a simplified implementation
        final double currentFraction = progressIndicator.getFraction();
        final double increment = totalFraction * (amount / 100.0);
        progressIndicator.setFraction(Math.min(currentFraction + increment, startFraction + totalFraction));
    }

    public @Nullable ProgressMonitorTaskMonitorAdapter getParent() {
        return parent;
    }
}
