# Team Explorer Everywhere - Eclipse 插件 UI 分析

## 概述

Team Explorer Everywhere (TEE) 是一个 Eclipse 插件，提供与 Team Foundation Server (TFS) / Azure DevOps 的集成。本文档分析其 UI 源代码结构。

**UI 相关 Java 文件总数**: 约 1451 个

---

## UI 模块划分

### 1. com.microsoft.tfs.client.common.ui (核心 UI 模块)

**路径**: `source/com.microsoft.tfs.client.common.ui/src/com/microsoft/tfs/client/common/ui`

**文件数量**: 1046 个 Java 文件

**主要功能**: 提供 TFS 客户端的通用 UI 组件，是最大的 UI 模块

#### 子目录结构:

| 目录 | 功能描述 |
|------|----------|
| `adapters` | 适配器类，用于 Eclipse 平台适配 |
| `autoconnect` | 自动连接功能 |
| `browser` | 浏览器集成组件 |
| `buildmanager` | 构建管理器 UI |
| `commands` | 命令处理 (包括 annotate, search) |
| `compare` | 文件比较功能 |
| `config` | 配置相关 UI |
| `conflicts` | 冲突解决 UI (包含 resolutions, contributors) |
| `console` | 控制台视图 |
| `controls` | 自定义控件 (详见下方) |
| `diagnostics` | 诊断工具 UI |
| `dialogs` | 对话框 (详见下方) |
| `editors` | 编辑器 |
| `framework` | UI 框架基础类 (详见下方) |
| `help` | 帮助系统集成 |
| `helpers` | 辅助工具类 |
| `prefs` | 首选项/设置页面 |
| `productplugin` | 产品插件相关 |
| `protocolhandler` | 协议处理器 |
| `tasks` | 任务相关 |
| `teamexplorer` | Team Explorer 视图 (详见下方) |
| `vc` | 版本控制 UI (详见下方) |
| `views` | 视图组件 |
| `viewstate` | 视图状态管理 |
| `webaccessintegration` | Web 访问集成 |
| `wit` | 工作项跟踪 UI (详见下方) |
| `wizard` | 向导组件 |

#### controls 子目录 (自定义控件):

| 目录 | 描述 |
|------|------|
| `connect` | 连接相关控件 |
| `eula` | EULA (最终用户许可协议) 控件 |
| `generic` | 通用控件 (包含 compatibility, datepicker, html, menubutton) |
| `teamexplorer` | Team Explorer 控件 |
| `vc` | 版本控制控件 (changes, checkin, file, folder, history, properties) |
| `wit` | 工作项控件 |
| `workspaces` | 工作区控件 |

**主要控件文件**:
- `CSSNodeComboControl.java` - CSS 节点组合控件
- `CommonStructureControl.java` - 通用结构控件
- `RegularExpressionTable.java` - 正则表达式表格
- `TeamProjectTable.java` - 团队项目表格

#### dialogs 子目录 (对话框):

| 目录 | 描述 |
|------|------|
| `connect` | 连接对话框 (包含 resources) |
| `css` | CSS (公共结构服务) 对话框 (包含 actions) |
| `generic` | 通用对话框 |
| `prefs` | 首选项对话框 |
| `vc` | 版本控制对话框 (candidates, checkinpolicies) |
| `wit` | 工作项对话框 |
| `workspaces` | 工作区对话框 |

**对话框总数**: 90+ 个

#### teamexplorer 子目录 (Team Explorer 视图):

| 目录 | 描述 |
|------|------|
| `actions` | Team Explorer 操作 |
| `events` | 事件处理 |
| `favorites` | 收藏夹功能 |
| `helpers` | 辅助工具 |
| `internal` | 内部实现 |
| `items` | Team Explorer 项目 |
| `link` | 链接功能 |
| `pages` | 页面组件 |
| `sections` | 区块组件 |

