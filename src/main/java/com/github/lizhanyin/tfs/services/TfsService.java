package com.github.lizhanyin.tfs.services;

import com.github.lizhanyin.tfs.model.TfsWorkspace;
import com.github.lizhanyin.tfs.settings.TfsSettings;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

/**
 * TFS 核心服务
 * 管理与 TFS 服务器的连接和操作
 */
@Service(Service.Level.PROJECT)
public final class TfsService {

    private static final Logger LOG = Logger.getInstance(TfsService.class);

    private final Project project;
    private final TfsSettings settings;
    private TfsWorkspace currentWorkspace;
    private boolean initialized = false;

    public TfsService(@NotNull Project project) {
        this.project = project;
        this.settings = TfsSettings.getInstance(project);
    }

    /**
     * 初始化服务
     */
    public synchronized void initialize() {
        if (initialized) {
            return;
        }

        if (!settings.isConfigured()) {
            LOG.info("TFS settings not configured yet");
            return;
        }

        try {
            initialized = true;
            LOG.info("TFS service initialized successfully");
        } catch (Exception e) {
            LOG.error("Failed to initialize TFS service", e);
        }
    }
//
//    /**
//     * 检测并加载工作区信息
//     */
//    public CompletableFuture<TfsWorkspace> detectWorkspace() {
//        if (commandClient == null) {
//            return CompletableFuture.failedFuture(
//                    new IllegalStateException("TFS service not initialized"));
//        }
//
//        return CompletableFuture.supplyAsync(() -> {
//            try {
//                currentWorkspace = commandClient.getWorkspaceInfo();
//                return currentWorkspace;
//            } catch (Exception e) {
//                LOG.error("Failed to detect workspace", e);
//                throw new RuntimeException(e);
//            }
//        });
//    }

    /**
     * 获取当前工作区
     */
    @Nullable
    public TfsWorkspace getCurrentWorkspace() {
        return currentWorkspace;
    }

    /**
     * 获取配置
     */
    @NotNull
    public TfsSettings getSettings() {
        return settings;
    }

    /**
     * 检查是否已初始化
     */
    public boolean isInitialized() {
        return initialized;
    }

    /**
     * 检查是否已配置
     */
    public boolean isConfigured() {
        return settings.isConfigured();
    }

    /**
     * 重新初始化 (配置更改后调用)
     */
    public synchronized void reinitialize() {
        initialized = false;
        currentWorkspace = null;
        initialize();
    }

    /**
     * 获取服务实例
     */
    @NotNull
    public static TfsService getInstance(@NotNull Project project) {
        return project.getService(TfsService.class);
    }
}
