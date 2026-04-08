// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.github.lizhanyin.tfs.client.ui.helpers;

import java.awt.Color;

import com.github.lizhanyin.tfs.client.ui.framework.WindowSystem;
import com.github.lizhanyin.tfs.client.ui.framework.helper.ColorUtils;

public class SystemColor {
    private static final Object lock = new Object();

    private static Color dimmedWidgetForegroundColor;

    /**
     * Returns a color to use as a "dimmed" widget foreground color (created as
     * the average between the foreground and background widget colors.)
     *
     * @return A "dimmed" widget foreground color
     */
    public static Color getDimmedWidgetForegroundColor() {
        synchronized (lock) {
            if (dimmedWidgetForegroundColor == null) {
                /*
                 * On Win32, we can call GetSysColor(COLOR_GRAYTEXT), which
                 * respects the current theme settings (particularly important
                 * for high contrast mode.)
                 */
                if (WindowSystem.isCurrentWindowSystem(WindowSystem.WIN32)) {
                    dimmedWidgetForegroundColor = ColorUtils.getWin32SystemColor("COLOR_GRAYTEXT"); //$NON-NLS-1$
                }

                /*
                 * On non-win32 platforms, simply select the average of the
                 * foreground and background colors (typically a gray.)
                 */
                if (dimmedWidgetForegroundColor == null) {
                    dimmedWidgetForegroundColor = ColorUtils.getAverageColor(
                        java.awt.SystemColor.control,
                        java.awt.SystemColor.controlText);
                }
            }

            return dimmedWidgetForegroundColor;
        }
    }
}
