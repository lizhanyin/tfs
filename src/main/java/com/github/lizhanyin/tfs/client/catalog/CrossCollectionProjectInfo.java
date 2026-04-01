package com.github.lizhanyin.tfs.client.catalog;

import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.commonstructure.ProjectInfo;
import com.microsoft.tfs.core.clients.versioncontrol.SourceControlCapabilityFlags;

public class CrossCollectionProjectInfo extends ProjectInfo {
    private final String collectionName;
    private final String accountName;
    private final TFSTeamProjectCollection collection;

    public CrossCollectionProjectInfo(
            TFSTeamProjectCollection collection,
            String name,
            String uri,
            String collectionName,
            String accountName) {
        super(
                name != null ? name : "", //$NON-NLS-1$
                uri != null ? uri : ""); //$NON-NLS-1$

        this.collectionName = collectionName != null ? collectionName : ""; //$NON-NLS-1$
        this.accountName = accountName != null ? accountName : ""; //$NON-NLS-1$
        this.collection = collection;
    }

    public TFSTeamProjectCollection getCollection() {
        return collection;
    }

    public String getCollectionName() {
        return collectionName;
    }

    public String getAccountName() {
        return accountName;
    }

    public boolean isGitProject() {
        return getSourceControlCapabilityFlags().contains(SourceControlCapabilityFlags.GIT);
    }

    public boolean isTfsProject() {
        return getSourceControlCapabilityFlags().contains(SourceControlCapabilityFlags.TFS);
    }
}
