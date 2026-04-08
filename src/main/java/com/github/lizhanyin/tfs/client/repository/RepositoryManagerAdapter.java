// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.github.lizhanyin.tfs.client.repository;

public abstract class RepositoryManagerAdapter implements RepositoryManagerListener {
    @Override
    public void onRepositoryAdded(final RepositoryManagerEvent event) {
    }

    @Override
    public void onRepositoryRemoved(final RepositoryManagerEvent event) {
    }

    @Override
    public void onDefaultRepositoryChanged(final RepositoryManagerEvent event) {
    }
}
