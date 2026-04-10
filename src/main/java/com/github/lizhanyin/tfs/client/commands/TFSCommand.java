// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.github.lizhanyin.tfs.client.commands;

import com.intellij.openapi.progress.ProgressIndicator;

import com.github.lizhanyin.tfs.runtime.IStatus;
import com.github.lizhanyin.tfs.TFSClientPlugin;
import com.github.lizhanyin.tfs.client.framework.command.Command;
import com.github.lizhanyin.tfs.client.framework.command.CommandInitializationRunnable;
import com.github.lizhanyin.tfs.client.framework.command.exception.ICommandExceptionHandler;
import com.github.lizhanyin.tfs.client.framework.status.TeamExplorerStatus;
import com.github.lizhanyin.tfs.client.util.ProgressMonitorTaskMonitorAdapter;
import com.microsoft.tfs.core.exceptions.TECoreException;
import com.microsoft.tfs.core.httpclient.ActiveHttpMethods;
import com.microsoft.tfs.util.tasks.TaskMonitor;
import com.microsoft.tfs.util.tasks.TaskMonitorService;

/**
 * An extension of the basic command but with handling built in for common
 * exception cases in our client products.
 */
public abstract class TFSCommand extends Command {

    public TFSCommand() {
        addExceptionHandler(new TFSCommandExceptionHandler());

        addCommandInitializationRunnable(new TFSCommandInitializationRunnable());
    }

    protected static class TFSCommandExceptionHandler implements ICommandExceptionHandler {
        @Override
        public IStatus onException(final Throwable t) {
            if (t instanceof TECoreException) {
                final String exceptionMessage = getErrorMessage(t.getLocalizedMessage());
                return new TeamExplorerStatus(IStatus.ERROR, TFSClientPlugin.PLUGIN_ID, 0, exceptionMessage, t);
            }

            return null;
        }

        public static String getErrorMessage(final String message) {
            return message.replaceFirst("^TF[\\d]+: ", ""); //$NON-NLS-1$ //$NON-NLS-2$
        }
    }

    private static class TFSCommandInitializationRunnable implements CommandInitializationRunnable {
        private volatile boolean addedTaskMonitorAdapter = false;

        @Override
        public void initialize(final ProgressIndicator progressMonitor) throws Exception {
            final TaskMonitor tm = new ProgressMonitorTaskMonitorAdapter(progressMonitor);
            TaskMonitorService.pushTaskMonitor(tm);
            ActiveHttpMethods.setMonitor(tm);
            addedTaskMonitorAdapter = true;
        }

        @Override
        public void complete(final ProgressIndicator progressMonitor) {
            if (addedTaskMonitorAdapter) {
                TaskMonitorService.popTaskMonitor(true);
                ActiveHttpMethods.clearMonitor();
            }
        }
    }
}
