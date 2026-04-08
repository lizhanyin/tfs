// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.github.lizhanyin.tfs.client.ui.framework.helper;

import java.awt.image.BufferedImage;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.IconLoader;

import com.microsoft.tfs.util.Check;

/**
 * <p>
 * {@link ImageHelper} is a helper class that can be used by clients that manage
 * {@link Icon} resources. {@link ImageHelper} creates {@link Icon}s on demand,
 * caching them for subsequent use. Client code does not need to worry about the
 * storage of each individual {@link Icon} resource and does not need special
 * handling to ensure that each {@link Icon} is created only once, no matter how
 * often it is used.
 * </p>
 *
 * <p>
 * Clients that use {@link ImageHelper} typically instantiate an instance of
 * {@link ImageHelper} early in the life of the client class (the constructor is
 * a good place). During the lifetime of the client, it calls the various
 * methods on {@link ImageHelper} that return {@link Icon}s. At the end of the
 * lifetime of the client, the client <b>should</b> call the {@link #dispose()}
 * method of the {@link ImageHelper} to clear the cache.
 * </p>
 *
 * <p>
 * It is safe to call {@link #dispose()} multiple times. It is also safe to
 * re-use an {@link ImageHelper} after {@link #dispose()} has been called, as
 * long as there is an eventual ending {@link #dispose()} call.
 * </p>
 *
 * <p>
 * To obtain an {@link Icon}, specify a plugin ID and an image path relative to
 * that plugin to get an {@link Icon} for. Optionally, a default plugin ID can
 * be supplied when constructing an {@link ImageHelper}, and only the image path
 * need be specified when requesting {@link Icon}s.
 * </p>
 *
 * <p>
 * <b>Important</b>: The image paths passed to {@link ImageHelper} must not
 * include a leading "." or path separator. The path
 * "<code>icons/myimage.gif</code>" is valid, but "
 * <code>./icons/myimage.gif</code>" and "<code>/icons/myimage.gif</code>" are
 * not valid.
 * </p>
 */
public class ImageHelper {
    private static final Logger log = Logger.getInstance(ImageHelper.class);

    /**
     * An (optional) default plugin ID that can be set at construction time.
     */
    private final String defaultPluginId;

    /**
     * Caches {@link Icon}s that are created by this object. Maps from
     * {@link ImageDescriptorKey} to {@link Icon}. Must be synchronized on when
     * accessing.
     */
    private final Map<ImageDescriptorKey, Icon> cache = new HashMap<>();

    /**
     * Creates a new {@link ImageHelper} with no default plugin ID. When
     * requesting {@link Icon}s, the {@link #getImage(String)} convenience
     * method may not be used since it requires a default plugin ID to be set.
     */
    public ImageHelper() {
        this(null);
    }

    /**
     * Creates a new {@link ImageHelper} with the specified default plugin ID.
     * If the argument is not <code>null</code>, then {@link Icon}s can be
     * requested without having to specify a plugin ID with each request.
     *
     * @param defaultPluginId
     *        the default plugin ID to use when calling the
     *        {@link #getImage(String)} convenience method
     */
    public ImageHelper(final String defaultPluginId) {
        this.defaultPluginId = defaultPluginId;
    }

    /**
     * <p>
     * Obtains an {@link Icon} from this {@link ImageHelper}. If this
     * {@link ImageHelper} has not already created an {@link Icon}
     * corresponding to the specified image file path, a new {@link Icon} will
     * be created. Otherwise, a cached {@link Icon} will be returned. If no
     * image is found at the specified image file path, a warning will be logged
     * and a standard "missing image" {@link Icon} will be returned.
     * </p>
     *
     * <p>
     * This is a convenience method that does not specify a plugin ID to resolve
     * the image file path relative to. It should only be called if this
     * {@link ImageHelper} was created with a non-<code>null</code> default
     * plugin ID.
     * </p>
     *
     * @param imageFilePath
     *        the image file path (must not be <code>null</code>, also see
     *        comments on this class about specifying image file paths)
     * @return an {@link Icon} corresponding to the specified image file path
     *         (never <code>null</code>)
     */
    public Icon getImageDescriptor(final String imageFilePath) {
        if (defaultPluginId == null) {
            throw new IllegalStateException(
                "to use this method, you must construct an ImageHelper with a non-null default plugin ID"); //$NON-NLS-1$
        }

        return getImageDescriptor(defaultPluginId, imageFilePath);
    }