**核心文件**:
- `TeamExplorerContext.java` - Team Explorer 上下文
- `TeamExplorerNavigator.java` - 导航器
- `TeamExplorerEvents.java` - 事件管理

#### wit 子目录 (工作项跟踪):

| 目录 | 描述 |
|------|------|
| `controls` | 工作项控件 |
| `dialogs` | 工作项对话框 |
| `form` | 表单组件 |
| `qe` | 查询编辑器 |
| `query` | 查询相关 |
| `results` | 查询结果 |

**核心文件**:
- `WITSearchModel.java` - 工作项搜索模型
- `StoredQueryGroup.java` - 存储查询组

#### vc 子目录 (版本控制):

| 目录 | 描述 |
|------|------|
| `branch` | 分支管理 |
| `serveritem` | 服务器项目 |
| `tfsitem` | TFS 项目 |

#### views (视图组件):

- `TeamExplorerView.java` - Team Explorer 主视图
- `TeamExplorerPendingChangesView.java` - 挂起更改视图
- `PendingChangesView.java` - 挂起更改视图
- `ChangesetDetailsView.java` - 变更集详情视图
- `ShelvesetDetailsView.java` - 搁置集详情视图
- `HistoryEditor.java` - 历史记录编辑器

#### framework 子目录 (UI 框架):

| 目录 | 描述 |
|------|------|
| `action` | 操作框架 (包含 keybinding) |
| `celleditor` | 单元格编辑器 (包含 accessibility) |
| `command` | 命令框架 |
| `compare` | 比较框架 (包含 internal) |
| `console` | 控制台框架 |
| `diagnostics` | 诊断框架 (builtin, cache, data, export, extend, ui) |
| `dialog` | 对话框框架 |
| `form` | 表单框架 |
| `help` | 帮助框架 |
| `helper` | 辅助工具 |
| `image` | 图像处理 |
| `launcher` | 启动器 |
| `layout` | 布局管理 |
| `preferences` | 首选项框架 |
| `runnable` | 可运行任务 |
| `selection` | 选择管理 |
| `sizing` | 尺寸管理 |
| `table` | 表格组件 (包含 tooltip) |
| `tooltip` | 工具提示 |
| `tree` | 树形组件 |
| `validation` | 验证框架 |
| `viewer` | 查看器 |
| `wizard` | 向导框架 |

#### wizard 子目录 (向导):

| 目录 | 描述 |
|------|------|
| `common` | 通用向导 |
| `connectwizard` | 连接向导 |
| `eula` | EULA 向导 |
| `merge` | 合并向导 |
| `teamprojectwizard` | 团队项目向导 |

---

### 2. com.microsoft.tfs.client.eclipse.ui (Eclipse 集成 UI)

**路径**: `source/com.microsoft.tfs.client.eclipse.ui/src/com/microsoft/tfs/client/eclipse/ui`

**文件数量**: 140 个 Java 文件

**主要功能**: 提供 Eclipse IDE 特定的 UI 集成

#### 子目录结构:

| 目录 | 功能描述 |
|------|----------|
| `actions` | Eclipse 操作 (sync, vc, workbench) |
| `adapters` | Eclipse 适配器 |
| `commands` | Eclipse 命令 |
| `connectionconflict` | 连接冲突处理 |
| `controls` | Eclipse 控件 |
| `decorators` | 装饰器 (项目图标装饰) |
| `dialogs` | Eclipse 对话框 (6 个) |
| `filemodification` | 文件修改处理 |
| `history` | 历史记录 |
| `license` | 许可证 UI |
| `offline` | 离线模式 |
| `prefs` | 首选项 |
| `productplugin` | 产品插件 |
| `project` | 项目管理 |
| `projectcreation` | 项目创建 |
| `propertypages` | 属性页面 |
| `propertysources` | 属性源 |
| `resourcechange` | 资源变更 |
| `sync` | 同步功能 |
| `tasks` | 任务 |
| `viewer` | 查看器 |
| `wizard` | 向导 |

