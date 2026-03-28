// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.github.lizhanyin.tfs.client.framework.command;

import com.intellij.openapi.progress.ProgressIndicator;

public interface CommandInitializationRunnable {

    public void initialize(ProgressIndicator progressMonitor) throws Exception;

    public void complete(ProgressIndicator progressMonitor);

}
