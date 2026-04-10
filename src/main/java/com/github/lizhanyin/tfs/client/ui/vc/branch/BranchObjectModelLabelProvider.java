// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.github.lizhanyin.tfs.client.ui.vc.branch;

import com.github.lizhanyin.tfs.TFSClientPlugin;
import com.github.lizhanyin.tfs.client.ui.framework.helper.ImageHelper;

import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.BranchObject;

import javax.swing.Icon;

public class BranchObjectModelLabelProvider {
    public String getText(final Object element) {
        if (element instanceof BranchObject) {
            final BranchObject b = (BranchObject) element;
            final String serverPath = b.getProperties().getRootItem().getItem();
            return ServerPath.getFileName(serverPath);
        }
        return null;
    }

    public Icon getImage(final Object element) {
        if (element instanceof BranchObject) {
            return new ImageHelper(TFSClientPlugin.PLUGIN_ID).getImage("images/vc/folder_branch.gif"); //$NON-NLS-1$
        }
        return null;
    }
}
