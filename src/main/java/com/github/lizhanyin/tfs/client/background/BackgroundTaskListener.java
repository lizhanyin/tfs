// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.github.lizhanyin.tfs.client.background;

import java.util.EventListener;

/**
 * A simple listener implementation to provide common notifications for
 * background task notification.
 */
public interface BackgroundTaskListener extends EventListener {
    void onBackgroundTaskStarted(BackgroundTaskEvent event);

    void onBackgroundTaskFinished(BackgroundTaskEvent event);
}
