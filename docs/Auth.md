# TFS 服务器连接认证流程分析

> 分析路径: `team-explorer-everywhere\source\com.github.lizhanyin.tfs.client.ui.ui`

---

## 1. 入口点 - 添加服务器向导

**文件**: `WizardServerSelectionPage.java:106-131`

当用户添加 TFS 服务器时，有两种认证路径：
- **VSTS (Azure DevOps Services)**: 托管服务器，使用 OAuth2 认证
- **On-Premises TFS**: 本地服务器，使用用户名/密码或 Windows 集成认证

```java
protected boolean onPageFinished() {
    if (serverTypeSelectControl.isVstsSelected()) {
        // VSTS: 使用 OAuth2 设备流程
        final List<Account> accounts = getUserAccounts(vstsCredentials, deviceFlowCallback);
        final List<TFSConnection> configurationServers = getConfigurationServers(...);
    } else {
        // 本地 TFS: 使用用户名/密码
        final TFSConnection connection = openAccount(serverURI, null);
    }
}
```

---

## 2. 连接命令执行

**文件**: `ConnectToConfigurationServerCommand.java:80-223`

核心连接逻辑：

```java
protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
    final ConnectionAdvisor connectionAdvisor = new UIClientConnectionAdvisor();

    // 1. 创建 TFSConfigurationServer 或 TFSTeamProjectCollection
    if (TFSConfigurationServer.class.equals(connectionData.getType())) {
        connection = new TFSConfigurationServer(connectionData.getURI(), credentials, connectionAdvisor);
    } else {
        connection = new TFSTeamProjectCollection(connectionData.getURI(), credentials, connectionAdvisor);
    }

    // 2. 执行认证
    connection.authenticate();
}
```

---

## 3. 客户端连接顾问

**文件**: `UIClientConnectionAdvisor.java:22-69`

配置 HTTP 客户端和 Web 服务工厂，注入 UI 认证处理器：

```java
public WebServiceFactory getWebServiceFactory(final ConnectionInstanceData instanceData) {
    return new DefaultWebServiceFactory(
        getLocale(instanceData),
        new UITransportRequestHandler(  // ← 关键：UI 认证处理器
            instanceData,
            (ConfigurableHTTPClientFactory) getHTTPClientFactory(instanceData)));
}
```

---

## 4. 传输请求处理器 - 核心认证逻辑

**文件**: `UITransportRequestHandler.java:52-445`

这是认证的核心处理类，处理三种认证场景：

### 4.1 准备请求 - 检查空密码
```java
public Status prepareRequest(...) {
    // 如果密码为空，弹出对话框让用户输入
    if (StringUtil.isNullOrEmpty(oldCredentials.getPassword())) {
        final Credentials newCredentials = getCredentials(
            new UITransportUsernamePasswordAuthRunnable(serverURI, credentials));
        connectionInstanceData.setCredentials(newCredentials);
    }
}
```

### 4.2 处理成功响应 - 保存 FedAuth Cookie
```java
public Status handleSuccess(...) {
    // 解析 Set-Cookie 头，提取 FedAuth cookie
    if (cookie.getName().startsWith("FedAuth")) {
        CookieCredentials newCredentials = new CookieCredentials(fedAuthCookies);
        connectionInstanceData.setCredentials(newCredentials);
        // 保存到 Eclipse 安全存储
        credentialsManager.setCredentials(new CachedCredentials(uri, newCredentials));
    }
}
```

### 4.3 处理认证异常 - 弹出认证对话框
```java
public Status handleException(...) {
    if (exception instanceof FederatedAuthException) {
        // 联合认证异常 → 使用 OAuth2
        dialogRunnable = new UITransportOAuthRunnable(serverURI);
    }
    else if (exception instanceof UnauthorizedException) {
        // 未授权异常 → 使用用户名/密码对话框
        dialogRunnable = new UITransportUsernamePasswordAuthRunnable(serverURI, credentials, exception);
    }

    final Credentials credentials = getCredentials(dialogRunnable);
    connectionInstanceData.setCredentials(credentials);
}
```

---

## 5. 认证对话框实现

### 5.1 用户名/密码对话框
**文件**: `CredentialsDialog.java:44-357`