**核心文件**:
- `TFSEclipseClientUIPlugin.java` - Eclipse 客户端 UI 插件主类
- `EclipseAutoConnector.java` - Eclipse 自动连接器

---

### 3. com.microsoft.tfs.client.common.ui.teambuild (团队构建 UI)

**路径**: `source/com.microsoft.tfs.client.common.ui.teambuild/src/com/microsoft/tfs/client/common/ui/teambuild`

**文件数量**: 150 个 Java 文件

**主要功能**: 提供团队构建功能的 UI

#### 子目录结构:

| 目录 | 功能描述 |
|------|----------|
| `actions` | 构建操作 (删除、编辑、管理等) |
| `adapters` | 适配器 |
| `buildstatus` | 构建状态 |
| `commands` | 构建命令 |
| `controls` | 构建控件 (builddefinition, ToolStripTabs) |
| `dialogs` | 构建对话框 (12 个) |
| `editors` | 构建编辑器 |
| `enums` | 枚举类型 |
| `git` | Git 相关 |
| `prefs` | 首选项 |
| `teamexplorer` | Team Explorer 集成 |
| `wizards` | 构建向导 |

**主要 Actions**:
- `DeleteBuildAction.java`
- `EditBuildDefinitionFromDetailsAction.java`
- `EditBuildQualityAction.java`
- `ManageBuildQualitiesAction.java`
- `OpenDropFolderAction.java`
- `PinBuildAction.java`
- `PostponeBuildAction.java`
- `QueueBuildCommand.java`
- `StopBuildAction.java`
- `ViewBuildReportAction.java`

**主要 Dialogs**:
- `BuildDefinitionDialog.java`
- `BuildDefinitionTemplateSelectionDialog.java`
- `BuildStatusNotificationDialog.java`
- `DeleteBuildsDialog.java`
- `ManageBuildQualitiesDialog.java`
- `QueueBuildDialog.java`

---

### 4. com.microsoft.tfs.client.common.ui.teambuild.egit (EGit 构建集成)

**路径**: `source/com.microsoft.tfs.client.common.ui.teambuild.egit/src/com/microsoft/tfs/client/common/ui/teambuild/egit`

**主要功能**: 提供与 EGit (Eclipse Git 插件) 的构建集成

#### 子目录结构:

| 目录 | 功能描述 |
|------|----------|
| `dialogs` | Git 构建对话框 |
| `repositories` | Git 仓库 (GitBranch, GitFolder, GitRepository) |
| `serveritem` | 服务器项目 (GitFolderSource) |

**主要文件**:
- `GitBuildDefinitionDialog.java`
- `GitProjectFileControl.java`
- `GitSourceSettingsControl.java`

---

### 5. com.microsoft.tfs.client.common.ui.vcexplorer (版本控制资源管理器)

**路径**: `source/com.microsoft.tfs.client.common.ui.vcexplorer/src/com/microsoft/tfs/client/common/ui/vcexplorer`

**文件数量**: 59 个 Java 文件

**主要功能**: 版本控制资源管理器视图

#### 子目录结构:

| 目录 | 功能描述 |
|------|----------|
| `findinsce` | 在源代码资源管理器中查找 |
| `teamexplorer` | Team Explorer 集成 |
| `versioncontrol` | 版本控制 |

**核心文件**:
- `TFSPerspective.java` - TFS 透视图
- `BranchHierarchyEditorInput.java` - 分支层次结构编辑器输入
- `TFSVersionControlExplorerPlugin.java` - 版本控制资源管理器插件

---

### 6. com.microsoft.tfs.client.eclipse.ui.egit (EGit UI 集成)

**路径**: `source/com.microsoft.tfs.client.eclipse.ui.egit/src/com/microsoft/tfs/client/eclipse/ui`

**主要功能**: 提供 EGit 与 TFS 的 UI 集成

---

### 7. 签入策略 UI 模块