    /**
     * <p>
     * Obtains an {@link Icon} from this {@link ImageHelper}. If this
     * {@link ImageHelper} has not already created an {@link Icon}
     * corresponding to the specified plugin ID and image file path, a new
     * {@link Icon} will be created. Otherwise, a cached {@link Icon} will be
     * returned. If no image is found at the specified image file path, a
     * warning will be logged and a standard "missing image" {@link Icon} will
     * be returned.
     * </p>
     *
     * @param pluginId
     *        specifies the plugin to resolve the relative image path with (must
     *        not be <code>null</code>)
     * @param imageFilePath
     *        the image file path (must not be <code>null</code>, also see
     *        comments on this class about specifying image file paths)
     * @return an {@link Icon} corresponding to the specified image file path
     *         (never <code>null</code>)
     */
    public Icon getImageDescriptor(final String pluginId, final String imageFilePath) {
        Check.notNull(pluginId, "pluginId"); //$NON-NLS-1$
        Check.notNull(imageFilePath, "imageFilePath"); //$NON-NLS-1$

        final ImageDescriptorKey key = new ImageDescriptorKey(pluginId, imageFilePath);
        synchronized (cache) {
            Icon icon = cache.get(key);
            if (icon != null) {
                return icon;
            }

            icon = IconLoader.findIcon(imageFilePath, ImageHelper.class);

            if (icon == null) {
                final String messageFormat = "image missing: pluginId=[{0}] path=[{1}]"; //$NON-NLS-1$
                final String message = MessageFormat.format(messageFormat, pluginId, imageFilePath);
                log.warn(message);
                icon = new ImageIcon(new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB));
            } else {
                if (log.isTraceEnabled()) {
                    final String messageFormat = "loaded icon (pluginId=[{0}] path=[{1}]):{2}"; //$NON-NLS-1$
                    final String message = MessageFormat.format(messageFormat, pluginId, imageFilePath, icon);
                    log.trace(message);
                }
            }

            cache.put(key, icon);
            return icon;
        }
    }

    /**
     * <p>
     * Obtains an {@link Icon} from this {@link ImageHelper}. If this
     * {@link ImageHelper} has not already created an {@link Icon}
     * corresponding to the specified image file path, a new {@link Icon} will
     * be created. Otherwise, a cached {@link Icon} will be returned. If no
     * {@link Icon} can be found at the specified image file path, a warning
     * will be logged and a standard "missing image" {@link Icon} will be
     * returned.
     * </p>
     *
     * <p>
     * This is a convenience method that does not specify a plugin ID to resolve
     * the image file path relative to. It should only be called if this
     * {@link ImageHelper} was created with a non-<code>null</code> default
     * plugin ID.
     * </p>
     *
     * @param imageFilePath
     *        the image file path (must not be <code>null</code>, also see
     *        comments on this class about specifying image file paths)
     * @return an {@link Icon} corresponding to the specified image file path
     *         (never <code>null</code>)
     */
    public Icon getImage(final String imageFilePath) {
        if (defaultPluginId == null) {
            throw new IllegalStateException(
                "to use this method, you must construct an ImageHelper with a non-null default plugin ID"); //$NON-NLS-1$
        }

        return getImage(defaultPluginId, imageFilePath);
    }

    /**
     * <p>
     * Obtains an {@link Icon} from this {@link ImageHelper}. If this
     * {@link ImageHelper} has not already created an {@link Icon}
     * corresponding to the specified plugin ID and image file path, a new
     * {@link Icon} will be created. Otherwise, a cached {@link Icon} will be
     * returned. If no {@link Icon} can be found at the specified image file
     * path, a warning will be logged and a standard "missing image"
     * {@link Icon} will be returned.
     * </p>
     *
     * @param pluginId
     *        specifies the plugin to resolve the relative image path with (must
     *        not be <code>null</code>)
     * @param imageFilePath
     *        the image file path (must not be <code>null</code>, also see
     *        comments on this class about specifying image file paths)
     * @return an {@link Icon} corresponding to the specified image file path
     *         (never <code>null</code>)
     */
    public Icon getImage(final String pluginId, final String imageFilePath) {
        return getImageDescriptor(pluginId, imageFilePath);
    }

    /**
     * Obtains an {@link Icon} from this {@link ImageHelper}. Simply returns
     * the given {@link Icon} directly.
     *
     * @param icon
     *        an {@link Icon} to obtain (must not be <code>null</code>)
     * @return the given {@link Icon} (never <code>null</code>)
     */
    public Icon getImage(final Icon icon) {
        Check.notNull(icon, "icon"); //$NON-NLS-1$
        return icon;
    }

    /**
     * Clears the icon cache. This method is safe to call multiple times.
     */
    public void dispose() {
        synchronized (cache) {
            cache.clear();
        }
    }

    /**
     * A private class used as a key for cached {@link Icon}s.
     */
    private static class ImageDescriptorKey {
        private final String pluginId;
        private final String imageFilePath;

        public ImageDescriptorKey(final String pluginId, final String imageFilePath) {
            this.pluginId = pluginId;
            this.imageFilePath = imageFilePath;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + pluginId.hashCode();
            result = prime * result + imageFilePath.hashCode();
            return result;
        }

        @Override
        public boolean equals(final Object obj) {
            if (obj == this) {
                return true;
            }
            if (!(obj instanceof ImageDescriptorKey other)) {
                return false;
            }

            return pluginId.equals(other.pluginId) && imageFilePath.equals(other.imageFilePath);
        }
    }
}
