package com.github.lizhanyin.tfs.api.command;

import com.github.lizhanyin.tfs.model.PendingChange;
import com.github.lizhanyin.tfs.model.TfsWorkspace;
import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * TFS 命令行客户端 (tf.exe 包装器)
 * 用于执行需要本地工作区的操作
 */
public class TfsCommandLineClient {

    private static final Logger LOG = Logger.getInstance(TfsCommandLineClient.class);

    private static final Pattern WORKSPACE_PATTERN = Pattern.compile(
            "Workspace:\\s*(.+?)\\s*\\((.+?)\\)");
    private static final Pattern COLLECTION_PATTERN = Pattern.compile(
            "Collection:\\s*(.+)");
    private static final Pattern MAPPING_PATTERN = Pattern.compile(
            "(\\$/.+?):\\s*(.+)");

    private final String tfExePath;
    private final String workspacePath;

    public TfsCommandLineClient(@Nullable String tfExePath, @Nullable String workspacePath) {
        this.tfExePath = findTfExe(tfExePath);
        this.workspacePath = workspacePath != null ? workspacePath : System.getProperty("user.dir");
    }

    /**
     * 查找 tf.exe 路径
     */
    private String findTfExe(@Nullable String customPath) {
        if (customPath != null && !customPath.isEmpty() && Files.exists(Paths.get(customPath))) {
            return customPath;
        }

        // 常见安装路径
        String[] commonPaths = {
                "C:\\Program Files\\Microsoft Visual Studio\\2022\\Enterprise\\Common7\\IDE\\CommonExtensions\\Microsoft\\TeamFoundation\\Team Explorer\\tf.exe",
                "C:\\Program Files\\Microsoft Visual Studio\\2022\\Professional\\Common7\\IDE\\CommonExtensions\\Microsoft\\TeamFoundation\\Team Explorer\\tf.exe",
                "C:\\Program Files\\Microsoft Visual Studio\\2022\\Community\\Common7\\IDE\\CommonExtensions\\Microsoft\\TeamFoundation\\Team Explorer\\tf.exe",
                "C:\\Program Files (x86)\\Microsoft Visual Studio\\2019\\Enterprise\\Common7\\IDE\\CommonExtensions\\Microsoft\\TeamFoundation\\Team Explorer\\tf.exe",
                "C:\\Program Files (x86)\\Microsoft Visual Studio\\2019\\Professional\\Common7\\IDE\\CommonExtensions\\Microsoft\\TeamFoundation\\Team Explorer\\tf.exe",
                "C:\\Program Files (x86)\\Microsoft Visual Studio\\2017\\Enterprise\\Common7\\IDE\\CommonExtensions\\Microsoft\\TeamFoundation\\Team Explorer\\tf.exe",
                "C:\\Program Files (x86)\\Microsoft Visual Studio 14.0\\Common7\\IDE\\tf.exe",
                "C:\\Program Files\\Microsoft Team Foundation Server 2015 Tools\\tf.exe"
        };

        for (String path : commonPaths) {
            if (Files.exists(Paths.get(path))) {
                return path;
            }
        }

        // 尝试从 PATH 环境变量中查找
        return "tf.exe";
    }

    /**
     * 获取工作区信息
     */
    @Nullable
    public TfsWorkspace getWorkspaceInfo() throws IOException, InterruptedException {
        CommandResult result = executeCommand("workfold", List.of(workspacePath));
        if (!result.isSuccess()) {
            return null;
        }
        return parseWorkspaceInfo(result.getOutput());
    }