- 支持两种凭据类型：
  - **用户名/密码** (`UsernamePasswordCredentials`)
  - **PAT (Personal Access Token)** (`PatCredentials`)
- 可选保存密码到 Eclipse 安全存储

### 5.2 OAuth2 认证
**文件**: `UITransportOAuthRunnable.java:26-141`

```java
protected CredentialsCompleteDialog getCredentialsDialog() {
    final OAuthCredentialsDialog credentialsDialog = new OAuthCredentialsDialog(shell, serverURI);
    credentialsDialog.open();
    // 使用 OAuth2 设备流程获取令牌
    credentials = CredentialsHelper.getOAuthCredentials(serverURI, null, deviceFlowCallback);
}
```

---

## 6. OAuth2 认证流程

**文件**: `CredentialsHelper.java:105-197`

```java
public static Credentials getOAuthCredentials(URI serverURI, JwtCredentials accessToken, Action<DeviceFlowResponse> callback) {
    // 1. 创建 OAuth2 认证器
    OAuth2Authenticator oauth2Authenticator = OAuth2Authenticator.getAuthenticator(CLIENT_ID, REDIRECT_URL, ...);

    // 2. 对于特定服务器，生成 PAT 令牌
    if (serverURI != null) {
        authenticator = new VstsPatAuthenticator(oauth2Authenticator, tokenStore);
        token = authenticator.getPersonalAccessToken(serverURI, VsoTokenScope.AllScopes, ...);
        return new PatCredentials(token.Value);
    }
    // 3. 对于 VSTS 全局，获取 OAuth2 访问令牌
    else {
        TokenPair tokenPair = authenticator.getOAuth2TokenPair();
        return new JwtCredentials(tokenPair.AccessToken.Value);
    }
}
```

---

## 认证流程图

```
用户添加服务器
      │
      ▼
WizardServerSelectionPage.onPageFinished()
      │
      ├─ VSTS/Azure DevOps ──► OAuth2DeviceFlow ──► PAT/JWT Credentials
      │                                              │
      └─ On-Premises TFS ──► 用户名/密码对话框 ──► UsernamePasswordCredentials
                                                     │
                              ConnectToConfigurationServerCommand.doRun()
                                                     │
                              new TFSConfigurationServer(uri, credentials, advisor)
                                                     │
                              connection.authenticate()
                                                     │
                              UITransportRequestHandler 处理认证异常
                                     │              │
                           FederatedAuthException  UnauthorizedException
                                     │              │
                              OAuth2对话框    用户名/密码对话框
                                     │              │
                              获取新凭据 ─────────────► 重试连接
                                     │
                              保存到 Eclipse 安全存储
```

---

## 关键凭据类型

| 类型 | 类 | 用途 |
|------|-----|------|
| `UsernamePasswordCredentials` | 用户名+密码 | 本地 TFS 基本认证 |
| `PatCredentials` | PAT 令牌 | Azure DevOps 个人访问令牌 |
| `JwtCredentials` | JWT 令牌 | OAuth2 访问令牌 |
| `CookieCredentials` | FedAuth Cookie | 联合认证后的会话 Cookie |
| `DefaultNTCredentials` | Windows 凭据 | Windows 集成认证 |

---

## 关键文件列表

| 文件路径 | 作用 |
|----------|------|
| `wizard/common/WizardServerSelectionPage.java` | 服务器选择页面入口 |
| `wizard/connectwizard/ConnectWizard.java` | 连接向导基类 |
| `commands/ConnectToConfigurationServerCommand.java` | 连接命令执行 |
| `config/UIClientConnectionAdvisor.java` | 客户端连接顾问 |
| `config/UITransportRequestHandler.java` | 传输请求处理器（核心） |
| `config/UITransportAuthRunnable.java` | 认证对话框基类 |
| `config/UITransportUsernamePasswordAuthRunnable.java` | 用户名密码认证 |
| `config/UITransportOAuthRunnable.java` | OAuth2 认证 |
| `dialogs/connect/CredentialsDialog.java` | 凭据对话框 UI |
| `helpers/CredentialsHelper.java` | OAuth2 认证辅助类 |
