// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.github.lizhanyin.tfs.client.ui.framework.command;


import com.github.lizhanyin.tfs.client.framework.command.ICommand;
import com.github.lizhanyin.tfs.client.ui.framework.UIContext;
import com.intellij.openapi.progress.ProgressIndicator;
import com.microsoft.tfs.util.Check;

/**
 * A concrete subclass of {@link AbstractRunnableContextCommandExecutor} that is
 * passed a {@link ProgressIndicator} at construction. This progress indicator is
 * then used for all command executions.
 *
 * @see AbstractRunnableContextCommandExecutor
 */
public class RunnableContextCommandExecutor extends AbstractRunnableContextCommandExecutor {
    private final ProgressIndicator progressIndicator;

    /**
     * Creates a new instance of {@link RunnableContextCommandExecutor}. The
     * specified {@link ProgressIndicator} will be used for all command
     * executions.
     *
     * @param uiContext
     *        the {@link UIContext} (must not be <code>null</code>)
     * @param progressIndicator
     *        a {@link ProgressIndicator} to use (must not be <code>null</code>)
     */
    public RunnableContextCommandExecutor(final UIContext uiContext, final ProgressIndicator progressIndicator) {
        super(uiContext);

        Check.notNull(progressIndicator, "progressIndicator"); //$NON-NLS-1$

        this.progressIndicator = progressIndicator;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.github.lizhanyin.tfs.client.ui.framework.command.
     * AbstractRunnableContextCommandExecutor
     * #getProgressIndicator(com.github.lizhanyin.tfs.client.framework.command.ICommand)
     */
    @Override
    protected ProgressIndicator getProgressIndicator(final ICommand command) {
        return progressIndicator;
    }
}
