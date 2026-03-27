# Microsoft TFS SDK for Java (14.139.1)

Microsoft Team Foundation Server 软件开发工具包 (Java 版本)，用于在 Java 应用程序中访问 Team Foundation 功能。

## 环境要求

- Java 6 或更高版本

## 主要功能

### 1. 工作项管理 (Work Item Tracking)

| 功能 | 说明 |
|------|------|
| 创建工作项 | 通过 API 创建新的工作项 |
| 编辑工作项 | 修改现有工作项内容 |
| 链接管理 | 添加外部链接、关联链接到工作项 |
| 查询功能 | 运行 WIQL 查询获取工作项 |
| 元数据枚举 | 枚举工作项类别、字段、类型、团队项目 |

**相关示例:**
- `CreateWorkItem.java` - 创建工作项
- `EditWorkItemByID.java` - 按 ID 编辑工作项
- `AddExternalLinkToWorkItem.java` - 添加外部链接
- `AddRelatedLinkToWorkItem.java` - 添加关联链接
- `RunWorkItemQuery.java` - 运行工作项查询

### 2. 构建系统 (Build Automation)

| 功能 | 说明 |
|------|------|
| 构建定义管理 | 创建、编辑构建定义 |
| 构建查询 | 查找构建定义和构建结果 |
| 构建队列 | 将构建加入队列执行 |
| 私有构建 | 执行私有构建 (Private Build) |
| 门控构建 | 执行门控签入构建 (Gated Build) |
| 构建质量 | 添加、移除、枚举构建质量 |
| 构建资源 | 查找构建控制器和代理 |

**相关示例:**
- `CreateBuildDefinition.java` - 创建构建定义
- `EditBuildDefinition.java` - 编辑构建定义
- `FindBuildDefinitions.java` - 查找构建定义
- `FindBuilds.java` - 查找构建
- `QueueBuild.java` - 队列构建
- `QueuePrivateBuild.java` - 私有构建
- `GatedBuildSample.java` - 门控构建
- `AddRemoveBuildQuality.java` - 管理构建质量

### 3. 版本控制 (Version Control)

| 功能 | 说明 |
|------|------|
| 文件操作 | 获取 (Get) 文件和目录 |
| 事件监听 | 监听版本控制事件 |
| 操作跟踪 | 跟踪获取操作的状态 |

**相关示例:**
- `VersionControlSample.java` - 版本控制示例
- `SampleGetEventListener.java` - 获取事件监听器
- `SampleGetOperationStartedListener.java` - 操作开始监听器
- `SampleGetOperationCompletedListener.java` - 操作完成监听器

### 4. 团队项目管理 (Team Project Management)

| 功能 | 说明 |
|------|------|
| 项目集合 | 枚举团队项目集合 |
| 区域路径 | 创建和使用区域路径 |

**相关示例:**
- `EnumerateTeamProjectCollections.java` - 枚举项目集合
- `CreateUseAreaPath.java` - 区域路径管理

### 5. 认证与连接 (Authentication & Connection)

| 功能 | 说明 |
|------|------|
| 服务器连接 | 连接到 Team Foundation Server |
| HTTP 代理 | 支持通过 HTTP 代理连接 |
| 单点登录 | 集成 Windows 身份验证 |
| 凭据管理 | Windows 凭据管理器集成 |
| 连接配置 | 自定义连接行为 |

**相关示例:**
- `ConnectionAdvisorSample.java` - 连接配置示例
- `WinCredentialsManagerSample.java` - 凭据管理

### 6. 扩展能力 (Extensibility)

#### 6.1 自定义签入策略 (Check-in Policy)

在文件签入到 TFS 前执行自定义规则验证。

**相关示例:**
- `SamplePolicy.java` - 自定义签入策略示例

#### 6.2 自定义工作项控件 (Work Item Controls)

扩展 Team Explorer Everywhere 工作项编辑器的 UI 控件。

**相关示例:**
- `ExternalSourceDropdown.java` - 外部数据源下拉框
- `FileSourceDropdown.java` - 文件数据源下拉框
- `RadioButtonsControl.java` - 单选按钮控件
- `SimpleButtonControl.java` - 简单按钮控件

