// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.github.lizhanyin.tfs.client.codemarker;

import java.util.ArrayList;
import java.util.List;

import com.intellij.openapi.diagnostic.Logger;

/**
 * This class "dispatches" a code marker - basically notifying listeners that a
 * code marker has been hit.
 *
 * In the IDEA platform, listeners are registered programmatically via
 * {@link #addListener(CodeMarkerListener)}. When running the product generally,
 * no code marker listeners should be registered. When running in the test
 * harness, the test bridge will provide a code marker listener for its own use.
 */
public class CodeMarkerDispatch {
    public static final String EXTENSION_POINT_ID = "com.microsoft.tfs.client.common.codeMarkerListenerProviders"; //$NON-NLS-1$

    private static final Logger log = Logger.getInstance(CodeMarkerDispatch.class);

    private static final Object lock = new Object();
    private static final List<CodeMarkerListener> listeners = new ArrayList<CodeMarkerListener>();
    private static boolean listenersLoaded = false;

    /**
     * Registers a code marker listener programmatically.
     *
     * @param listener the listener to add (must not be <code>null</code>)
     */
    public static void addListener(final CodeMarkerListener listener) {
        synchronized (lock) {
            listeners.add(listener);
        }
    }

    /**
     * Removes a code marker listener.
     *
     * @param listener the listener to remove
     */
    public static void removeListener(final CodeMarkerListener listener) {
        synchronized (lock) {
            listeners.remove(listener);
        }
    }

    /**
     * Notifies listeners that a code marker has been reached.
     *
     * @param event
     */
    public static void dispatch(final CodeMarker event) {
        synchronized (lock) {
            listenersLoaded = true;

            for (final CodeMarkerListener listener : listeners) {
                try {
                    listener.onCodeMarker(event);
                } catch (final Throwable t) {
                    log.warn("Exception while providing CodeMarker (" + event.toString() + ")", t); //$NON-NLS-1$ //$NON-NLS-2$
                }
            }
        }
    }
}
