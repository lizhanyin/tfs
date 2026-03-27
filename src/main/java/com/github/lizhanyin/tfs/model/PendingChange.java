package com.github.lizhanyin.tfs.model;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * TFS 待提交变更模型
 */
public record PendingChange(String serverItem, String localItem, ChangeType changeType, String version, String lock,
                            String lockOwner) {

    public PendingChange(@NotNull String serverItem,
                         @Nullable String localItem,
                         @NotNull ChangeType changeType,
                         @Nullable String version,
                         @Nullable String lock,
                         @Nullable String lockOwner) {
        this.serverItem = serverItem;
        this.localItem = localItem;
        this.changeType = changeType;
        this.version = version;
        this.lock = lock;
        this.lockOwner = lockOwner;
    }

    @Override
    @NotNull
    public String serverItem() {
        return serverItem;
    }

    @Override
    @Nullable
    public String localItem() {
        return localItem;
    }

    @Override
    @NotNull
    public ChangeType changeType() {
        return changeType;
    }

    @Override
    @Nullable
    public String version() {
        return version;
    }

    @Override
    @Nullable
    public String lock() {
        return lock;
    }

    @Override
    @Nullable
    public String lockOwner() {
        return lockOwner;
    }

    /**
     * 变更类型枚举 (使用 Changeset.ChangeType)
     */
    public enum ChangeType {
        ADD("add"),
        EDIT("edit"),
        DELETE("delete"),
        RENAME("rename"),
        UNDELETE("undelete"),
        BRANCH("branch"),
        LOCK("lock"),
        NONE("none");

        private final String value;

        ChangeType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static ChangeType fromValue(String value) {
            for (ChangeType type : values()) {
                if (type.value.equalsIgnoreCase(value)) {
                    return type;
                }
            }
            return NONE;
        }
    }
}
