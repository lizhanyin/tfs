package com.github.lizhanyin.tfs.client.ui.wizard;

import com.github.lizhanyin.tfs.client.commands.CreateWorkspaceCommand;
import com.github.lizhanyin.tfs.client.commands.QueryLocalWorkspacesCommand;
import com.github.lizhanyin.tfs.runtime.IStatus;
import com.github.lizhanyin.tfs.wizard.ImportProjectContext;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.versioncontrol.WorkspacePermissionProfile;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.jni.helpers.LocalHost;

public class WizardWorkspacePage extends ExtendedWizardPage{

    public WizardWorkspacePage(ImportProjectContext context){
        super(context);
    }

    public Workspace[] queryWorkspace(final boolean createWorkspaceIfNone){
        Workspace[] workspaces = {};
        var commandExecutor = getCommandExecutor();
        var connection = context.getCollection().getCollection();
        final QueryLocalWorkspacesCommand queryCommand = new QueryLocalWorkspacesCommand(connection);
        if (commandExecutor.execute(queryCommand).getSeverity() != IStatus.OK) {
            return workspaces;
        }
        workspaces = queryCommand.getWorkspaces();
        if (createWorkspaceIfNone && (workspaces == null || workspaces.length == 0)) {
            /*
             * No workspaces - create a default one.
             */
            final CreateWorkspaceCommand createCommand = new CreateWorkspaceCommand(
                    connection,
                    null,
                    LocalHost.getShortName(),
                    null,
                    null,
                    null,
                    WorkspacePermissionProfile.getPrivateProfile());
            if (commandExecutor.execute(createCommand).getSeverity() != IStatus.OK) {
                return workspaces;
            }
            workspaces = new Workspace[] {
                createCommand.getWorkspace()
            };
        }

//        workspacesTable.setWorkspaces(workspaces);
//        workspacesTable.setSelectedWorkspaces(previouslySelectedWorkspaces);
//
//        if (autoSelect && workspacesTable.getSelectionCount() == 0) {
//            // Use the default workspace as the initial selection if one
//            // exists. Otherwise retrieve the last referenced workspace
//            // name and set it as the default selection.
//            final RepositoryManager manager =
//                    TFSCommonUIClientPlugin.getDefault().getProductPlugin().getRepositoryManager();
//            final TFSRepository defaultRepository = manager.getDefaultRepository();
//
//            if (defaultRepository != null) {
//                workspacesTable.setSelectedWorkspace(defaultRepository.getWorkspace());
//            } else {
//                final String lastWorkspaceName =
//                        getPreferencesNode(viewDataKey, connection).get(PREFS_NODE_LAST_WORKSPACE_NAME, null);
//                if (lastWorkspaceName != null) {
//                    workspacesTable.setSelectedWorkspace(lastWorkspaceName);
//                }
//            }
//
//            if (workspacesTable.getSelectionCount() == 0) {
//                workspacesTable.selectFirst();
//            }
//        }

//        workspacesTable.setFocus();
        return workspaces;
    }

}
