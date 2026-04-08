// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.github.lizhanyin.tfs.client.ui.framework.helper;

import java.awt.Color;
import java.awt.SystemColor;
import java.text.MessageFormat;

import com.github.lizhanyin.tfs.client.ui.framework.WindowSystem;
import com.intellij.openapi.diagnostic.Logger;

import com.intellij.ui.JBColor;
import com.microsoft.tfs.util.Check;

public class ColorUtils {
    private static final Logger log = Logger.getInstance(ColorUtils.class);

    /**
     * Returns a hexadecimal string ("html color") describing the given color.
     *
     * @param color
     *        The color to describe in hexadecimal notation (not null)
     * @return A hexadecimal string (including leading "#") that describes the
     *         color
     */
    public static String getColorString(final Color color) {
        Check.notNull(color, "color"); //$NON-NLS-1$

        final String red = Integer.toHexString(color.getRed());
        final String green = Integer.toHexString(color.getGreen());
        final String blue = Integer.toHexString(color.getBlue());

        final StringBuilder sb = new StringBuilder("#"); //$NON-NLS-1$

        if (red.length() == 1) {
            sb.append("0"); //$NON-NLS-1$
        }
        sb.append(red);

        if (green.length() == 1) {
            sb.append("0"); //$NON-NLS-1$
        }
        sb.append(green);

        if (blue.length() == 1) {
            sb.append("0"); //$NON-NLS-1$
        }
        sb.append(blue);

        return sb.toString();
    }

    /**
     * Convert hexadecimal string to Color.
     *
     * @param hexadecimal
     * @return
     */
    public static Color hexadecimalToRGB(final String hexadecimal) {
        try {
            return Color.decode(hexadecimal);
        } catch (final Exception e) {
            return JBColor.WHITE;
        }
    }

    /**
     * Convert hexadecimal to Color.
     *
     * @param hexadecimal
     * @return
     */
    public static Color hexadecimalToColor(final String hexadecimal) {
        return hexadecimalToRGB(hexadecimal);
    }

    /**
     * Returns a color created as the average of the two provided color values.
     *
     * @param one
     *        A color to build the average from (not <code>null</code>)
     * @param two
     *        A color to build the average from (not <code>null</code>)
     * @return The average color created from the inputs
     */
    public static Color getAverageColor(final Color one, final Color two) {
        Check.notNull(one, "one"); //$NON-NLS-1$
        Check.notNull(two, "two"); //$NON-NLS-1$

        return new JBColor(
            new Color(
                (one.getRed() + two.getRed()) / 2,
                (one.getGreen() + two.getGreen()) / 2,
                (one.getBlue() + two.getBlue()) / 2),
            new Color(
                (one.getRed() + two.getRed()) / 2,
                (one.getGreen() + two.getGreen()) / 2,
                (one.getBlue() + two.getBlue()) / 2));
    }

    /* WIN32 Specific Color Handling */

