// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.github.lizhanyin.tfs.client.ui.vc.branch;

import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.BranchObject;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemIdentifier;

public class BranchObjectModelContentProvider {
    private BranchObjectModel model = null;

    public Object[] getChildren(final Object parentElement) {
        if (parentElement instanceof BranchObject) {
            final BranchObject b = (BranchObject) parentElement;
            return model.getChildren(b).toArray();
        }
        if (parentElement instanceof ItemIdentifier) {
            final ItemIdentifier id = (ItemIdentifier) parentElement;
            return getChildren(model.toBranchObject(id));
        }
        return null;
    }

    public Object getParent(final Object element) {
        if (element instanceof BranchObject) {
            final BranchObject b = (BranchObject) element;
            return model.getParent(b);
        }
        if (element instanceof ItemIdentifier) {
            final ItemIdentifier id = (ItemIdentifier) element;
            return getParent(model.toBranchObject(id));
        }
        return null;
    }

    public boolean hasChildren(final Object element) {
        if (element instanceof BranchObject) {
            final BranchObject b = (BranchObject) element;
            return !model.getChildren(b).isEmpty();
        }
        if (element instanceof ItemIdentifier) {
            final ItemIdentifier id = (ItemIdentifier) element;
            return hasChildren(model.toBranchObject(id));
        }
        return false;
    }

    public Object[] getElements(final Object inputElement) {
        if (inputElement instanceof BranchObjectModel) {
            final BranchObjectModel b = (BranchObjectModel) inputElement;
            return b.getRoots();
        }
        return null;
    }

    public void dispose() {
    }

    public void inputChanged(final Object oldInput, final Object newInput) {
        if (newInput instanceof BranchObjectModel) {
            model = (BranchObjectModel) newInput;
        }
    }
}
