package com.github.lizhanyin.tfs.api.rest;

import com.github.lizhanyin.tfs.model.Changeset;
import com.github.lizhanyin.tfs.model.TfsBranch;
import com.github.lizhanyin.tfs.settings.TfsSettings;
import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * TFS 2015 REST API 客户端
 * API 版本: 2.0 (TFS 2015 最高支持)
 */
public class TfsRestClient {

    private static final Logger LOG = Logger.getInstance(TfsRestClient.class);
    private static final String API_VERSION = "2.0";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

    private final String collectionUrl;
    private final TfsSettings.AuthType authType;
    private final String username;
    private final String password;
    private final String domain;
    private final HttpClient httpClient;

    public TfsRestClient(@NotNull String collectionUrl,
                         @NotNull TfsSettings.AuthType authType,
                         @Nullable String username,
                         @Nullable String password,
                         @Nullable String domain) {
        this.collectionUrl = collectionUrl.replaceAll("/$", "");
        this.authType = authType;
        this.username = username != null ? username : "";
        this.password = password != null ? password : "";
        this.domain = domain != null ? domain : "";

        // 配置认证
        if (authType == TfsSettings.AuthType.NTLM) {
            Authenticator.setDefault(new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    String user = null;
                    if (domain != null) {
                        user = domain.isEmpty() ? TfsRestClient.this.username : domain + "\\" + TfsRestClient.this.username;
                    }
                    return new PasswordAuthentication(user, TfsRestClient.this.password.toCharArray());
                }
            });
        }

        // 创建 HTTP 客户端
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }

    /**
     * 获取变更集列表
     */
    public CompletableFuture<List<Changeset>> getChangesets(@Nullable String project,
                                                             @Nullable Integer fromId,
                                                             @Nullable Integer toId,
                                                             int top) {
        StringBuilder url = new StringBuilder(collectionUrl);
        url.append("/_apis/tfvc/changesets?api-version=").append(API_VERSION);
        url.append("&$top=").append(top);

        if (project != null && !project.isEmpty()) {
            url.append("&searchCriteria.project=").append(project);
        }
        if (fromId != null) {
            url.append("&searchCriteria.fromId=").append(fromId);
        }
        if (toId != null) {
            url.append("&searchCriteria.toId=").append(toId);
        }

        return sendRequest(url.toString())
                .thenApply(this::parseChangesets);
    }

    /**
     * 获取单个变更集
     */
    public CompletableFuture<Changeset> getChangeset(int changesetId) {
        String url = collectionUrl + "/_apis/tfvc/changesets/" + changesetId + "?api-version=" + API_VERSION;
        return sendRequest(url)
                .thenApply(this::parseChangeset);
    }

    /**
     * 获取变更集的文件变更
     */
    public CompletableFuture<List<Changeset.Change>> getChangesetChanges(int changesetId) {
        String url = collectionUrl + "/_apis/tfvc/changesets/" + changesetId + "/changes?api-version=" + API_VERSION;
        return sendRequest(url)
                .thenApply(this::parseChangesetChanges);
    }

    /**
     * 获取分支列表
     */
    public CompletableFuture<List<TfsBranch>> getBranches(@Nullable String project,
                                                          @Nullable String path) {
        StringBuilder url = new StringBuilder(collectionUrl);
        url.append("/_apis/tfvc/branches?api-version=").append(API_VERSION);

        if (project != null && !project.isEmpty()) {
            url.append("&project=").append(project);
        }
        if (path != null && !path.isEmpty()) {
            url.append("&path=").append(path);
        }

        return sendRequest(url.toString())
                .thenApply(this::parseBranches);
    }

    /**
     * 获取文件内容
     */
    public CompletableFuture<String> getFileContent(@NotNull String path,
                                                     @Nullable String version) {
        StringBuilder url = new StringBuilder(collectionUrl);
        url.append("/_apis/tfvc/items?path=").append(path);
        if (version != null && !version.isEmpty()) {
            url.append("&version=").append(version);
        }
        url.append("&api-version=").append(API_VERSION);

        return sendRequest(url.toString());
    }

    /**
     * 发送 HTTP 请求
     */
    private CompletableFuture<String> sendRequest(@NotNull String url) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofSeconds(30))
                        .header("Accept", "application/json");

                // 添加认证头
                if (authType == TfsSettings.AuthType.BASIC) {
                    String auth = Base64.getEncoder().encodeToString(
                            (username + ":" + password).getBytes());
                    requestBuilder.header("Authorization", "Basic " + auth);
                } else if (authType == TfsSettings.AuthType.PAT) {
                    requestBuilder.header("Authorization", "Bearer " + password);
                }

                HttpRequest request = requestBuilder.GET().build();
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    return response.body();
                } else {
                    throw new RuntimeException("HTTP request failed: " + response.statusCode() + " - " + response.body());
                }
            } catch (IOException | InterruptedException e) {
                LOG.error("HTTP request failed: " + url, e);
                throw new RuntimeException(e);
            }
        });
    }

    // ============== JSON 解析方法 (简化实现，实际应使用 Gson/Jackson) ==============

    private List<Changeset> parseChangesets(String json) {
        // TODO: 使用 JSON 库解析
        // 这里是简化实现
        List<Changeset> changesets = new ArrayList<>();
        // 解析 JSON 并创建 Changeset 对象
        return changesets;
    }

    private Changeset parseChangeset(String json) {
        // TODO: 使用 JSON 库解析
        return new Changeset(0, "", "", null, LocalDateTime.now(), null);
    }

    private List<Changeset.Change> parseChangesetChanges(String json) {
        // TODO: 使用 JSON 库解析
        return new ArrayList<>();
    }

    private List<TfsBranch> parseBranches(String json) {
        // TODO: 使用 JSON 库解析
        return new ArrayList<>();
    }
}
