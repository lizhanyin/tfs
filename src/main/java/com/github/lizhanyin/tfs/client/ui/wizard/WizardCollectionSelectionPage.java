package com.github.lizhanyin.tfs.client.ui.wizard;

import com.github.lizhanyin.tfs.client.catalog.CrossCollectionProjectInfo;
import com.github.lizhanyin.tfs.client.catalog.TeamProjectCollectionInfo;
import com.github.lizhanyin.tfs.client.commands.wizard.QueryProjectCollectionsCommand;
import com.github.lizhanyin.tfs.client.commands.wizard.QueryTeamProjectsCommand;
import com.github.lizhanyin.tfs.client.framework.command.ThreadedCancellableCommand;
import com.github.lizhanyin.tfs.runtime.IStatus;
import com.github.lizhanyin.tfs.wizard.ImportProjectContext;
import com.intellij.openapi.diagnostic.Logger;
import com.microsoft.tfs.core.TFSConfigurationServer;
import com.microsoft.tfs.core.TFSConnection;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.commonstructure.ProjectInfo;

import java.util.ArrayList;
import java.util.List;

public class WizardCollectionSelectionPage extends ExtendedWizardPage{
    protected static Logger log = Logger.getInstance(WizardCollectionSelectionPage.class);

    public WizardCollectionSelectionPage(ImportProjectContext context){
        super(context);
    }

    public List<CrossCollectionProjectInfo> queryTeamProjects(){
        List<CrossCollectionProjectInfo> projects = new ArrayList<>(100);

        TFSConnection connection = context.getTfsConn();

        TFSConfigurationServer configurationServer = null;
        if (connection instanceof TFSConfigurationServer) {
            configurationServer = (TFSConfigurationServer) connection;
        } else if (connection instanceof TFSTeamProjectCollection) {
            configurationServer = ((TFSTeamProjectCollection) connection).getConfigurationServer();
        }

        if (configurationServer == null) {
            log.error(new IllegalArgumentException("Unexpected connection type: " + connection.getClass().getName())); //$NON-NLS-1$
            return projects;
        }

        final List<TFSTeamProjectCollection> collections = new ArrayList<>(5);
        final QueryProjectCollectionsCommand queryCommand = new QueryProjectCollectionsCommand(configurationServer);

        final IStatus status = getCommandExecutor().execute(new ThreadedCancellableCommand(queryCommand));
        if (!status.isOK()) {
            return projects;
        }

        final TeamProjectCollectionInfo[] projectCollections = queryCommand.getProjectCollections();
        for (final TeamProjectCollectionInfo collectionInfo : projectCollections) {
            try {
                collections.add(
                        configurationServer.getTeamProjectCollection(collectionInfo.getIdentifier()));
            } catch (final Exception e) {
                log.warn("Failed to get Team Project Collection: " + collectionInfo.getDisplayName()); //$NON-NLS-1$
                log.warn(e);
            }
        }

        // For each collection get the list of projects
        for (final TFSTeamProjectCollection collection : collections) {
            final QueryTeamProjectsCommand queryCommand2 = new QueryTeamProjectsCommand(collection);
            final IStatus status2 = getCommandExecutor().execute(new ThreadedCancellableCommand(queryCommand2));
            if (!status2.isOK()) {
                continue;
            }
            final ProjectInfo[] projectInfos = queryCommand2.getProjects();
            for (final ProjectInfo info : projectInfos) {
                final CrossCollectionProjectInfo pi = new CrossCollectionProjectInfo(
                        collection,
                        info.getName(),
                        info.getURI(),
                        collection.getName(),
                        collection.getBaseURI().getHost());
                projects.add(pi);
            }
        }
        return projects;
    }
}