    /**
     * Gets the windows system color id identified by name.
     *
     * @param colorName
     *        The name of the color to resolve.
     * @throws IllegalArgumentException
     *         if the current platform is not win32
     * @return The color id identified by this name, or -1 if it could not be
     *         looked up.
     */
    public static int getWin32SystemColorID(final String colorName) {
        Check.notNull(colorName, "colorName"); //$NON-NLS-1$
        Check.isTrue(WindowSystem.isCurrentWindowSystem(WindowSystem.WIN32), "WindowSystem.WIN32"); //$NON-NLS-1$

        return switch (colorName) {
            case "COLOR_SCROLLBAR" -> 0; //$NON-NLS-1$
            case "COLOR_BACKGROUND" -> 1; //$NON-NLS-1$
            case "COLOR_ACTIVECAPTION" -> 2; //$NON-NLS-1$
            case "COLOR_INACTIVECAPTION" -> 3; //$NON-NLS-1$
            case "COLOR_MENU" -> 4; //$NON-NLS-1$
            case "COLOR_WINDOW" -> 5; //$NON-NLS-1$
            case "COLOR_WINDOWFRAME" -> 6; //$NON-NLS-1$
            case "COLOR_MENUTEXT" -> 7; //$NON-NLS-1$
            case "COLOR_WINDOWTEXT" -> 8; //$NON-NLS-1$
            case "COLOR_CAPTIONTEXT" -> 9; //$NON-NLS-1$
            case "COLOR_ACTIVEBORDER" -> 10; //$NON-NLS-1$
            case "COLOR_INACTIVEBORDER" -> 11; //$NON-NLS-1$
            case "COLOR_APPWORKSPACE" -> 12; //$NON-NLS-1$
            case "COLOR_HIGHLIGHT" -> 13; //$NON-NLS-1$
            case "COLOR_HIGHLIGHTTEXT" -> 14; //$NON-NLS-1$
            case "COLOR_BTNFACE" -> 15; //$NON-NLS-1$
            case "COLOR_BTNSHADOW" -> 16; //$NON-NLS-1$
            case "COLOR_GRAYTEXT" -> 17; //$NON-NLS-1$
            case "COLOR_BTNTEXT" -> 18; //$NON-NLS-1$
            case "COLOR_INACTIVECAPTIONTEXT" -> 19; //$NON-NLS-1$
            case "COLOR_BTNHIGHLIGHT" -> 20; //$NON-NLS-1$
            case "COLOR_3DDKSHADOW" -> 21; //$NON-NLS-1$
            case "COLOR_3DLIGHT" -> 22; //$NON-NLS-1$
            case "COLOR_INFOTEXT" -> 23; //$NON-NLS-1$
            case "COLOR_INFOBK" -> 24; //$NON-NLS-1$
            case "COLOR_HOTLIGHT" -> 26; //$NON-NLS-1$
            case "COLOR_GRADIENTACTIVECAPTION" -> 27; //$NON-NLS-1$
            case "COLOR_GRADIENTINACTIVECAPTION" -> 28; //$NON-NLS-1$
            default -> {
                log.warn(MessageFormat.format("Could not resolve win32 color constant {0}", colorName)); //$NON-NLS-1$
                yield -1;
            }
        };
    }

    /**
     * Gets the windows system color identified by name.
     * <p>
     * This color MUST NOT be disposed.
     *
     * @param colorName
     *        The name of the color to resolve.
     * @throws IllegalArgumentException
     *         if the current platform is not win32
     * @return The color identified by this name, or null if it could not be
     *         looked up.
     */
    public static Color getWin32SystemColor(final String colorName) {
        final int colorId = getWin32SystemColorID(colorName);

        if (colorId >= 0) {
            return getWin32SystemColor(colorId);
        }

        return null;
    }

    /**
     * Gets the windows system color identified by id.
     * <p>
     * This color MUST NOT be disposed.
     *
     * @param colorId
     *        The id of the color to be resolved.
     * @throws IllegalArgumentException
     *         if the current platform is not win32
     * @return The color identified by this id, or null if it could not be
     *         looked up.
     */
    public static Color getWin32SystemColor(final int colorId) {
        Check.isTrue(WindowSystem.isCurrentWindowSystem(WindowSystem.WIN32), "WindowSystem.WIN32"); //$NON-NLS-1$

        return switch (colorId) {
            case 0 -> SystemColor.scrollbar;
            case 1 -> SystemColor.desktop;
            case 2 -> SystemColor.activeCaption;
            case 3 -> SystemColor.inactiveCaption;
            case 4 -> SystemColor.menu;
            case 5 -> SystemColor.window;
            case 6 -> SystemColor.windowBorder;
            case 7 -> SystemColor.menuText;
            case 8 -> SystemColor.windowText;
            case 9 -> SystemColor.activeCaptionText;
            case 10 -> SystemColor.activeCaptionBorder;
            case 11 -> SystemColor.inactiveCaptionBorder;
            case 13 -> SystemColor.textHighlight;
            case 14 -> SystemColor.textHighlightText;
            case 15 -> SystemColor.control;
            case 16 -> SystemColor.controlShadow;
            case 17 -> SystemColor.textInactiveText;
            case 18 -> SystemColor.controlText;
            case 19 -> SystemColor.inactiveCaptionText;
            case 20 -> SystemColor.controlLtHighlight;
            case 21 -> SystemColor.controlDkShadow;
            case 22 -> SystemColor.controlHighlight;
            case 23 -> SystemColor.infoText;
            case 24 -> SystemColor.info;
            default -> {
                log.warn(MessageFormat.format("Could not resolve win32 color id {0}", Integer.toString(colorId))); //$NON-NLS-1$
                yield null;
            }
        };
    }
}
