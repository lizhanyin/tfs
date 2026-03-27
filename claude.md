# 开发功能

# VCS 
用于支持TFS2015的IDEA插件

## TFS SDK
后端使用 [TFS-SDK](docs/TFS-SDK-14.139.1/README.md) 中的sdk为与tfs的连接工具       

## 开发
前端使用 kt, 后端使用java

## UI与功能
可参考从eclipse中整理出来的文件 [UI.md](docs/UI.md)

## 待实现功能

导入项目向导 参考ui图片 docs/ui-img/1.导入项目向导 目录
```TODO
[x] 导入项目向导
  [x] 服务器选择
    [x] 添加/移除服务器功能
    [x] 修改/移除凭据
    [x] 服务器配置持久化存储
    [x] 连接测试功能
  [x] 团队项目选择
    [x] 从 TFS 服务器加载团队项目列表
  [x] 项目选择
    [x] 树形结构显示服务器项目
    [x] 多选项目/文件夹
    [x] 本地路径选择
  [ ] 项目导入执行
    [ ] 创建工作区
    [ ] 映射文件夹
    [ ] 获取文件
    [ ] 创建 IDEA 项目
```

## 已实现的核心文件

### 服务层
- `TfsConnectionService` - TFS 连接服务接口
- `TfsConnectionServiceImpl` - TFS 连接服务实现
- `TfsServerConfiguration` - 服务器配置持久化存储

### 向导
- `ImportProjectWizard` - 导入项目向导主类
- `ImportProjectContext` - 向导上下文数据
- `ServerSelectionStep` - 服务器选择步骤
- `TeamProjectSelectionStep` - 团队项目选择步骤
- `ProjectSelectionStep` - 项目选择步骤


