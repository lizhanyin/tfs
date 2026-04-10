// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.github.lizhanyin.tfs.client.framework.command;


import com.github.lizhanyin.tfs.TFSClientPlugin;
import com.github.lizhanyin.tfs.client.framework.status.TeamExplorerStatus;
import com.github.lizhanyin.tfs.client.framework.status.UncaughtCommandExceptionStatus;
import com.github.lizhanyin.tfs.runtime.IStatus;

public class PlatformLogCommandFinishedCallback implements ICommandFinishedCallback {

    @Override
    public void onCommandFinished(final ICommand command, IStatus status) {
        if (status instanceof UncaughtCommandExceptionStatus) {
            /*
             * UncaughtCommandExceptionStatus is a TeamExplorerStatus, which
             * does a silly trick: it makes the exception it was given
             * accessible only through a non-standard method. This helps when
             * displaying the status to the user in a dialog, but prevents the
             * platform logger from logging stack traces, so fix it up here.
             *
             * The right long term fix is to ditch TeamExplorerStatus entirely.
             */
            status = ((TeamExplorerStatus) status).toNormalStatus();

            TFSClientPlugin.log(status);
        }
    }
}
