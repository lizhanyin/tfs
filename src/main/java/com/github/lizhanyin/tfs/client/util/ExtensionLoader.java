// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.github.lizhanyin.tfs.client.util;

import java.text.MessageFormat;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Utilities to simplify finding and loading executable content from other
 * plug-ins via extension points.
 *
 * In IntelliJ IDEA, extension points are defined in plugin.xml and accessed
 * via {@link ExtensionPointName}.
 *
 * @threadsafety unknown
 */
public abstract class ExtensionLoader {
    private static final Logger log = Logger.getInstance(ExtensionLoader.class);

    private ExtensionLoader() {
    }

    /**
     * @equivalence loadSingleExtensionClass(extensionPointID, true);
     */
    @Nullable
    public static Object loadSingleExtensionClass(@NotNull final String extensionPointID) {
        return loadSingleExtensionClass(extensionPointID, true);
    }

    /**
     * Finds exactly one contribution for the specified extension point ID and
     * instantiates an object for the <code>class</code> element in that
     * contribution.
     *
     * In IntelliJ IDEA, this uses {@link ExtensionPointName} to find extensions.
     *
     * @param extensionPointID
     *        the extension point ID to load an extension for (must not be
     *        <code>null</code>)
     * @param throwForZeroElements
     *        if <code>true</code> and no elements that contribute to the
     *        specified ID were found, an exception is thrown; if false no error
     *        is thrown if no elements are found and <code>null</code> is
     *        returned
     * @return the instance of the type the extension specifies, or <code>null</code> if
     *         throwForZeroElements is false and no elements were found
     * @throws RuntimeException
     *         if the extension could not be loaded (0 or > 1 contributions
     *         available, or class instantiation error)
     */
    @Nullable
    public static Object loadSingleExtensionClass(@NotNull final String extensionPointID, final boolean throwForZeroElements) {
        try {
            final ExtensionPointName<Object> extensionPoint = ExtensionPointName.create(extensionPointID);
            final Object[] extensions = extensionPoint.getExtensions();

            if (extensions.length == 0) {
                if (!throwForZeroElements) {
                    return null;
                }
                throw new RuntimeException(
                    MessageFormat.format("No provider is configured for extension point id {0}", extensionPointID)); //$NON-NLS-1$
            }

            if (extensions.length > 1) {
                log.warn(
                    MessageFormat.format("Multiple providers are configured for extension point id {0}, using the first one", extensionPointID)); //$NON-NLS-1$
            }

            return extensions[0];
        } catch (final Exception e) {
            final String message =
                MessageFormat.format("Could not load provider for extension point id {0}", extensionPointID); //$NON-NLS-1$

            if (throwForZeroElements) {
                throw new RuntimeException(message, e);
            }

            log.debug(message, e);
            return null;
        }
    }

    /**
     * Loads all extensions for the specified extension point ID.
     *
     * @param extensionPointID
     *        the extension point ID to load extensions for (must not be
     *        <code>null</code>)
     * @return an array of extension instances (never <code>null</code>)
     */
    @NotNull
    public static Object[] loadExtensionClasses(@NotNull final String extensionPointID) {
        try {
            final ExtensionPointName<Object> extensionPoint = ExtensionPointName.create(extensionPointID);
            return extensionPoint.getExtensions();
        } catch (final Exception e) {
            log.debug(
                MessageFormat.format("Could not load providers for extension point id {0}", extensionPointID), e); //$NON-NLS-1$
            return new Object[0];
        }
    }
}
