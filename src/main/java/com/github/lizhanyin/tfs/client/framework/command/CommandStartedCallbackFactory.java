// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.github.lizhanyin.tfs.client.framework.command;

/**
 * Helper class for command started callback management.
 *
 * @threadsafety unknown
 */
public class CommandStartedCallbackFactory {
    protected CommandStartedCallbackFactory() {
    }

    public static ICommandStartedCallback getDefaultCallback() {
        return new TeamExplorerLogCommandStartedCallback();
    }
}
