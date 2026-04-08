// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.github.lizhanyin.tfs.client.ui.vc.serveritem;

import java.util.ArrayList;
import java.util.List;

import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.DeletedState;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ExtendedItem;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;

public class WorkspaceItemSource extends ServerItemSource {
    private final Workspace workspace;

    public WorkspaceItemSource(final Workspace workspace) {
        super(workspace.getClient().getConnection());

        this.workspace = workspace;
    }

    @Override
    protected TypedServerItem[] computeChildren(final TypedServerItem parent) {
        final ItemSpec[] itemSpecs = new ItemSpec[] {
            new ItemSpec(parent.getServerPath(), RecursionType.ONE_LEVEL)
        };

        final ExtendedItem[][] itemsArray =
            workspace.getExtendedItems(itemSpecs, DeletedState.NON_DELETED, ItemType.ANY);

        final List<TypedServerItem> list = new ArrayList<>();

        if (itemsArray != null && itemsArray.length > 0) {
            final ExtendedItem[] items = itemsArray[0];
            if (items != null) {
                for (ExtendedItem item : items) {
                    final ServerItemType type = ServerItemType.getTypeFromItemType(item.getItemType());
                    final boolean isBranch = item.isBranch();
                    final TypedServerItem child = new TypedServerItem(item.getTargetServerItem(), type, isBranch);

                    if (!parent.equals(child)) {
                        list.add(child);
                    }
                }
            }
        }

        return list.toArray(new TypedServerItem[0]);
    }
}
