// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.github.lizhanyin.tfs.client.ui.controls.workspaces;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.WorkingFolder;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.util.Check;

public class WorkingFolderDataCollection {
    private final List<WorkingFolderData> workingFolders = new ArrayList<>();

    public WorkingFolderDataCollection() {

    }

    public WorkingFolderDataCollection(final Workspace workspace) {
        this(workspace.getFolders());
    }

    public WorkingFolderDataCollection(final WorkingFolder[] workingFolders) {
        Check.notNull(workingFolders, "workingFolders"); //$NON-NLS-1$

        for (WorkingFolder workingFolder : workingFolders) {
            add(new WorkingFolderData(workingFolder));
        }
    }

    public WorkingFolder[] createWorkingFolders() {
        final WorkingFolder[] result = new WorkingFolder[workingFolders.size()];
        int ix = 0;
        for (final WorkingFolderData workingFolderData : workingFolders) {
            result[ix++] = workingFolderData.createWorkingFolder();
        }
        return result;
    }

    public WorkingFolderDataCollection(final WorkingFolderData[] workingFolderData) {
        Check.notNull(workingFolderData, "workingFolderData"); //$NON-NLS-1$

        workingFolders.addAll(Arrays.asList(workingFolderData));
    }

    public WorkingFolderData[] getWorkingFolderData() {
        return workingFolders.toArray(new WorkingFolderData[0]);
    }

    public void add(final WorkingFolderData workingFolderData) {
        Check.notNull(workingFolderData, "workingFolderData"); //$NON-NLS-1$

        workingFolders.add(workingFolderData);
    }

    public void remove(final WorkingFolderData workingFolderData) {
        Check.notNull(workingFolderData, "workingFolderData"); //$NON-NLS-1$

        workingFolders.remove(workingFolderData);
    }
}