    /**
     * 获取待提交的变更
     */
    public CompletableFuture<List<PendingChange>> getPendingChanges() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                CommandResult result = executeCommand("status", List.of(
                        "/recursive",
                        "/format:detailed",
                        workspacePath
                ));
                if (result.isSuccess()) {
                    return parsePendingChanges(result.getOutput());
                }
                return new ArrayList<>();
            } catch (Exception e) {
                LOG.error("Failed to get pending changes", e);
                return new ArrayList<>();
            }
        });
    }

    /**
     * 检出文件
     */
    public CompletableFuture<Boolean> checkout(@NotNull List<String> filePaths, boolean recursive) {
        List<String> args = new ArrayList<>();
        args.add("/noprompt");
        if (recursive) {
            args.add("/recursive");
        }
        args.addAll(filePaths);

        return executeCommandAsync("checkout", args)
                .thenApply(CommandResult::isSuccess);
    }

    /**
     * 提交变更
     */
    public CompletableFuture<Boolean> checkin(@NotNull List<String> filePaths,
                                               @NotNull String comment,
                                               boolean recursive) {
        List<String> args = new ArrayList<>();
        args.add("/comment:" + comment);
        args.add("/noprompt");
        if (recursive) {
            args.add("/recursive");
        }
        args.addAll(filePaths);

        return executeCommandAsync("checkin", args)
                .thenApply(CommandResult::isSuccess);
    }

    /**
     * 获取最新版本
     */
    public CompletableFuture<Boolean> getLatest(@NotNull String path, boolean recursive) {
        List<String> args = new ArrayList<>();
        args.add("/noprompt");
        if (recursive) {
            args.add("/recursive");
        }
        args.add(path);

        return executeCommandAsync("get", args)
                .thenApply(CommandResult::isSuccess);
    }

    /**
     * 撤销变更
     */
    public CompletableFuture<Boolean> undo(@NotNull List<String> filePaths, boolean recursive) {
        List<String> args = new ArrayList<>();
        args.add("/noprompt");
        if (recursive) {
            args.add("/recursive");
        }
        args.addAll(filePaths);

        return executeCommandAsync("undo", args)
                .thenApply(CommandResult::isSuccess);
    }

    /**
     * 获取历史记录
     */
    public CompletableFuture<String> getHistory(@NotNull String path,
                                                 boolean recursive,
                                                 int limit) {
        List<String> args = new ArrayList<>();
        args.add("/format:detailed");
        args.add("/noprompt");
        if (recursive) {
            args.add("/recursive");
        }
        args.add("/stopafter:" + limit);
        args.add(path);

        return executeCommandAsync("history", args)
                .thenApply(CommandResult::getOutput);
    }

    /**
     * 添加文件
     */
    public CompletableFuture<Boolean> add(@NotNull List<String> filePaths, boolean recursive) {
        List<String> args = new ArrayList<>();
        args.add("/noprompt");
        if (recursive) {
            args.add("/recursive");
        }
        args.addAll(filePaths);

        return executeCommandAsync("add", args)
                .thenApply(CommandResult::isSuccess);
    }

    /**
     * 删除文件
     */
    public CompletableFuture<Boolean> delete(@NotNull List<String> filePaths) {
        List<String> args = new ArrayList<>();
        args.add("/noprompt");
        args.addAll(filePaths);

        return executeCommandAsync("delete", args)
                .thenApply(CommandResult::isSuccess);
    }

    /**
     * 执行命令
     */
    private CommandResult executeCommand(@NotNull String command,
                                          @NotNull List<String> args) throws IOException, InterruptedException {
        List<String> commandLine = new ArrayList<>();
        commandLine.add(tfExePath);
        commandLine.add(command);
        commandLine.addAll(args);

        ProcessBuilder pb = new ProcessBuilder(commandLine);
        pb.directory(Paths.get(workspacePath).toFile());
        pb.redirectErrorStream(true);

        Process process = pb.start();
        StringBuilder output = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }

        int exitCode = process.waitFor();
        return new CommandResult(exitCode, output.toString());
    }

    /**
     * 异步执行命令
     */
    private CompletableFuture<CommandResult> executeCommandAsync(@NotNull String command,
                                                                  @NotNull List<String> args) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return executeCommand(command, args);
            } catch (Exception e) {
                LOG.error("Command execution failed: " + command, e);
                return new CommandResult(-1, e.getMessage());
            }
        });
    }

    // ============== 解析方法 ==============

    private TfsWorkspace parseWorkspaceInfo(String output) {
        String name = "";
        String owner = "";
        String collectionUrl = "";
        List<TfsWorkspace.WorkingFolder> folders = new ArrayList<>();

        for (String line : output.split("\n")) {
            line = line.trim();

            Matcher workspaceMatcher = WORKSPACE_PATTERN.matcher(line);
            if (workspaceMatcher.find()) {
                name = workspaceMatcher.group(1).trim();
                owner = workspaceMatcher.group(2).trim();
                continue;
            }

            Matcher collectionMatcher = COLLECTION_PATTERN.matcher(line);
            if (collectionMatcher.find()) {
                collectionUrl = collectionMatcher.group(1).trim();
                continue;
            }

            Matcher mappingMatcher = MAPPING_PATTERN.matcher(line);
            if (mappingMatcher.find()) {
                String serverPath = mappingMatcher.group(1).trim();
                String localPath = mappingMatcher.group(2).trim();
                folders.add(new TfsWorkspace.WorkingFolder(serverPath, localPath));
            }
        }

        if (name.isEmpty()) {
            return null;
        }

        // 从 URL 提取集合名称
        String collectionName = "";
        if (!collectionUrl.isEmpty()) {
            int lastSlash = collectionUrl.lastIndexOf('/');
            if (lastSlash >= 0) {
                collectionName = collectionUrl.substring(lastSlash + 1);
            }
        }

        return new TfsWorkspace(name, owner, collectionUrl, collectionName, folders);
    }

    private List<PendingChange> parsePendingChanges(String output) {
        List<PendingChange> changes = new ArrayList<>();
        // TODO: 解析 tf status 输出
        // 格式示例:
        // edit    $/Project/file.cs    C:\Project\file.cs
        return changes;
    }

    /**
     * 命令执行结果
     */
    public static class CommandResult {
        private final int exitCode;
        private final String output;

        public CommandResult(int exitCode, String output) {
            this.exitCode = exitCode;
            this.output = output;
        }

        public boolean isSuccess() {
            return exitCode == 0;
        }

        public int getExitCode() {
            return exitCode;
        }

        public String getOutput() {
            return output;
        }
    }
}