#### 7.1 com.microsoft.tfs.checkinpolicies.build (构建策略)

**UI 文件**:
- `BuildPolicyDialog.java`
- `BuildPolicyResourceChangedListener.java`
- `BuildPolicyUI.java`
- `GeneralTabControl.java`
- `MarkerBrowseDialog.java`
- `MarkerListControl.java`
- `MarkerMatchTable.java`
- `MarkerTabControl.java`

#### 7.2 com.microsoft.tfs.checkinpolicies.forbiddenpatterns (禁止模式策略)

**UI 文件**:
- `ForbiddenPatternsDialog.java`
- `ForbiddenPatternsPolicyUI.java`

#### 7.3 com.microsoft.tfs.checkinpolicies.workitempolicy (工作项策略)

**UI 文件**:
- `WorkItemPolicyUI.java`

#### 7.4 com.microsoft.tfs.checkinpolicies.workitemquerypolicy (工作项查询策略)

**UI 文件**:
- `StoredQueryDialog.java`
- `StoredQueryTable.java`
- `WorkItemQueryPolicyUI.java`

---

## UI 组件统计

| 组件类型 | 数量 |
|----------|------|
| 对话框 (Dialog) | 113 |
| 向导 (Wizard) | 67 |
| 控件 (Control) | 248 |
| 页面 (Page) | 101 |
| 操作 (Action) | 218 |
| 视图 (View) | 58 |
| 编辑器 (Editor) | 59 |

---

## 技术架构

### UI 框架基础

项目基于 Eclipse SWT (Standard Widget Toolkit) 和 JFace 构建，遵循 Eclipse RCP (Rich Client Platform) 架构模式。

### 主要设计模式

1. **MVC 模式**: 模型-视图-控制器分离
2. **适配器模式**: 用于 Eclipse 平台集成
3. **命令模式**: 操作和命令的封装
4. **观察者模式**: 事件监听和通知
5. **工厂模式**: UI 组件创建

### 插件扩展点

- 视图扩展 (`org.eclipse.ui.views`)
- 编辑器扩展 (`org.eclipse.ui.editors`)
- 透视图扩展 (`org.eclipse.ui.perspectives`)
- 首选项页扩展 (`org.eclipse.ui.preferencePages`)
- 属性页扩展 (`org.eclipse.ui.propertyPages`)
- 装饰器扩展 (`org.eclipse.ui.decorators`)
- 命令扩展 (`org.eclipse.ui.commands`)

---

## 模块依赖关系

```
com.microsoft.tfs.client.eclipse.ui
    └── com.microsoft.tfs.client.common.ui
            ├── com.microsoft.tfs.client.common (核心客户端)
            ├── com.microsoft.tfs.core (核心库)
            └── com.microsoft.tfs.util (工具库)

com.microsoft.tfs.client.common.ui.teambuild
    └── com.microsoft.tfs.client.common.ui

com.microsoft.tfs.client.common.ui.vcexplorer
    └── com.microsoft.tfs.client.common.ui
```

---

## 主要功能区域

### 1. Team Explorer (团队资源管理器)
- 项目导航
- 团队项目管理
- 收藏夹

### 2. 版本控制 (Version Control)
- 源代码资源管理器
- 挂起更改
- 变更集
- 搁置集
- 分支可视化
- 历史记录
- 比较/合并
- 冲突解决

### 3. 工作项跟踪 (Work Item Tracking)
- 工作项查询
- 工作项表单
- 查询结果
- 工作项搜索

### 4. 构建管理 (Team Build)
- 构建定义
- 构建队列
- 构建详情
- 构建报告

### 5. 签入策略 (Check-in Policies)
- 构建验证
- 工作项关联
- 代码分析

---

## 资源文件

每个 UI 模块都包含对应的资源文件:
- `messages.properties` - 国际化消息
- 图标和图像资源

---

*文档生成时间: 2026-03-26*
