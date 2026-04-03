package com.github.lizhanyin.tfs.client.ui.wizard;

import com.github.lizhanyin.tfs.client.commands.CreateWorkspaceCommand;
import com.github.lizhanyin.tfs.client.commands.QueryLocalWorkspacesCommand;
import com.github.lizhanyin.tfs.client.framework.command.CommandExecutor;
import com.github.lizhanyin.tfs.client.framework.command.ICommandExecutor;
import com.github.lizhanyin.tfs.runtime.IStatus;
import com.github.lizhanyin.tfs.wizard.ImportProjectContext;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.versioncontrol.WorkspaceLocation;
import com.microsoft.tfs.core.clients.versioncontrol.WorkspacePermissionProfile;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.jni.helpers.LocalHost;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

    public Workspace addWorkspace(
            @NotNull String name,
            @Nullable String comment,
            @NotNull WorkspaceLocation location,
            @NotNull WorkspacePermissionProfile permissionProfile
    ) throws Exception {
        final TFSTeamProjectCollection connection = context.getCollection().getCollection();
        final CreateWorkspaceCommand command = new CreateWorkspaceCommand(
                connection,
                null, // workingFolders - 创建时无映射
                name,
                comment,
                location,
                null, // options - 使用默认
                permissionProfile);

        final ICommandExecutor executor = new CommandExecutor();
        final IStatus status = executor.execute(command);
        if (!status.isOK()) {
            throw new Exception("创建工作区失败: " + status.getMessage());
        }
        return command.getWorkspace();
    }
}
