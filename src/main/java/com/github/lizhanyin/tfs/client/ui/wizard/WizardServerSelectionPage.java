package com.github.lizhanyin.tfs.client.ui.wizard;

import com.github.lizhanyin.tfs.client.framework.command.ICommandExecutor;
import com.github.lizhanyin.tfs.client.ui.framework.command.UICommandFinishedCallbackFactory;
import com.github.lizhanyin.tfs.client.ui.tasks.ConnectToConfigurationServerTask;
import com.github.lizhanyin.tfs.runtime.IStatus;
import com.github.lizhanyin.tfs.wizard.ImportProjectContext;
import com.microsoft.tfs.core.TFSConnection;
import com.microsoft.tfs.core.httpclient.Credentials;

import java.net.URI;

public class WizardServerSelectionPage extends ExtendedWizardPage{

    private final URI accountUri;
    private final Credentials credentials;

    public WizardServerSelectionPage(ImportProjectContext context){
        this(context, null);
    }

    public WizardServerSelectionPage(ImportProjectContext context, Credentials credentials){
        super(context);
        this.credentials = credentials;
        this.accountUri = context.getServerUri();
    }

    public TFSConnection openAccount(){
        final Credentials accountCredentials = getAccountCredentials(accountUri, credentials);

        final ICommandExecutor noErrorDialogCommandExecutor = getCommandExecutor();
        noErrorDialogCommandExecutor.setCommandFinishedCallback(
                UICommandFinishedCallbackFactory.getDefaultNoErrorDialogCallback());

        final ConnectToConfigurationServerTask connectTask =
                new ConnectToConfigurationServerTask(context.getUiContext(), accountUri, accountCredentials);
        connectTask.setCommandExecutor(noErrorDialogCommandExecutor);
        final IStatus status = connectTask.run();

        final TFSConnection connection;
        if (status.isOK()) {
            connection = connectTask.getConnection();
            updateCredentials(accountUri, connection.getCredentials());
        } else {
            /* Connection cancelled */
            connection = null;
        }

        return connection;
    }
}
