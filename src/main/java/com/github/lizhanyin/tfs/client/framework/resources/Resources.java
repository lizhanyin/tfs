// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.github.lizhanyin.tfs.client.framework.resources;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;

import com.microsoft.tfs.util.Check;

/**
 * {@link Resources} is a helper class containing static utility methods for
 * working with {@link VirtualFile} objects.
 */
public class Resources {
    private static final Logger log = Logger.getInstance(Resources.class);

    /**
     * Resource type enum.
     */
    public enum ResourceType {
        FILE,
        CONTAINER,
        ANY
    }

    /**
     * Policy for handling resources without a location.
     */
    public enum LocationUnavailablePolicy {
        THROW,
        IGNORE_RESOURCE
    }

    /**
     * <p>
     * Obtains the absolute paths of an array of resources.
     * </p>
     *
     * @param resources
     *        the input array of {@link VirtualFile}s - must not be
     *        <code>null</code>, and must not contain any <code>null</code>
     *        elements
     * @param locationUnavailablePolicy
     *        a {@link LocationUnavailablePolicy} specifying what to do if one
     *        of the input {@link VirtualFile}s does not have a path (must not
     *        be <code>null</code>)
     * @return an array of {@link String} paths as described above
     */
    public static String[] getLocations(
        final VirtualFile[] resources,
        final LocationUnavailablePolicy locationUnavailablePolicy) {
        Check.notNull(resources, "resources"); //$NON-NLS-1$
        Check.notNull(locationUnavailablePolicy, "locationUnavailablePolicy"); //$NON-NLS-1$

        final List<String> locations = new ArrayList<>();

        for (int i = 0; i < resources.length; i++) {
            if (resources[i] == null) {
                throw new IllegalArgumentException("element " + i + " in the passed VirtualFile array was null"); //$NON-NLS-1$ //$NON-NLS-2$
            }

            final String location = getLocation(resources[i], locationUnavailablePolicy);

            if (location != null) {
                locations.add(location);
            }
        }

        return locations.toArray(new String[0]);
    }

    /**
     * <p>
     * Obtains the absolute path of a resource.
     * </p>
     *
     * @param resource
     *        the input {@link VirtualFile} - must not be <code>null</code>
     * @param locationUnavailablePolicy
     *        a {@link LocationUnavailablePolicy} specifying what to do if the
     *        input {@link VirtualFile} does not have a path (must not be
     *        <code>null</code>)
     * @return the {@link String} path as described above
     */
    public static String getLocation(
        final VirtualFile resource,
        final LocationUnavailablePolicy locationUnavailablePolicy) {
        Check.notNull(resource, "resource"); //$NON-NLS-1$
        Check.notNull(locationUnavailablePolicy, "locationUnavailablePolicy"); //$NON-NLS-1$

        final String locationPath = resource.getPath();

        if (locationPath.isEmpty()) {
            if (LocationUnavailablePolicy.THROW == locationUnavailablePolicy) {
                final String messageFormat = "the resource [{0}] does not have a location"; //$NON-NLS-1$
                final String message = MessageFormat.format(messageFormat, resource);
                throw new RuntimeException(message);
            }
            return null;
        }

        return locationPath;
    }

    /**
     * <p>
     * Given an absolute path on the local file system, attempts to find a
     * file resource that corresponds to that path.
     * </p>
     *
     * @param location
     *        an absolute path on the local file system (must not be
     *        <code>null</code>)
     * @return a {@link VirtualFile} that corresponds to the specified file system
     *         path, or <code>null</code>
     */
    @Nullable
    public static VirtualFile getFileForLocation(final String location) {
        return getFileForLocation(location, true);
    }

    /**
     * <p>
     * Given an absolute path on the local file system, attempts to find a
     * file resource that corresponds to that path.
     * </p>
     *
     * @param location
     *        an absolute path on the local file system (must not be
     *        <code>null</code>)
     * @param mustExist
     *        if <code>true</code>, this method will only return an existing
     *        resource; if <code>false</code>, the returned resource may not
     *        actually exist
     * @return a {@link VirtualFile} that corresponds to the specified file system
     *         path, or <code>null</code>
     */
    @Nullable
    public static VirtualFile getFileForLocation(final String location, final boolean mustExist) {
        return getResourceForLocation(location, ResourceType.FILE, mustExist);
    }

    /**
     * <p>
     * Given an absolute path on the local file system, attempts to find a
     * container (directory) resource that corresponds to that path.
     * </p>
     *
     * @param location
     *        an absolute path on the local file system (must not be
     *        <code>null</code>)
     * @return a {@link VirtualFile} that corresponds to the specified file system
     *         path, or <code>null</code>
     */
    @Nullable
    public static VirtualFile getContainerForLocation(final String location) {
        return getContainerForLocation(location, true);
    }

    /**
     * <p>
     * Given an absolute path on the local file system, attempts to find a
     * container (directory) resource that corresponds to that path.
     * </p>
     *
     * @param location
     *        an absolute path on the local file system (must not be
     *        <code>null</code>)
     * @param mustExist
     *        if <code>true</code>, this method will only return an existing
     *        resource; if <code>false</code>, the returned resource may not
     *        actually exist
     * @return a {@link VirtualFile} that corresponds to the specified file system
     *         path, or <code>null</code>
     */
    @Nullable
    public static VirtualFile getContainerForLocation(final String location, final boolean mustExist) {
        return getResourceForLocation(location, ResourceType.CONTAINER, mustExist);
    }

