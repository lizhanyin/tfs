// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.github.lizhanyin.tfs.client.ui.helpers;

import com.intellij.openapi.diagnostic.Logger;

/**
 * Browser utility for IntelliJ IDEA platform.
 * Provides a no-op implementation for session management.
 * In the Eclipse version, this cleared embedded browser sessions.
 * For IDEA/TFS 2015 on-prem, this is typically not needed.
 */
public class Browser {
    private static final Logger log = Logger.getInstance(Browser.class);

    /**
     * Clears browser sessions. No-op in IDEA platform.
     */
    public static void clearSessions() {
        log.debug("Browser.clearSessions() called - no-op in IDEA platform"); //$NON-NLS-1$
    }
}
