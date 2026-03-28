// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.github.lizhanyin.tfs.client.ui.framework.command;

import com.github.lizhanyin.tfs.client.framework.command.CommandFinishedCallbackFactory;
import com.github.lizhanyin.tfs.client.framework.command.ICommandFinishedCallback;
import com.github.lizhanyin.tfs.client.framework.command.MultiCommandFinishedCallback;
import com.intellij.openapi.project.Project;

public class UICommandFinishedCallbackFactory extends CommandFinishedCallbackFactory {
    protected UICommandFinishedCallbackFactory() {
    }

    /**
     * This will attempt to derive the project from the current context. It is
     * recommended instead that you use {@link #getDefaultCallback(Project)}
     * instead.
     *
     * @return A command finished callback that does not participate in UI.
     */
    public static ICommandFinishedCallback getDefaultCallback() {
        return getDefaultCallback((Project) null);
    }

    /**
     * The standard UI command finished callback. This will display a warning or
     * error dialog given the severity.
     *
     * @return A command finished callback that participates in UI.
     */
    public static ICommandFinishedCallback getDefaultCallback(final Project project) {
        /* Make sure to include the default non-UI callbacks */
        final ICommandFinishedCallback nonUiCallbacks = CommandFinishedCallbackFactory.getDefaultCallback();

        final ICommandFinishedCallback uiCallbacks =
            MultiCommandFinishedCallback.combine(getConsoleWriterCallback(), getErrorDialogCallback(project));

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

    public static ICommandFinishedCallback getErrorDialogCallback(final Project project) {
        return new ErrorDialogCommandFinishedCallback(project);
    }
}
