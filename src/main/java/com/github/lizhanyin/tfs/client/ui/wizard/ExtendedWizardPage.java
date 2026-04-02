package com.github.lizhanyin.tfs.client.ui.wizard;

import com.github.lizhanyin.tfs.client.credentials.IdeaCredentialsManagerFactory;
import com.github.lizhanyin.tfs.client.framework.command.CommandExecutor;
import com.github.lizhanyin.tfs.client.framework.command.ICommandExecutor;
import com.github.lizhanyin.tfs.client.ui.framework.UIContext;
import com.github.lizhanyin.tfs.client.ui.framework.command.WizardContainerCommandExecutor;
import com.github.lizhanyin.tfs.wizard.ImportProjectContext;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.microsoft.tfs.core.config.persistence.DefaultPersistenceStoreProvider;
import com.microsoft.tfs.core.credentials.CachedCredentials;
import com.microsoft.tfs.core.credentials.CredentialsManager;
import com.microsoft.tfs.core.httpclient.CookieCredentials;
import com.microsoft.tfs.core.httpclient.Credentials;
import com.microsoft.tfs.core.httpclient.DefaultNTCredentials;
import com.microsoft.tfs.core.httpclient.UsernamePasswordCredentials;
import com.microsoft.tfs.core.util.ServerURIUtils;
import com.microsoft.tfs.util.Platform;

import java.net.URI;

public class ExtendedWizardPage {
    protected static Logger log = Logger.getInstance(ExtendedWizardPage.class);

    protected ImportProjectContext context;

    public ExtendedWizardPage(ImportProjectContext context){
        this.context = context;
    }


    protected ICommandExecutor getCommandExecutor() {
        UIContext uiContext = this.context.getUiContext();
        // 优先使用 UIContext 中携带的 ProgressIndicator
        if (uiContext.getProgressIndicator() != null) {
            return new WizardContainerCommandExecutor(uiContext);
        }

        // 尝试获取当前线程的 ProgressIndicator（在 Task.Modal/Backgroundable 内）
        final ProgressIndicator currentIndicator = ProgressManager.getInstance().getProgressIndicator();
        if (currentIndicator != null) {
            return new WizardContainerCommandExecutor(UIContext.of(
                    uiContext.getProject(), uiContext.getParentComponent(), currentIndicator));
        }

        // 无 ProgressIndicator 时使用不需要进度的执行器
        return new CommandExecutor();
    }


    protected Credentials getAccountCredentials(final URI accountUrl, final Credentials proposedCredentials) {
        if (proposedCredentials == null) {
            final CredentialsManager credentialsManager =
                    IdeaCredentialsManagerFactory.getCredentialsManager(DefaultPersistenceStoreProvider.INSTANCE);
            final CachedCredentials cachedCredentials = credentialsManager.getCredentials(accountUrl);

            if (cachedCredentials != null) {
                return cachedCredentials.toCredentials();
            } else {
                /*
                 * For on-premises servers, simply use empty
                 * UsernamePasswordCredentials (to force a username/password
                 * dialog.) For hosted servers, use default NT credentials at
                 * all (to avoid the username/password dialog.)
                 */
                return ServerURIUtils.isHosted(accountUrl) || Platform.isCurrentPlatform(Platform.WINDOWS)
                        ? new DefaultNTCredentials() : new UsernamePasswordCredentials("", null); //$NON-NLS-1$
            }
        } else if (proposedCredentials instanceof CookieCredentials) {
            return ((CookieCredentials) proposedCredentials).setDomain(accountUrl.getHost());
        } else {
            return proposedCredentials;
        }
    }

    protected void updateCredentials(final URI accountUrl, final Credentials credentials) {
        final CredentialsManager credentialsManager =
                IdeaCredentialsManagerFactory.getCredentialsManager(DefaultPersistenceStoreProvider.INSTANCE);

        // PasswordSafe.setPassword() 是慢操作，不允许在 EDT 上执行
        com.intellij.openapi.application.ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                if (credentials != null && !(credentials instanceof DefaultNTCredentials)) {
                    log.debug("Save the new Cookie Credentials in the Eclipse secure storage for future sessions."); //$NON-NLS-1$
                    credentialsManager.setCredentials(new CachedCredentials(accountUrl, credentials));
                } else {
                    credentialsManager.removeCredentials(accountUrl);
                }
            } catch (final Exception e) {
                log.error("Error writing credentials to the IntelliJ IDEA secure store", e); //$NON-NLS-1$
            }
        });
    }

}