    /**
     * <p>
     * Given an absolute path on the local file system, attempts to find a
     * resource that corresponds to that path.
     * </p>
     *
     * @param location
     *        an absolute path on the local file system (must not be
     *        <code>null</code>)
     * @return a {@link VirtualFile} that corresponds to the specified file system
     *         path, or <code>null</code>
     */
    @Nullable
    public static VirtualFile getResourceForLocation(final String location) {
        return getResourceForLocation(location, ResourceType.ANY, true);
    }

    /**
     * <p>
     * Given an absolute path on the local file system, attempts to find a
     * resource that corresponds to that path.
     * </p>
     *
     * @param location
     *        an absolute path on the local file system (must not be
     *        <code>null</code>)
     * @param resourceType
     *        determines what type of resource to look for (must not be
     *        <code>null</code>)
     * @return a {@link VirtualFile} that corresponds to the specified file system
     *         path, or <code>null</code>
     */
    @Nullable
    public static VirtualFile getResourceForLocation(final String location, final ResourceType resourceType) {
        return getResourceForLocation(location, resourceType, true);
    }

    /**
     * <p>
     * Given an absolute path on the local file system, attempts to find a
     * resource that corresponds to that path.
     * </p>
     *
     * @param location
     *        an absolute path on the local file system (must not be
     *        <code>null</code>)
     * @param resourceType
     *        determines what type of resource to look for (must not be
     *        <code>null</code>)
     * @param mustExist
     *        if <code>true</code>, this method will only return an existing
     *        resource; if <code>false</code>, the returned resource may not
     *        actually exist
     * @return a {@link VirtualFile} that corresponds to the specified file system
     *         path, or <code>null</code>
     */
    @Nullable
    public static VirtualFile getResourceForLocation(
        final String location,
        final ResourceType resourceType,
        final boolean mustExist) {
        Check.notNull(location, "location"); //$NON-NLS-1$
        Check.notNull(resourceType, "resourceType"); //$NON-NLS-1$

        final VirtualFile vf = LocalFileSystem.getInstance().findFileByPath(location);

        if (vf == null) {
            return null;
        }

        if (mustExist && !vf.exists()) {
            return null;
        }

        return switch (resourceType) {
            case FILE -> vf.isDirectory() ? null : vf;
            case CONTAINER -> vf.isDirectory() ? vf : null;
            default -> vf;
        };
    }

    /**
     * <p>
     * Given an absolute path on the local file system, attempts to find all
     * resources that correspond to that path.
     * </p>
     *
     * <p>
     * In IntelliJ's VFS, each path maps to at most one {@link VirtualFile},
     * so this method returns an array of at most one element.
     * </p>
     *
     * @param location
     *        an absolute path on the local file system (must not be
     *        <code>null</code>)
     * @param resourceType
     *        determines what type of resource to look for (must not be
     *        <code>null</code>)
     * @param mustExist
     *        if <code>true</code>, this method will only return an existing
     *        resource
     * @return all {@link VirtualFile}s that correspond to the specified file
     *         system path, or an empty array
     */
    @NotNull
    public static VirtualFile[] getAllResourcesForLocation(
        final String location,
        final ResourceType resourceType,
        final boolean mustExist) {
        final VirtualFile vf = getResourceForLocation(location, resourceType, mustExist);

        if (vf != null) {
            return new VirtualFile[] { vf };
        }

        return new VirtualFile[0];
    }

    /**
     * Selects a subset of the input array of {@link VirtualFile}s using a
     * filter predicate.
     *
     * @param resources
     *        the input array of {@link VirtualFile}s - must not be
     *        <code>null</code>, and must not contain any <code>null</code>
     *        elements
     * @param filter
     *        a {@link Predicate} used to select the returned resources
     *        (must not be <code>null</code>)
     * @return an array of {@link VirtualFile}s which is never <code>null</code>
     *         and is a subset of the resources passed to this method
     */
    public static VirtualFile[] filter(final VirtualFile[] resources, final Predicate<VirtualFile> filter) {
        Check.notNull(resources, "resources"); //$NON-NLS-1$
        Check.notNull(filter, "filter"); //$NON-NLS-1$

        final List<VirtualFile> results = new ArrayList<>();

        for (int i = 0; i < resources.length; i++) {
            if (resources[i] == null) {
                throw new IllegalArgumentException("element " + i + " in the passed VirtualFile array was null"); //$NON-NLS-1$ //$NON-NLS-2$
            }

            if (filter.test(resources[i])) {
                results.add(resources[i]);
            }
        }

        return results.toArray(new VirtualFile[0]);
    }

    /**
     * Equivalent to getting the children of the specified container directory,
     * and then filtering them through a predicate.
     *
     * @param container
     *        the container directory (must not be <code>null</code>)
     * @param filter
     *        the {@link Predicate} to use to filter the children (must not be
     *        <code>null</code>)
     * @return the filtered children of the container (never <code>null</code>)
     */
    public static VirtualFile[] getFilteredMembers(final VirtualFile container, final Predicate<VirtualFile> filter) {
        Check.notNull(container, "container"); //$NON-NLS-1$
        Check.notNull(filter, "filter"); //$NON-NLS-1$

        final VirtualFile[] allMembers = container.getChildren();

        final List<VirtualFile> filteredMembers = new ArrayList<>();

        for (final VirtualFile member : allMembers) {
            if (filter.test(member)) {
                filteredMembers.add(member);
            }
        }

        return filteredMembers.toArray(new VirtualFile[0]);
    }
}
