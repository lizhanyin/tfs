// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.github.lizhanyin.tfs.client.framework.command;

import com.intellij.openapi.progress.Task;

/**
 * Methods for waiting on certain conditions types of asynchronous objects that
 * {@link ICommandExecutor}s use. An implementation may perform other work while
 * waiting on the asynchronous objects (for instance, running UI events).
 *
 * @threadsafety thread-safe
 */
public interface IAsyncObjectWaiter {
    /**
     * An interface for simple truth testing.
     *
     * @threadsafety thread-safe
     */
    public interface Predicate {
        /**
         * @return <code>true</code> if the condition is true,
         *         <code>false</code> if it is false
         */
        boolean isTrue();
    }

    /**
     * Blocks until the thread has finished.
     *
     * @param thread
     *        the thread to wait on (must not be <code>null</code>)
     */
    void joinThread(Thread thread) throws InterruptedException;

    /**
     * Blocks until the job has finished.
     *
     * @param task
     *        the job to wait on (must not be <code>null</code>)
     */
    void joinJob(Task task) throws InterruptedException;

    /**
     * Blocks until the predicate reports the condition is true.
     *
     * @param predicate
     *        the predicate to test (must not be <code>null</code>)
     */
    void waitUntilTrue(Predicate predicate) throws InterruptedException;
}
