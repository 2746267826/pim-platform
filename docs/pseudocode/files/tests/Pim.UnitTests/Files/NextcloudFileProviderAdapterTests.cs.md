# tests/Pim.UnitTests/Files/NextcloudFileProviderAdapterTests.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.UnitTests
- 职责：Nextcloud WebDAV 适配器 HTTP 契约：PROPFIND/GET/PUT/MOVE/DELETE、路径安全、回收站/版本、打开链接。
- 主要依赖：`NextcloudFileProviderAdapter`、CapturingHandler、DomainException 5202
- 被谁使用：dotnet test

## 函数级结构化伪代码

### 列表/下载/上传/移动
- ListFolder：Depth=1 BasicAuth PROPFIND；去掉自身节点
- Download：GET 内容/类型/文件名；Dispose 释放响应
- Upload：PUT 后 Depth=0 PROPFIND
- Move：Destination + Overwrite=F 后取元数据

### 路径安全（5202 且无请求）
- 危险 path；Rename 非法名；RestoreTrash 危险/空 id；RestoreVersion/ListVersions 危险 id

### 回收站与版本
- DeleteToTrash DELETE；ListTrash trashbin PROPFIND；RestoreTrash MOVE→restore
- ListVersions/RestoreVersion 对应 versions 端点

### BuildOpenLink
- public base + dir + mode；openfile；危险路径拒绝

## 近逐行中文伪代码

1. [L15-41] PROPFIND 列表
2. [L44-117] Download/Upload
3. [L120-199] Move 与路径拒绝
4. [L201-301] 版本/回收站 HTTP
5. [L303-334] OpenLink
6. [L336+] CreateAdapter/Connection/XML fixtures

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/Pim.UnitTests/Files/NextcloudFileProviderAdapterTests.cs",
      "label": "NextcloudFileProviderAdapterTests",
      "path": "tests/Pim.UnitTests/Files/NextcloudFileProviderAdapterTests.cs",
      "doc": "docs/pseudocode/files/tests/Pim.UnitTests/Files/NextcloudFileProviderAdapterTests.cs.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "tests/Pim.UnitTests/Files/NextcloudFileProviderAdapterTests.cs", "to": "src/Pim.Module.Files/Providers/NextcloudFileProviderAdapter.cs", "type": "tests" }
  ]
}
```
