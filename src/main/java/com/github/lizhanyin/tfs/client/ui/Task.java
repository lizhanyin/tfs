// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.github.lizhanyin.tfs.client.ui;

import com.github.lizhanyin.tfs.client.ui.framework.command.ICommandExecutor;
import com.github.lizhanyin.tfs.runtime.IStatus ;

public interface Task {
    public void setCommandExecutor(ICommandExecutor commandExecutor);

    public ICommandExecutor getCommandExecutor();

    public IStatus run();
}
