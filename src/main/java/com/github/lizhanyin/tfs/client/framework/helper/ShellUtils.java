// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.github.lizhanyin.tfs.client.framework.helper;

import java.awt.Component;
import java.awt.Window;

import javax.swing.SwingUtilities;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.wm.WindowManager;

import com.microsoft.tfs.util.Check;

/**
 * {@link ShellUtils} contains static utility methods for working with
 * Swing/IntelliJ windows and dialogs.
 */
public class ShellUtils {
    private static final Logger log = Logger.getInstance(ShellUtils.class);

    /**
     * <p>
     * Tests whether the given {@link Window} is modal.
     * </p>
     *
     * @param window
     *        a {@link Window} to test for modality (must not be
     *        <code>null</code>)
     * @return <code>true</code> if the given {@link Window} is modal
     */
    public static boolean isModal(final Window window) {
        Check.notNull(window, "window"); //$NON-NLS-1$
        if (window instanceof java.awt.Dialog) {
            return ((java.awt.Dialog) window).isModal();
        }
        return false;
    }

    /**
     * <p>
     * Tests whether a {@link Window} is a child of another {@link Window}. The
     * test is actually a descendant test, since this method will return
     * <code>true</code> for both direct children and descendants.
     * </p>
     *
     * @param window
     *        the potential child {@link Window} (must not be <code>null</code>)
     * @param parent
     *        the potential parent {@link Window} (must not be <code>null</code>)
     * @return <code>true</code> if there is a parent-child relationship
     */
    public static boolean isChild(final Window window, final Window parent) {
        Check.notNull(window, "window"); //$NON-NLS-1$
        Check.notNull(parent, "parent"); //$NON-NLS-1$

        Window testWindow = window;
        while (testWindow != null && testWindow != parent) {
            testWindow = testWindow.getOwner();
        }

        return testWindow == parent;
    }

    /**
     * <p>
     * Finds the first modal {@link Window}. If there are no modal
     * {@link Window}s, returns <code>null</code>.
     * </p>
     *
     * @return the first modal {@link Window} or <code>null</code> if there are
     *         no modal {@link Window}s
     */
    public static Window getFirstModalWindow() {
        final Window[] windows = Window.getWindows();
        for (final Window window : windows) {
            if (isModal(window)) {
                return window;
            }
        }

        return null;
    }

    /**
     * <p>
     * Finds a {@link Window} that is a modal blocker of the given {@link Window}.
     * If such a {@link Window} exists and the given {@link Window} is modal, then
     * opening the given {@link Window} would result in multiple modal
     * {@link Window}s that do not have a parent-child relationship. This
     * condition should be avoided since it can result in a hung UI.
     * </p>
     *
     * @param windowToOpen
     *        a {@link Window} that is being considered for opening (must not be
     *        <code>null</code>)
     * @return a modal blocker {@link Window} (described above) or
     *         <code>null</code> if there is no modal blocker {@link Window}
     */
    public static Window getModalBlockingWindow(final Window windowToOpen) {
        Check.notNull(windowToOpen, "windowToOpen"); //$NON-NLS-1$

        final Window[] windows = Window.getWindows();

        for (final Window window : windows) {
            if (isModal(window)) {
                if (window == windowToOpen) {
                    continue;
                }
                if (!window.isVisible()) {
                    continue;
                }

                /*
                 * If the queried window is a child of the window about to be
                 * opened, consider this a blocking window.
                 */
                if (isChild(window, windowToOpen)) {
                    return window;
                }
                if (isChild(windowToOpen, window)) {
                    continue;
                }
                return window;
            }
        }

        return null;
    }

    /**
     * <p>
     * Attempts to find the best {@link Window} to use as the parent of a modal
     * dialog. You must pass in a {@link Window} that would be used as a parent
     * in the absence of this method. If no better parent can be found, the
     * given {@link Window} will be returned as the parent. If there are existing
     * modal {@link Window}s, this method will return a parent that is possibly
     * safer to use than the given {@link Window}. In particular, using this
     * method will avoid having multiple modal {@link Window}s that do not have a
     * parent-child relationship.
     * </p>
     *
     * <p>
     * In order to make this method applicable in as many situations as
     * possible, you are allowed to pass <code>null</code> as the default parent
     * {@link Window}. In this case, this method will return <code>null</code>.
     * </p>
     *
     * <p>
     * In order for this method to be useful, it must be called from the UI
     * thread for the given {@link Window}. However, if it is not, it will not
     * throw an exception. In this case it will simply return the given
     * {@link Window}.
     * </p>
     *
     * @param defaultParent
     *        the default parent {@link Window}, or <code>null</code>
     * @return the best parent {@link Window} to use, or <code>null</code> if
     *         <code>null</code> was passed as the argument
     */
    public static Window getBestParent(final Window defaultParent) {
        /*
         * Wrap the entire method in a try block. Errors in this method should
         * never bubble up to callers (but we will log them).
         */
        try {
            if (defaultParent == null) {
                return null;
            }

            if (!SwingUtilities.isEventDispatchThread()) {
                return defaultParent;
            }

            final Window[] children = defaultParent.getOwnedWindows();

            for (final Window child : children) {
                if (isModal(child)) {
                    return getBestParent(child);
                }
            }

            return defaultParent;
        } catch (final Exception ex) {
            log.error("error in getBestParent", ex); //$NON-NLS-1$
            return defaultParent;
        }
    }

    /**
     * Returns the window which contains the given component.
     *
     * @param component
     *        The component to test (not null)
     * @return The window which contains the given component, or null if it is not
     *         contained in a Window.
     */
    public static Window getParentWindow(final Component component) {
        Check.notNull(component, "component"); //$NON-NLS-1$
        return SwingUtilities.getWindowAncestor(component);
    }

    /**
     * Returns the active project window.
     *
     * @return the active project window, or null if no project is open
     */
    public static Window getActiveProjectWindow() {
        final Project[] projects = ProjectManager.getInstance().getOpenProjects();
        if (projects.length == 0) {
            return null;
        }

        for (final Project project : projects) {
            final Window window = WindowManager.getInstance().getFrame(project);
            if (window != null && window.isActive()) {
                return window;
            }
        }

        // Return the first project window if no active window found
        return WindowManager.getInstance().getFrame(projects[0]);
    }

    /**
     * Returns the window for the given project.
     *
     * @param project
     *        the project to get the window for
     * @return the project window, or null if the project is null
     */
    public static Window getProjectWindow(final Project project) {
        if (project == null) {
            return null;
        }
        return WindowManager.getInstance().getFrame(project);
    }
}
