package com.github.lizhanyin.tfs;

import org.jetbrains.annotations.NotNull;

/**
 * TFS 扩展接口
 * 允许其他插件扩展 TFS 功能
 */
public interface TfsExtension {

    /**
     * 获取扩展名称
     */
    @NotNull
    String getName();

    /**
     * 扩展初始化时调用
     */
    default void initialize() {
        // 默认空实现
    }

    /**
     * 扩展卸载时调用
     */
    default void dispose() {
        // 默认空实现
    }
}
