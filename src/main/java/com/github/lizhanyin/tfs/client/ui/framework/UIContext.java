// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.github.lizhanyin.tfs.client.ui.framework;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.WindowManager;
import org.jetbrains.annotations.Nullable;

import java.awt.*;

/**
 * UI 上下文，类似于 Eclipse SWT 中的 Shell。
 * 封装了 IntelliJ 中与顶层窗口相关的上下文信息，替代直接传递 {@link Project} 的模式。
 *
 * <p>提供两个核心概念：</p>
 * <ul>
 *   <li>{@link #getProject()} - IntelliJ 项目上下文，用于服务查找、进度任务等</li>
 *   <li>{@link #getParentComponent()} - 顶层窗口组件，用于对话框父级定位</li>
 * </ul>
 */
public class UIContext {
    @Nullable
    private final Project project;
    @Nullable
    private final Component parentComponent;

    /**
     * 仅使用 Project 创建上下文，parentComponent 从 Project 的主窗口派生。
     *
     * @param project the IntelliJ {@link Project} (may be <code>null</code>)
     */
    public UIContext(@Nullable final Project project) {
        this.project = project;
        this.parentComponent = null;
    }

    /**
     * 使用 Project 和显式的 parentComponent 创建上下文。
     *
     * @param project         the IntelliJ {@link Project} (may be <code>null</code>)
     * @param parentComponent the parent window component (may be <code>null</code>)
     */
    public UIContext(@Nullable final Project project, @Nullable final Component parentComponent) {
        this.project = project;
        this.parentComponent = parentComponent;
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
     * 获取用于对话框父级的最佳组件。
     * 优先使用显式设置的 parentComponent，否则从 project 获取主窗口框架。
     *
     * @return the best available parent component for dialogs, or <code>null</code>
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
}
