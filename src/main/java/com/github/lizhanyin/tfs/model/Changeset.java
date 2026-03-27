package com.github.lizhanyin.tfs.model;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.List;

/**
 * TFS 变更集模型
 */
public record Changeset(int id, String comment, String author, String authorDisplayName, LocalDateTime createdDate,
                        List<Change> changes) {

    public Changeset(int id,
                     @Nullable String comment,
                     @NotNull String author,
                     @Nullable String authorDisplayName,
                     @NotNull LocalDateTime createdDate,
                     @Nullable List<Change> changes) {
        this.id = id;
        this.comment = comment;
        this.author = author;
        this.authorDisplayName = authorDisplayName;
        this.createdDate = createdDate;
        this.changes = changes;
    }

    @Override
    @Nullable
    public String comment() {
        return comment;
    }

    @Override
    @NotNull
    public String author() {
        return author;
    }

    @Override
    @Nullable
    public String authorDisplayName() {
        return authorDisplayName;
    }

    @Override
    @NotNull
    public LocalDateTime createdDate() {
        return createdDate;
    }

    @Override
    @Nullable
    public List<Change> changes() {
        return changes;
    }

    /**
     * 文件变更
     */
    public record Change(ChangeType changeType, String serverItem, String localItem) {
        public Change(@NotNull ChangeType changeType,
                      @NotNull String serverItem,
                      @Nullable String localItem) {
            this.changeType = changeType;
            this.serverItem = serverItem;
            this.localItem = localItem;
        }

        @Override
        @NotNull
        public ChangeType changeType() {
            return changeType;
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
    }

    /**
     * 变更类型
     */
    public enum ChangeType {
        ADD("add"),
        EDIT("edit"),
        DELETE("delete"),
        RENAME("rename"),
        MERGE("merge"),
        UNDELETE("undelete"),
        BRANCH("branch"),
        ROLLBACK("rollback"),
        SOURCE_RENAME("sourceRename"),
        ENCODING("encoding"),
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
