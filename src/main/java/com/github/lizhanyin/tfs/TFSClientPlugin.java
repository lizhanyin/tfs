package com.github.lizhanyin.tfs;

import com.github.lizhanyin.tfs.client.repository.RepositoryManager;
import com.github.lizhanyin.tfs.client.server.ServerManager;
import com.github.lizhanyin.tfs.runtime.IStatus;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;

/**
 * TFS 插件全局服务（IDEA ApplicationService）。
 * <p>
 * 替代原 Eclipse 的 Plugin 单例模式，在插件启动时由 IDEA 平台自动创建。
 * 提供：
 * <ul>
 *   <li>全局状态管理（ServerManager、RepositoryManager）</li>
 *   <li>前后端桥接（UI 层通过 getDefault() 获取后端服务）</li>
 *   <li>日志功能</li>
 * </ul>
 */
@Service(Service.Level.PROJECT)
public final class TFSClientPlugin {
    private static final Logger log = Logger.getInstance(TFSClientPlugin.class);

    public static final String PLUGIN_ID = "com.microsoft.tfs.client"; //$NON-NLS-1$

    private static Project project;
    private final ServerManager serverManager = new ServerManager();
    private final RepositoryManager repositoryManager = new RepositoryManager();
    //    private final ResourceRefreshManager resourceRefreshManager = new ResourceRefreshManager(repositoryManager);
    //    private final ProjectRepositoryManager projectRepositoryManager = new ProjectRepositoryManager(serverManager, repositoryManager);
    /*
     * ResourceDataManager listens to core events, adapts those events into
     * information to be saved in the workspace's ISynchronizer store, and
     * queues those updates. It listens to workspace resource change events to
     * dequeue the updates (writes them to the ISynchronizer store). This must
     * be done in response to workspace change events because it needs to know
     * the state of the resources after they are fully modified by the core
     * operation.
     */
//    private final ResourceDataManager resourceDataManager = new ResourceDataManager(ResourcesPlugin.getWorkspace().getSynchronizer());

    /*
     * The main resource change listener for the plugin exists to pick up added
     * files and pends changes for them. It ignores changes that are not adds
     * (moves, renames, etc. are handled elsewhere).
     */
//    private final TFSResourceChangeListener resourceChangedListener = new TFSResourceChangeListener();

    private final Object consoleLock = new Object();
//    private TFSConsoleProvider consoleProvider = null;

    public TFSClientPlugin(Project _project) {
        project = _project;
        log.info("TFS plugin starting...");
    }

    /**
     * 获取项目级插件实例。
     *
     * @return the plugin instance (never <code>null</code>)
     */
    public static TFSClientPlugin getDefault() {
        return TFSClientPlugin.getDefault(project);
    }

    /**
     * 获取项目级插件实例。
     *
     * @return the plugin instance (never <code>null</code>)
     */
    public static TFSClientPlugin getDefault(Project project) {
        return project.getService(TFSClientPlugin.class);
    }

    public void start(){
//        ResourcesPlugin.getWorkspace().addResourceChangeListener(
//                resourceChangedListener,
//                IResourceChangeEvent.POST_CHANGE);
//
//        /*
//         * Queue a job to notify the project repository manager to start. We
//         * cannot simply do this work (we must put it in a job) to ensure that
//         * the workbench is started. When the workbench is started, it will
//         * start the JobManager which will execute our startup hook here.
//         */
//        final Job projectStartupJob = new Job(Messages.getString("TFSEclipseClientPlugin.ConnectintToTfsJobTitle")) //$NON-NLS-1$
//        {
//            @Override
//            protected IStatus run(final IProgressMonitor progressMonitor) {
//                projectRepositoryManager.start();
//                return Status.OK_STATUS;
//            }
//        };
//        projectStartupJob.setSystem(true);
//        projectStartupJob.schedule();
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.eclipse.core.runtime.Plugin#stop(org.osgi.framework.BundleContext)
     */
    public void stop() throws Exception {
//        ResourcesPlugin.getWorkspace().removeResourceChangeListener(resourceChangedListener);
//
//        plugin = null;
//        super.stop(context);
    }

    public ServerManager getServerManager() {
        return serverManager;
    }

    public RepositoryManager getRepositoryManager() {
        return repositoryManager;
    }


//    public ProjectRepositoryManager getProjectManager() {
//        return projectRepositoryManager;
//    }
//
//    public ResourceDataManager getResourceDataManager() {
//        return resourceDataManager;
//    }
//
//    public TFSResourceChangeListener getResourceChangeListener() {
//        return resourceChangedListener;
//    }
//
//    public ResourceRefreshManager getResourceRefreshManager() {
//        return resourceRefreshManager;
//    }
//
//    /**
//     * @return the {@link TFSConsoleProvider} contributed via extension point,
//     *         or <code>null</code> if none found
//     */
//    private final TFSConsoleProvider getConsoleProvider() {
//        synchronized (consoleLock) {
//            if (consoleProvider == null) {
//                try {
//                    consoleProvider =
//                            (TFSConsoleProvider) ExtensionLoader.loadSingleExtensionClass(TFS_CONSOLE_EXTENSION_POINT_ID);
//                } catch (final Exception e) {
//                    LogFactory.getLog(getClass()).error("Could not load TFS console provider for the product", e); //$NON-NLS-1$
//                    consoleProvider = null;
//                }
//            }
//
//            return consoleProvider;
//        }
//    }
//
//    public TFSEclipseConsole getConsole() {
//        final TFSConsoleProvider consoleProvider = getConsoleProvider();
//        if (consoleProvider != null) {
//            return consoleProvider.getConsole();
//        } else {
//            return new NullConsole();
//        }
//
//    }

    public static void log(final IStatus status) {
        if (status.getSeverity() == IStatus.ERROR) {
            log.error(status.getMessage(), status.getException());
        } else if (status.getSeverity() == IStatus.WARNING) {
            log.warn(status.getMessage(), status.getException());
        } else {
            log.info(status.getMessage());
        }
    }
}
