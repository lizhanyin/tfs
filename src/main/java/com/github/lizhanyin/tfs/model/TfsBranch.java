package com.github.lizhanyin.tfs.model;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * TFS 分支模型
 */
public record TfsBranch(String path, String description, String owner, Status status, List<TfsBranch> children) {

    public TfsBranch(@NotNull String path,
                     @Nullable String description,
                     @Nullable String owner,
                     @NotNull Status status,
                     @Nullable List<TfsBranch> children) {
        this.path = path;
        this.description = description;
        this.owner = owner;
        this.status = status;
        this.children = children;
    }

    @Override
    @NotNull
    public String path() {
        return path;
    }

    @Override
    @Nullable
    public String description() {
        return description;
    }

    @Override
    @Nullable
    public String owner() {
        return owner;
    }

    @Override
    @NotNull
    public Status status() {
        return status;
    }

    @Override
    @Nullable
    public List<TfsBranch> children() {
        return children;
    }

    /**
     * 分支状态
     */
    public enum Status {
        ACTIVE("active"),
        CLOSED("closed"),
        MERGED("merged"),
        NEW("new");

        private final String value;

        Status(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static Status fromValue(String value) {
            for (Status status : values()) {
                if (status.value.equalsIgnoreCase(value)) {
                    return status;
                }
            }
            return ACTIVE;
        }
    }
}
