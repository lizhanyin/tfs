// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.github.lizhanyin.tfs.client.background;

import com.intellij.openapi.progress.ProgressIndicator;

import com.microsoft.tfs.util.Check;

/**
 * An {@link IBackgroundTask} that is backed by an IDEA {@link ProgressIndicator}.
 */
public class JobBackgroundTask implements IBackgroundTask {
    private final ProgressIndicator indicator;
    private final String name;

    public JobBackgroundTask(final ProgressIndicator indicator, final String name) {
        Check.notNull(indicator, "indicator"); //$NON-NLS-1$

        this.indicator = indicator;
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isCancellable() {
        return true;
    }

    @Override
    public boolean cancel() {
        indicator.cancel();
        return indicator.isCanceled();
    }
}
