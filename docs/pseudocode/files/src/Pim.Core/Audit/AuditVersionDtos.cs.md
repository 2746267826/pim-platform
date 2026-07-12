# src/Pim.Core/Audit/AuditVersionDtos.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Core
- 职责：定义对象版本审计（审计时间线、恢复预览、导出）相关的只读 DTO `record`。
- 主要依赖：无业务类型依赖（仅 BCL：`Guid`、`string`、`DateTimeOffset`、`IReadOnlyList`）
- 被谁使用：`AuditVersionService`（映射/返回）、`DataCenterGovernanceService`（导出与恢复预览）、`CalendarModule` API 端点

## 函数级结构化伪代码

### AuditVersionDto
#### 记录构造（位置参数 record）
- 输入：
  - `Id`：版本记录 Id
  - `ObjectType` / `ObjectId`：被审计对象类型与 Id
  - `ConfirmationId`：可选关联操作确认 Id
  - `Source`：来源标识
  - `Actor`：操作者标识
  - `BeforeJson` / `AfterJson`：变更前后 JSON 快照
  - `ChangedFieldsJson`：变更字段列表的 JSON
  - `CreatedAt`：创建时间
- 输出：不可变版本审计 DTO
- 副作用：无
- 步骤：
  1. 调用方（通常为 `AuditVersionService.Map`）从实体映射全部字段
- 分支与异常：本类型无逻辑
- 调用：被时间线列表与记录写入返回路径使用

### AuditTimelineResponse
#### 记录构造（位置参数 record）
- 输入：`Items`：`AuditVersionDto` 只读列表
- 输出：对象审计时间线响应
- 副作用：无
- 步骤：
  1. 服务按对象查询版本列表后包装为本响应
- 分支与异常：无
- 调用：作为 `GetTimelineAsync` 一类 API 的返回体

### RestorePreviewResponse
#### 记录构造（位置参数 record）
- 输入：
  - `ObjectType` / `ObjectId`：目标对象
  - `Summary`：恢复预览摘要
  - `RequiresConfirmation`：是否需要二次确认
  - `ChangedFields`：将受影响字段列表
- 输出：恢复操作预览响应
- 副作用：无
- 步骤：
  1. 服务对比当前态与目标版本后组装预览
- 分支与异常：无
- 调用：数据中心/治理恢复预览端点

### AuditExportResponse
#### 记录构造（位置参数 record）
- 输入：
  - `FileName`：导出文件名
  - `ContentType`：MIME 类型
  - `Content`：文件内容（字符串载体）
- 输出：审计导出响应
- 副作用：无
- 步骤：
  1. 服务序列化审计数据后填入文件名、类型与内容
- 分支与异常：无
- 调用：审计导出 API

## 近逐行中文伪代码

1. 命名空间：`Pim.Core.Audit`
2. 定义密封 record `AuditVersionDto`，字段依次为：
3.   - `Id`（Guid）
4.   - `ObjectType`（string）、`ObjectId`（Guid）
5.   - `ConfirmationId`（Guid?）
6.   - `Source`、`Actor`（string）
7.   - `BeforeJson`、`AfterJson`、`ChangedFieldsJson`（string）
8.   - `CreatedAt`（DateTimeOffset）
9. 定义密封 record `AuditTimelineResponse`：持有 `Items`（`IReadOnlyList<AuditVersionDto>`）
10. 定义密封 record `RestorePreviewResponse`：
11.   - `ObjectType`、`ObjectId`
12.   - `Summary`（string）
13.   - `RequiresConfirmation`（bool）
14.   - `ChangedFields`（`IReadOnlyList<string>`）
15. 定义密封 record `AuditExportResponse`：
16.   - `FileName`、`ContentType`、`Content`（均为 string）

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/Pim.Core/Audit/AuditVersionDtos.cs",
      "label": "AuditVersionDtos",
      "path": "src/Pim.Core/Audit/AuditVersionDtos.cs",
      "doc": "docs/pseudocode/files/src/Pim.Core/Audit/AuditVersionDtos.cs.md",
      "layer": "core",
      "kind": "dto"
    }
  ],
  "edges": [
    { "from": "src/Pim.Infrastructure/Audit/AuditVersionService.cs", "to": "src/Pim.Core/Audit/AuditVersionDtos.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Calendar/Services/DataCenterGovernanceService.cs", "to": "src/Pim.Core/Audit/AuditVersionDtos.cs", "type": "calls" },
    { "from": "src/modules/Pim.Module.Calendar/CalendarModule.cs", "to": "src/Pim.Core/Audit/AuditVersionDtos.cs", "type": "depends_on" }
  ]
}
```
