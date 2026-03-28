// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.github.lizhanyin.tfs.client.ui;

import java.util.concurrent.Executor;

import com.github.lizhanyin.tfs.client.framework.helper.UIHelpers;
import org.jetbrains.annotations.NotNull;

/**
 * Runs {@link Runnable}s on the thread where the SWT/Eclipse UI is running.
 * Provided via extension to the JNI plug-in so it can marshall library calls
 * that must execute on the UI thread.
 *
 * @threadsafety unknown
 */
public class TFSCommonUIExecutor implements Executor {
    public TFSCommonUIExecutor() {
    }

    @Override
    public void execute(final @NotNull Runnable command) {
        UIHelpers.runOnUIThread(false, command);
    }
}
