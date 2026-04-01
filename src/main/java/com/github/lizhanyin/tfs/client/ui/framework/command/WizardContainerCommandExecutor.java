// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.github.lizhanyin.tfs.client.ui.framework.command;

import com.github.lizhanyin.tfs.client.framework.command.ICommand;
import com.github.lizhanyin.tfs.client.ui.framework.UIContext;
import com.intellij.openapi.progress.ProgressIndicator;
import com.microsoft.tfs.util.Check;

/**
 * <p>
 * A subclass of {@link RunnableContextCommandExecutor} that is constructed with
 * a {@link UIContext} and a {@link ProgressIndicator}. This executor runs
 * {@link ICommand}s using IntelliJ's progress infrastructure.
 * </p>
 *
 * <p>
 * In addition, this executor will raise an error dialog on the parent
 * window if the status when a command is finished meets
 * certain criteria, as determined by {@link ErrorDialogCommandFinishedCallback}.
 * </p>
 *
 * @see RunnableContextCommandExecutor
 * @see ErrorDialogCommandFinishedCallback
 */
public class WizardContainerCommandExecutor extends RunnableContextCommandExecutor {
    /**
     * Creates a new {@link WizardContainerCommandExecutor} that uses the
     * specified {@link UIContext} and {@link ProgressIndicator}.
     *
     * @param uiContext
     *        the {@link UIContext} (must not be <code>null</code>)
     * @param progressIndicator
     *        the {@link ProgressIndicator} to use (must not be <code>null</code>)
     */
    public WizardContainerCommandExecutor(final UIContext uiContext, final ProgressIndicator progressIndicator) {
        super(uiContext, progressIndicator);

        Check.notNull(uiContext, "uiContext"); //$NON-NLS-1$
        Check.notNull(progressIndicator, "progressIndicator"); //$NON-NLS-1$
    }

    /**
     * Creates a new {@link WizardContainerCommandExecutor} that uses the
     * {@link ProgressIndicator} from the specified {@link UIContext}.
     *
     * @param uiContext
     *        the {@link UIContext} (must not be <code>null</code>,
     *        must contain a non-null {@link ProgressIndicator})
     */
    public WizardContainerCommandExecutor(final UIContext uiContext) {
        super(uiContext, uiContext.getProgressIndicator());

        Check.notNull(uiContext, "uiContext"); //$NON-NLS-1$
        Check.notNull(uiContext.getProgressIndicator(), "progressIndicator"); //$NON-NLS-1$
    }
}
