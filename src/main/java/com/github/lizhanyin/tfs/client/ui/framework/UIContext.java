// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.github.lizhanyin.tfs.client.ui.framework;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.WindowManager;
import org.jetbrains.annotations.Nullable;

import java.awt.*;

/**
 * UI 上下文，类似于 Eclipse SWT 中的 Shell。
 * 封装了 IntelliJ 中与顶层窗口相关的上下文信息，替代直接传递 {@link Project} 的模式。
 *
 * <p>提供三个核心概念：</p>
 * <ul>
 *   <li>{@link #getProject()} - IntelliJ 项目上下文，用于服务查找、进度任务等</li>
 *   <li>{@link #getParentComponent()} - 顶层窗口组件，用于对话框父级定位</li>
 *   <li>{@link #getProgressIndicator()} - 进度指示器，用于命令执行中的进度反馈和取消</li>
 * </ul>
 */
public class UIContext {
    @Nullable
    private final Project project;
    @Nullable
    private final Component parentComponent;
    @Nullable
    private final ProgressIndicator progressIndicator;

    public UIContext(@Nullable final Project project) {
        this(project, null, null);
    }

    public UIContext(@Nullable final Project project, @Nullable final Component parentComponent) {
        this(project, parentComponent, null);
    }

    public UIContext(@Nullable final Project project, @Nullable final Component parentComponent, @Nullable final ProgressIndicator progressIndicator) {
        this.project = project;
        this.parentComponent = parentComponent;
        this.progressIndicator = progressIndicator;
    }

    /**
     * @return the IntelliJ {@link Project}, or <code>null</code>
     */
    @Nullable
    public Project getProject() {
        return project;
    }

    /**
     * @return the explicitly set parent component, or <code>null</code>
     */
    @Nullable
    public Component getParentComponent() {
        return parentComponent;
    }

    /**
     * @return the progress indicator, or <code>null</code>
     */
    @Nullable
    public ProgressIndicator getProgressIndicator() {
        return progressIndicator;
    }

    /**
     * 获取用于对话框父级的最佳组件。
     * 优先使用显式设置的 parentComponent，否则从 project 获取主窗口框架。
     */
    @Nullable
    public Component getDialogParent() {
        if (parentComponent != null) {
            return parentComponent;
        }
        if (project != null && !project.isDisposed()) {
            return WindowManager.getInstance().getFrame(project);
        }
        return null;
    }

    /**
     * 便捷方法：从 Project 创建 UIContext。
     */
    public static UIContext from(@Nullable final Project project) {
        return new UIContext(project);
    }

    /**
     * 便捷方法：从 Project 和父组件创建 UIContext。
     */
    public static UIContext of(@Nullable final Project project, @Nullable final Component parentComponent) {
        return new UIContext(project, parentComponent);
    }

    /**
     * 便捷方法：创建带进度指示器的完整 UIContext。
     */
    public static UIContext of(@Nullable final Project project, @Nullable final Component parentComponent, @Nullable final ProgressIndicator progressIndicator) {
        return new UIContext(project, parentComponent, progressIndicator);
    }
}
