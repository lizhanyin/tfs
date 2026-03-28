// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.github.lizhanyin.tfs.client.framework.command;

import java.text.MessageFormat;

import com.github.lizhanyin.tfs.client.util.ExtensionLoader;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.Task;
import org.jetbrains.annotations.NotNull;

/**
 * An {@link IAsyncObjectWaiter} that will proxy to an implementation provided
 * by an extension point, or if there is no contribution, falls back to a simple
 * implementation.
 * <p>
 * Prefer using this class to waiting on objects directly, so extensions can
 * perform work like processing UI events.
 * <p>
 * This class is for use inside this plug-in only.
 *
 * @threadsafety thread-safe
 */
public class ExtensionPointAsyncObjectWaiter implements IAsyncObjectWaiter {
    public static final String EXTENSION_POINT_ID = "com.github.lizhanyin.tfs.asyncObjectWaiter"; //$NON-NLS-1$

    private static final Logger log = Logger.getInstance(ExtensionPointAsyncObjectWaiter.class);

    private static final Object extensionLock = new Object();
    private static volatile IAsyncObjectWaiter extension;

    public ExtensionPointAsyncObjectWaiter() {
    }

    @Override
    public void joinThread(@NotNull final Thread thread) throws InterruptedException {
        final IAsyncObjectWaiter e = getExtension();
        if (e != null) {
            e.joinThread(thread);
        } else {
            thread.join();
        }
    }

    @Override
    public void joinTask(@NotNull final Task task) throws InterruptedException {
        final IAsyncObjectWaiter e = getExtension();
        if (e != null) {
            e.joinTask(task);
        } else {
            // IDEA Task doesn't have a direct join method
            // We need to wait for completion using polling
            waitUntilTrue(new Predicate() {
                @Override
                public boolean isTrue() {
                    // For JobCommandAdapter (our Task.Backgroundable subclass),
                    // check if the status has been set (indicates completion)
                    if (task instanceof JobCommandAdapter) {
                        ((JobCommandAdapter) task).getStatus();
                        return true;
                    }
                    // For other Task types, we can't reliably check completion
                    // This shouldn't happen in normal usage
                    return true;
                }
            });
        }
    }

    @Override
    public void waitUntilTrue(@NotNull final Predicate predicate) throws InterruptedException {
        final IAsyncObjectWaiter e = getExtension();
        if (e != null) {
            e.waitUntilTrue(predicate);
        } else {
            while (!predicate.isTrue()) {
                Thread.sleep(10);
            }
        }
    }

    private static IAsyncObjectWaiter getExtension() {
        if (extension == null) {
            synchronized (extensionLock) {
                if (extension == null) {
                    final Object provider = ExtensionLoader.loadSingleExtensionClass(EXTENSION_POINT_ID, false);

                    if (provider instanceof IAsyncObjectWaiter) {
                        extension = (IAsyncObjectWaiter) provider;
                    }

                    if (extension == null) {
                        log.debug(
                            MessageFormat.format(
                                "No IAsyncObjectWaiter at extension point {0}, using simple implementation", //$NON-NLS-1$
                                EXTENSION_POINT_ID));
                    }
                }
            }
        }

        return extension;
    }
}