#### 6.3 Team Explorer 扩展 (Team Explorer Extension)

通过 Eclipse 扩展点扩展 Team Explorer Everywhere。

**相关示例:**
- `TeamExplorerSampleNavigationItem.java` - 自定义导航项
- `TeamExplorerSampleNavigationLink.java` - 自定义导航链接
- `TeamExplorerSamplePage.java` - 自定义页面
- `TeamExplorerSampleView.java` - 自定义视图
- `TeamExplorerSampleSection*.java` - 自定义部分

### 7. 其他功能

| 功能 | 说明 |
|------|------|
| 日志配置 | 自定义日志记录行为 |
| 区域设置 | 指定 Locale 和 TimeZone |
| User-Agent | 自定义 HTTP 请求头 |

**相关示例:**
- `LogConfigurationSample.java` - 日志配置

---

## SDK 目录结构

```
TFS-SDK-14.139.1/
├── readme.html              # SDK 说明文档
├── license.html             # 许可证
├── redist/                  # 可再分发文件
│   ├── lib/                 # JAR 文件
│   │   └── com.microsoft.tfs.sdk-14.139.1.jar
│   └── native/              # 原生库文件
│       ├── win32/           # Windows (x86, x86_64)
│       ├── linux/           # Linux (arm, ppc, x86, x86_64)
│       ├── macosx/          # macOS
│       ├── solaris/         # Solaris
│       ├── aix/             # AIX
│       ├── hpux/            # HP-UX
│       └── freebsd/         # FreeBSD
└── samples/                 # 示例代码
    ├── com.microsoft.tfs.sdk.samples.snippets/      # 基础代码片段
    ├── com.microsoft.tfs.sdk.samples.console/       # 控制台应用示例
    ├── com.microsoft.tfs.sdk.samples.checkinpolicy/ # 签入策略示例
    ├── com.microsoft.tfs.sdk.samples.witcontrols/   # 工作项控件示例
    └── com.microsoft.tfs.sdk.samples.teamexplorer/  # Team Explorer 扩展示例
```

---

## 快速开始

### 1. 配置项目

将以下文件添加到项目中:
- `redist/lib/com.microsoft.tfs.sdk-14.139.1.jar` - 添加到 classpath
- `redist/native/` - 整个目录复制到项目

### 2. 设置原生库路径

启动 JVM 时设置系统属性:

**Linux/macOS:**
```bash
java -D"com.microsoft.tfs.jni.native.base-directory=/path/to/native" -cp "com.microsoft.tfs.sdk-14.139.1.jar:." YourApp
```

**Windows:**
```cmd
java -D"com.microsoft.tfs.jni.native.base-directory=C:\path\to\native" -cp "com.microsoft.tfs.sdk-14.139.1.jar;." YourApp
```

### 3. 基本连接示例

```java
import com.microsoft.tfs.core.TFSConfigurationServer;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.httpclient.Credentials;
import com.microsoft.tfs.core.httpclient.UsernamePasswordCredentials;

// 连接到 TFS
String url = "https://your-tfs-server:8080/tfs";
Credentials credentials = new UsernamePasswordCredentials("username", "password");
TFSTeamProjectCollection tpc = new TFSTeamProjectCollection(url, credentials);

// 获取工作项客户端
WorkItemClient workItemClient = tpc.getWorkItemClient();
```

---

## 使用场景

| 场景 | 描述 |
|------|------|
| **独立 Java 应用** | 直接使用客户端对象模型访问 TFS 功能 |
| **Eclipse 插件开发** | 扩展 Team Explorer Everywhere 功能 |
| **CI/CD 集成** | 自动化构建管理、工作项处理 |
| **自定义工具** | 开发 TFS 相关的命令行或桌面工具 |
| **企业集成** | 与其他系统集成 (如 JIRA、Jenkins) |

---

## 支持的平台

| 操作系统 | 架构 |
|----------|------|
| Windows | x86, x86_64 |
| Linux | arm, ppc, x86, x86_64 |
| macOS | 通用 |
| Solaris | sparc, x86, x86_64 |
| AIX | ppc |
| HP-UX | ia64_32, PA_RISC |
| FreeBSD | x86, x86_64 |

