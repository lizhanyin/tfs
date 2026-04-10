package com.github.lizhanyin.tfs.client.ui.wizard;

import com.github.lizhanyin.tfs.client.codemarker.CodeMarker;
import com.github.lizhanyin.tfs.client.codemarker.CodeMarkerDispatch;
import com.github.lizhanyin.tfs.client.ui.config.UIClientConnectionAdvisor;
import com.github.lizhanyin.tfs.client.ui.vc.serveritem.VersionedItemSource;
import com.github.lizhanyin.tfs.wizard.ImportProjectContext;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.commonstructure.CommonStructureClient;
import com.microsoft.tfs.core.clients.commonstructure.ProjectInfo;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class WizardProjectSelectionPage extends ExtendedWizardPage{

    public static final CodeMarker CODEMARKER_REFRESH_COMPLETE =
            new CodeMarker("com.github.lizhanyin.tfs.client.ui.wizard.WizardProjectSelectionPage#refreshComplete"); //$NON-NLS-1$


    public WizardProjectSelectionPage(ImportProjectContext context) {
        super(context);
    }


    public @NotNull VersionedItemSource queryItems() {
        final List<ProjectInfo> projects;

        TFSTeamProjectCollection connection = context.getCollection().getCollection();
        VersionedItemSource itemSource;

        if (context.getCollection() != null) {
            projects = Collections.singletonList(context.getCollection());
        } else {
            projects = getInitialTeamProjectList();
        }

        itemSource = new VersionedItemSource(connection, projects.toArray(new ProjectInfo[0]));
        itemSource.setCommandExecutor(getCommandExecutor());

//        folderControl.setServerItemSource(itemSource);
//
//        if (folderControl.getSelectedItems().length == 0) {
//            /*
//             * None previously selected (first time in). Look to see if we were
//             * created with paths in the TFVC Source Control Explorer view
//             */
//            final String[] folderPaths = options.getImportFolders();
//            setSelectedFolders(folderPaths);
//        } else {
//            /*
//             * We're back to the page with some items selected. Let's make sure
//             * that all selected items are visible, i.e. their parents are
//             * expanded.
//             */
//            folderControl.setSelectedItems(folderControl.getSelectedItems());
//        }
//
//        handleSelection();
//
//        computeWorkingSets();

        CodeMarkerDispatch.dispatch(CODEMARKER_REFRESH_COMPLETE);

        return itemSource;
    }



    private List<ProjectInfo> getInitialTeamProjectList() {
        final ProjectInfo[] selectedProjects;

        try {
            final TFSTeamProjectCollection connection = new TFSTeamProjectCollection(context.getServerUri(), context.getCredentials(), new UIClientConnectionAdvisor());;
            final CommonStructureClient client =
                    (CommonStructureClient) connection.getClient(CommonStructureClient.class);
            selectedProjects = client.listAllProjects();
        } catch (final Exception e) {
            log.error("Error getting initial team projects", e); //$NON-NLS-1$
            return new ArrayList<>();
        }

        final List<ProjectInfo> filteredProjects = new ArrayList<>();
        for (final ProjectInfo project : selectedProjects) {
            if (project.getSourceControlCapabilityFlags().contains(context.getSourceControlCapabilityFlags())) {
                filteredProjects.add(project);
            }
        }

        return filteredProjects;
    }
}
