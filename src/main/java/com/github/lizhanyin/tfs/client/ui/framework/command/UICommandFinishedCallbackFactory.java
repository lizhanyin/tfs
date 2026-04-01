// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.github.lizhanyin.tfs.client.ui.framework.command;

import com.github.lizhanyin.tfs.client.framework.command.CommandFinishedCallbackFactory;
import com.github.lizhanyin.tfs.client.framework.command.ICommandFinishedCallback;
import com.github.lizhanyin.tfs.client.framework.command.MultiCommandFinishedCallback;
import com.github.lizhanyin.tfs.client.ui.framework.UIContext;
import com.intellij.openapi.project.Project;

public class UICommandFinishedCallbackFactory extends CommandFinishedCallbackFactory {
    protected UICommandFinishedCallbackFactory() {
    }

    /**
     * This will attempt to derive the project from the current context. It is
     * recommended instead that you use {@link #getDefaultCallback(UIContext)}
     * instead.
     *
     * @return A command finished callback that does not participate in UI.
     */
    public static ICommandFinishedCallback getDefaultCallback() {
        return getDefaultCallback((UIContext) null);
    }

    /**
     * The standard UI command finished callback. This will display a warning or
     * error dialog given the severity.
     *
     * @return A command finished callback that participates in UI.
     */
    public static ICommandFinishedCallback getDefaultCallback(final Project project) {
        return getDefaultCallback(UIContext.from(project));
    }

    /**
     * The standard UI command finished callback. This will display a warning or
     * error dialog given the severity.
     *
     * @return A command finished callback that participates in UI.
     */
    public static ICommandFinishedCallback getDefaultCallback(final UIContext uiContext) {
        /* Make sure to include the default non-UI callbacks */
        final ICommandFinishedCallback nonUiCallbacks = CommandFinishedCallbackFactory.getDefaultCallback();

        final ICommandFinishedCallback uiCallbacks =
            MultiCommandFinishedCallback.combine(getConsoleWriterCallback(), getErrorDialogCallback(uiContext));

        return MultiCommandFinishedCallback.combine(nonUiCallbacks, uiCallbacks);
    }

    public static ICommandFinishedCallback getDefaultNoErrorDialogCallback() {
        /* Make sure to include the default non-UI callbacks */
        final ICommandFinishedCallback nonUiCallbacks = CommandFinishedCallbackFactory.getDefaultCallback();

        final ICommandFinishedCallback uiCallbacks = getConsoleWriterCallback();

        return MultiCommandFinishedCallback.combine(nonUiCallbacks, uiCallbacks);
    }

    public static ICommandFinishedCallback getConsoleWriterCallback() {
        return new ConsoleWriterCommandFinishedCallback();
    }

    public static ICommandFinishedCallback getErrorDialogCallback(final UIContext uiContext) {
        return new ErrorDialogCommandFinishedCallback(uiContext);
    }

    public static ICommandFinishedCallback getErrorDialogCallback(final Project project) {
        return new ErrorDialogCommandFinishedCallback(UIContext.from(project));
    }
}
