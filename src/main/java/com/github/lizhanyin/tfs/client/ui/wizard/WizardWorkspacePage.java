package com.github.lizhanyin.tfs.client.ui.wizard;

import com.github.lizhanyin.tfs.client.commands.wizard.CreateWorkspaceCommand;
import com.github.lizhanyin.tfs.client.commands.wizard.DeleteWorkspaceCommand;
import com.github.lizhanyin.tfs.client.commands.wizard.QueryLocalWorkspacesCommand;
import com.github.lizhanyin.tfs.client.commands.wizard.UpdateWorkspaceCommand;
import com.github.lizhanyin.tfs.client.ui.controls.workspaces.WorkspaceData;
import com.github.lizhanyin.tfs.runtime.IStatus;
import com.github.lizhanyin.tfs.wizard.ImportProjectContext;
import com.microsoft.tfs.core.clients.versioncontrol.GetOptions;
import com.microsoft.tfs.core.clients.versioncontrol.WorkspaceOptions;
import com.microsoft.tfs.core.clients.versioncontrol.WorkspacePermissionProfile;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.WorkingFolder;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.WorkingFolderComparator;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.WorkingFolderComparatorType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.jni.helpers.LocalHost;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

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
        return workspaces;
    }

    public Workspace createWorkspace(@NotNull WorkspaceData workspaceData) throws Exception {
        var connection = context.getCollection().getCollection();
        final CreateWorkspaceCommand command = new CreateWorkspaceCommand(
                connection,
                workspaceData.getWorkingFolderDataCollection().createWorkingFolders(),
                workspaceData.getWorkspaceDetails().getName(),
                workspaceData.getWorkspaceDetails().getComment(),
                workspaceData.getWorkspaceDetails().getWorkspaceLocation(),
                workspaceData.getWorkspaceDetails().getWorkspaceOptions(),
                workspaceData.getWorkspaceDetails().getPermissionProfile());

        var commandExecutor = getCommandExecutor();
        final IStatus status = commandExecutor.execute(command);
        if (!status.isOK()) {
            throw new Exception("创建工作区失败: " + status.getMessage());
        }
        return command.getWorkspace();
    }

    public Workspace updateWorkspace(@NotNull WorkspaceData dataToEdit,
                                     @NotNull WorkspaceData oldData,
                                     @NotNull Workspace workspace) {

        final UpdateWorkspaceCommand command = new UpdateWorkspaceCommand(
                workspace,
                dataToEdit.getWorkspaceDetails().getName(),
                dataToEdit.getWorkspaceDetails().getComment(),
                dataToEdit.getWorkingFolderDataCollection().createWorkingFolders(),
                dataToEdit.getWorkspaceDetails().getWorkspaceOptions(),
                dataToEdit.getWorkspaceDetails().getWorkspaceLocation(),
                dataToEdit.getWorkspaceDetails().getPermissionProfile());

        var commandExecutor = getCommandExecutor();
        if (commandExecutor.execute(command).isOK()) {
            /*
             * Check to see if any folders changed.
             */
            final WorkingFolder[] oldFolders = oldData.getWorkingFolderDataCollection().createWorkingFolders();
            final WorkingFolder[] newFolders =
                    dataToEdit.getWorkingFolderDataCollection().createWorkingFolders();

            Arrays.sort(oldFolders, new WorkingFolderComparator(WorkingFolderComparatorType.SERVER_PATH));
            Arrays.sort(newFolders, new WorkingFolderComparator(WorkingFolderComparatorType.SERVER_PATH));
            final boolean foldersChanged = !Arrays.equals(oldFolders, newFolders);

            final WorkspaceOptions oldOptions = oldData.getWorkspaceDetails().getWorkspaceOptions();
            final WorkspaceOptions newOptions = dataToEdit.getWorkspaceDetails().getWorkspaceOptions();

            final boolean fileTimeChanged = !oldOptions.contains(WorkspaceOptions.SET_FILE_TO_CHECKIN)
                    && newOptions.contains(WorkspaceOptions.SET_FILE_TO_CHECKIN)
                    && newFolders.length > 0;

            final GetOptions getOptions = fileTimeChanged ? GetOptions.GET_ALL : GetOptions.NONE;

            if (foldersChanged || fileTimeChanged) {
                /*
                 * Prompt to get latest.
                 *
                 * Prefer the file time message if folders also changed
                 * since the force get will handle everything.
                 */
//                final boolean getNow = MessageDialog.openQuestion(
//                        getShell(),
//                        Messages.getString("WorkspacesControl.WorkspaceModifiedDialogTitle"), //$NON-NLS-1$
//                        fileTimeChanged ? Messages.getString("WorkspacesControl.SetFileTimeToCheckinGetPrompt") //$NON-NLS-1$
//                                : Messages.getString("WorkspacesControl.WorkspaceChangedMessage")); //$NON-NLS-1$
//
//                if (getNow) {
//                    final RepositoryManager manager =
//                            TFSCommonUIClientPlugin.getDefault().getProductPlugin().getRepositoryManager();
//                    TFSRepository repository = manager.getRepository(selectedWorkspace);
//                    if (repository == null) {
//                        repository = new TFSRepository(selectedWorkspace);
//                    }
//
//                    /*
//                     * GetTask will not prompt for
//                     * "all files up to date" like GetLatestTask does,
//                     * and it accepts GetOptions. A GetRequest with a
//                     * null item spec means "get the whole workspace."
//                     */
//                    final GetTask task = new GetTask(getShell(), repository, new GetRequest[] {
//                            new GetRequest(null, LatestVersionSpec.INSTANCE)
//                    }, getOptions);
//                    task.run();
//                }
            }
        }
        return workspace;
    }

    public boolean deleteWorkspace(@NotNull Workspace workspace) {
        var commandExecutor = getCommandExecutor();
        final DeleteWorkspaceCommand command = new DeleteWorkspaceCommand(workspace);

        final IStatus status = commandExecutor.execute(command);

        return status.isOK();
    }
}