---

## SDK 构成分析 (基于构建配置)

本文档基于 Team Explorer Everywhere Ant 构建配置文件分析整理。

### 核心 JAR 文件组成

SDK 的主 JAR (`com.microsoft.tfs.sdk-14.139.1.jar`) 由以下组件合并而成：

#### TEE 核心组件

| 组件 | 说明 |
|------|------|
| `com.microsoft.tfs.console` | 控制台支持 |
| `com.microsoft.tfs.core` | TFS 核心功能库 |
| `com.microsoft.tfs.core.httpclient` | HTTP 客户端 |
| `com.microsoft.tfs.core.ws` | Web Service 支持 |
| `com.microsoft.tfs.core.ws.runtime` | WS 运行时 |
| `com.microsoft.tfs.jni` | JNI 本地接口 |
| `com.microsoft.tfs.logging` | 日志功能 |
| `com.microsoft.tfs.util` | 工具类库 |

#### 第三方依赖库

| 库 | 版本 | 用途 |
|----|------|------|
| `annotation-detector` | 3.0.4 | 注解检测 |
| `commons-codec` | 1.6 | 编码/解码 |
| `commons-io` | 2.4 | IO 操作 |
| `commons-lang3` | 3.1 | 通用工具 |
| `commons-logging` | 1.2 | 日志接口 |
| `guava` | 12.0.1 | Google 核心库 |
| `hsqldb` | - | HSQL 数据库 |
| `httpclient` | 4.3.1 | HTTP 客户端 |
| `httpcore` | 4.3 | HTTP 核心 |
| `jackson-annotations` | 2.7.9 | JSON 注解 |
| `jackson-core` | 2.7.9 | JSON 核心 |
| `jackson-databind` | 2.7.9.7 | JSON 数据绑定 |
| `jsr305` | 1.3.9 | JSR-305 注解 |
| `log4j-api` | 2.3.2 | Log4j API |
| `log4j-core` | 2.3.2 | Log4j 核心 |

### 原生库详情

SDK 原生库用于以下功能：

- 单点登录认证
- 文件系统属性修改
- 控制台测量
- 进程环境信息读取

#### 原生库文件列表

**Windows (x86, x86_64):**
- `native_filesystem.dll`
- `native_synchronization.dll`
- `native_messagewindow.dll`
- `native_credential.dll`
- `native_auth.dll`
- `native_console.dll`
- `native_misc.dll`
- `native_registry.dll`

**Linux (x86, x86_64, ARM, PPC):**
- `libnative_auth.so`
- `libnative_filesystem.so`
- `libnative_synchronization.so`
- `libnative_misc.so`
- `libnative_console.so`

**macOS (x86_64):**
- `libnative_misc.jnilib`
- `libnative_keychain.jnilib`
- `libnative_synchronization.jnilib`
- `libnative_auth.jnilib`
- `libnative_console.jnilib`
- `libnative_filesystem.jnilib`

**其他平台:**
- AIX: `.a` 文件
- FreeBSD: `.so` 文件
- HP-UX: `.sl` / `.so` 文件
- Solaris: `.so` 文件

### 公共 API 包

Javadoc 包含以下公共 API 包：

- `com.microsoft.tfs.core.*`
- `com.microsoft.tfs.util.locking.*`
- `com.microsoft.tfs.util.shutdown.*`
- `com.microsoft.tfs.util.tasks.*`
- `com.microsoft.tfs.util.temp.*`

> **注意**: 所有 `*.internal.*` 包被排除在外，不作为公共 API。

### 可再分发文件

根据 `redist/redist.txt`，以下文件可按 SDK 许可条款再分发：

- `ThirdPartyNotices.html`
- `lib/com.microsoft.tfs.sdk-14.139.1.jar`
- `native/` 目录下的所有原生库文件

---

## 相关资源

- [官方文档](docs/javadoc/index.html) - API 参考文档
- [示例代码](samples/) - 完整示例项目
- [许可证](license.html) - 软件许可条款
- [GitHub 仓库](https://github.com/Microsoft/team-explorer-everywhere) - 源代码
