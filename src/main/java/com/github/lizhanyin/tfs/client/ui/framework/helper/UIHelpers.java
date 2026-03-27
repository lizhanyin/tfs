// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.github.lizhanyin.tfs.client.ui.framework.helper;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * UI helper utilities for IntelliJ IDEA platform.
 * Migrated from Eclipse SWT version to Swing/IDEA platform.
 */
public class UIHelpers {

    /**
     * Gets the default text font for the platform.
     * In IDEA, this returns the editor font.
     *
     * @return the text font
     */
    @NotNull
    public static Font getTextFont() {
        return UIUtil.getFont(UIUtil.FontSize.NORMAL, null);
    }

    /**
     * Copies text to the system clipboard.
     *
     * @param text the text to copy
     */
    public static void copyToClipboard(@Nullable final String text) {
        if (text == null) {
            return;
        }
        final Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        final StringSelection selection = new StringSelection(text);
        clipboard.setContents(selection, null);
    }

    /**
     * Checks if a specific tab is selected in a JTabbedPane.
     *
     * @param tabbedPane the tabbed pane
     * @param tabIndex   the tab index to check
     * @return true if the tab is selected
     */
    public static boolean isTabSelected(@NotNull final JTabbedPane tabbedPane, final int tabIndex) {
        return tabbedPane.getSelectedIndex() == tabIndex;
    }

    /**
     * Converts an array to a string representation.
     *
     * @param array the array to convert
     * @return string representation of the array
     */
    @NotNull
    public static String arrayToString(@Nullable final Object[] array) {
        if (array == null) {
            return "null"; //$NON-NLS-1$
        }

        final StringBuilder sb = new StringBuilder("["); //$NON-NLS-1$

        for (int i = 0; i < array.length; i++) {
            if (array[i] == null) {
                sb.append("null"); //$NON-NLS-1$
            } else {
                sb.append(array[i].toString());
            }
            if (i < (array.length - 1)) {
                sb.append(", "); //$NON-NLS-1$
            }
        }

        sb.append("]"); //$NON-NLS-1$

        return sb.toString();
    }

    /**
     * Sets a container and all its children enabled or disabled.
     *
     * @param container the container to modify
     * @param enabled   whether to enable or disable
     */
    public static void setContainerEnabled(@NotNull final Container container, final boolean enabled) {
        final Component[] children = container.getComponents();
        for (final Component child : children) {
            child.setEnabled(enabled);
            if (child instanceof Container && child.isVisible()) {
                setContainerEnabled((Container) child, enabled);
            }
        }
    }

    /**
     * Checks if the current thread is the Event Dispatch Thread (EDT).
     *
     * @return true if running on EDT
     */
    public static boolean isUIThread() {
        return EventQueue.isDispatchThread();
    }

    /**
     * Executes a runnable synchronously on the UI thread (EDT).
     * If already on the UI thread, the runnable is executed immediately.
     *
     * @param runnable the runnable to execute
     */
    public static void syncExec(@NotNull final Runnable runnable) {
        runOnUIThread(false, runnable);
    }

    /**
     * Executes a runnable asynchronously on the UI thread (EDT).
     *
     * @param runnable the runnable to execute
     */
    public static void asyncExec(@NotNull final Runnable runnable) {
        runOnUIThread(true, runnable);
    }

    /**
     * Runs the given runnable on the UI thread (EDT).
     *
     * @param async    true to run asynchronously (recommended)
     * @param runnable the Runnable to run
     */
    public static void runOnUIThread(final boolean async, @NotNull final Runnable runnable) {
        runOnUIThread(async, runnable, ModalityState.defaultModalityState());
    }

    /**
     * Runs the given runnable on the UI thread (EDT) with a specific modality state.
     *
     * @param async         true to run asynchronously (recommended)
     * @param runnable      the Runnable to run
     * @param modalityState the modality state to use
     */
    public static void runOnUIThread(final boolean async, @NotNull final Runnable runnable,
                                     @NotNull final ModalityState modalityState) {
        final Application application = ApplicationManager.getApplication();

        if (async) {
            application.invokeLater(runnable, modalityState);
        } else {
            if (EventQueue.isDispatchThread()) {
                runnable.run();
            } else {
                application.invokeAndWait(runnable, modalityState);
            }
        }
    }

    /**
     * Runs the given runnable on the UI thread (EDT) associated with a specific component.
     *
     * @param component the component whose UI thread to use
     * @param async     true to run asynchronously (recommended)
     * @param runnable  the Runnable to run
     */
    public static void runOnUIThread(@Nullable final Component component, final boolean async,
                                     @NotNull final Runnable runnable) {
        runOnUIThread(async, runnable);
    }

    /**
     * Opens a dialog on the UI thread and returns the result code.
     *
     * @param dialog the dialog wrapper to open
     * @return the dialog exit code
     */
    public static int openOnUIThread(@NotNull final DialogWrapper dialog) {
        final AtomicInteger dialogStatus = new AtomicInteger();

        runOnUIThread(false, () -> {
            dialog.show();
            dialogStatus.set(dialog.getExitCode());
        });

        return dialogStatus.get();
    }

    /**
     * Executes a runnable in a read action on the UI thread.
     *
     * @param runnable the runnable to execute
     */
    public static void runReadActionOnUIThread(@NotNull final Runnable runnable) {
        final Application application = ApplicationManager.getApplication();
        application.invokeLater(() -> application.runReadAction(runnable));
    }

    /**
     * Executes a runnable in a write action on the UI thread.
     *
     * @param runnable the runnable to execute
     */
    public static void runWriteActionOnUIThread(@NotNull final Runnable runnable) {
        final Application application = ApplicationManager.getApplication();
        application.invokeLater(() -> application.runWriteAction(runnable));
    }

    /**
     * Gets the parent window of a component.
     *
     * @param component the component
     * @return the parent window, or null if not found
     */
    @Nullable
    public static Window getWindow(@Nullable final Component component) {
        if (component == null) {
            return null;
        }
        return SwingUtilities.getWindowAncestor(component);
    }

    /**
     * Gets the current active window.
     *
     * @return the current active window, or null if none
     */
    @Nullable
    public static Window getActiveWindow() {
        final Window[] windows = Window.getWindows();
        for (final Window window : windows) {
            if (window.isActive() && window.isShowing()) {
                return window;
            }
        }
        return null;
    }
}
