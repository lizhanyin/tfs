// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.github.lizhanyin.tfs.client.ui.framework.helper;

import java.awt.Color;
import java.awt.Component;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import com.github.lizhanyin.tfs.TFSClientPlugin;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.ui.ImageUtil;

import com.microsoft.tfs.util.Check;

public class ImageUtils {
    private static final Logger log = Logger.getInstance(ImageUtils.class);

    private ImageUtils() {
    }

    /**
     * Creates a "disclosure" or "drop-down" triangle suitable for the given
     * Component's font size and colors.
     *
     * @param component
     *        The component to use as the basis for creating a triangle
     * @return An {@link Icon} representing a triangle
     */
    public static Icon createDisclosureTriangle(final Component component) {
        Check.notNull(component, "component"); //$NON-NLS-1$

        try {
            final FontMetrics fm = component.getFontMetrics(component.getFont());
            final int arrowHeight = fm.getHeight();
            final int arrowWidth = arrowHeight / 2;

            final BufferedImage image = ImageUtil.createImage(arrowWidth, arrowHeight, BufferedImage.TYPE_INT_ARGB);
            final Graphics2D g = image.createGraphics();

            try {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(component.getForeground());
                g.fillPolygon(new int[] {
                    0,
                    arrowWidth,
                    arrowWidth / 2,
                }, new int[] {
                    arrowHeight / 2,
                    arrowHeight / 2,
                    (int) ((((double) arrowHeight / 2) + ((double) arrowWidth / 2))),
                }, 3);
            } finally {
                g.dispose();
            }

            return new ImageIcon(image);
        } catch (final Exception e) {
            log.warn("Could not create drop hyperlink image", e); //$NON-NLS-1$

            /*
             * Note: do not dispose this ImageHelper. Callers should dispose
             * the image manually.
             */
            return new ImageHelper(TFSClientPlugin.PLUGIN_ID).getImage("/images/common/drop_arrow.png"); //$NON-NLS-1$
        }
    }

    public static Icon createRectangular(final Color color, final int width, final int height) {
        Check.notNull(color, "color"); //$NON-NLS-1$

        final BufferedImage image = ImageUtil.createImage(width, height, BufferedImage.TYPE_INT_ARGB);
        final Graphics2D g = image.createGraphics();

        try {
            g.setColor(color);
            g.fillRect(0, 0, width, height);
        } finally {
            g.dispose();
        }

        return new ImageIcon(image);
    }

    public static Icon grayScaleImage(final Icon icon, final int width, final int height) {
        // Convert Icon to BufferedImage
        final BufferedImage original = ImageUtil.createImage(
            icon.getIconWidth(),
            icon.getIconHeight(),
            BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = original.createGraphics();
        try {
            icon.paintIcon(null, g, 0, 0);
        } finally {
            g.dispose();
        }

        // Scale
        final BufferedImage scaled = ImageUtil.createImage(width, height, BufferedImage.TYPE_INT_ARGB);
        g = scaled.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(original, 0, 0, width, height, null);
        } finally {
            g.dispose();
        }

        // Convert to grayscale
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                final int argb = scaled.getRGB(x, y);
                final int a = (argb >> 24) & 0xff;
                final int r = (argb >> 16) & 0xff;
                final int green = (argb >> 8) & 0xff;
                final int b = argb & 0xff;
                final int gray = (int) (0.299 * r + 0.587 * green + 0.114 * b);
                scaled.setRGB(x, y, (a << 24) | (gray << 16) | (gray << 8) | gray);
            }
        }

        return new ImageIcon(scaled);
    }
}
