# src/modules/Pim.Module.Calendar/Services/CalendarAuditWriter.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.Calendar
- 职责：日历模块成功操作审计写入适配器；固定 `Source=calendar` 并委托 `IAuditLogService`。
- 主要依赖：`IAuditLogService`、`CreateAuditLogRequest`、`AuditActorType`、`AuditResult`
- 被谁使用：`CalendarModule` DI 注册；`CalendarDeleteService`、`CalendarRecycleBinService`；相关单元测试

## 函数级结构化伪代码

### CalendarAuditWriter
#### CalendarAuditWriter(IAuditLogService auditLog)
- 输入：审计日志服务
- 输出：写入器实例
- 副作用：保存 `_auditLog`
- 步骤：赋值字段
- 分支与异常：无
- 调用：无

#### Task RecordSuccessAsync(Guid userId, string action, string resourceType, Guid resourceId, IReadOnlyDictionary<string,string>? metadata = null, CancellationToken ct = default)
- 输入：用户、动作、资源类型/Id、可选元数据、取消令牌
- 输出：异步完成任务
- 副作用：向审计存储写一条 Success 记录（经 `IAuditLogService`）
- 步骤：
  1. 构造 `CreateAuditLogRequest`：`ActorType=User`、`Source=calendar`、`Result=Success`、`ResourceId=resourceId.ToString()`
  2. IP/UserAgent/Correlation/Error 等字段传 null；metadata 原样传入
  3. 调用 `_auditLog.RecordAsync(..., ct)` 并返回其 Task
- 分支与异常：异常由审计服务向上抛
- 调用：`IAuditLogService.RecordAsync`

## 近逐行中文伪代码

1. 引入 `Pim.Core.Operations`
2. 命名空间 `Pim.Module.Calendar.Services`
3. 密封类；常量 Source=`calendar`；注入 `IAuditLogService`
4. `RecordSuccessAsync`：组装 CreateAuditLogRequest（User、Success、无错误字段）
5. 委托 `RecordAsync` 并透传 CancellationToken

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.Calendar/Services/CalendarAuditWriter.cs",
      "label": "CalendarAuditWriter",
      "path": "src/modules/Pim.Module.Calendar/Services/CalendarAuditWriter.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.Calendar/Services/CalendarAuditWriter.cs.md",
      "layer": "module.calendar",
      "kind": "service"
    }
  ],
  "edges": [
    { "from": "src/modules/Pim.Module.Calendar/Services/CalendarAuditWriter.cs", "to": "src/Pim.Core/Operations", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Calendar/Services/CalendarAuditWriter.cs", "to": "src/Pim.Core/Operations/AuditDtos.cs", "type": "calls" },
    { "from": "src/modules/Pim.Module.Calendar/CalendarModule.cs", "to": "src/modules/Pim.Module.Calendar/Services/CalendarAuditWriter.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Calendar/Services/CalendarDeleteService.cs", "to": "src/modules/Pim.Module.Calendar/Services/CalendarAuditWriter.cs", "type": "calls" },
    { "from": "src/modules/Pim.Module.Calendar/Services/CalendarRecycleBinService.cs", "to": "src/modules/Pim.Module.Calendar/Services/CalendarAuditWriter.cs", "type": "calls" },
    { "from": "tests/Pim.UnitTests/Calendar/CalendarAuditWriterTests.cs", "to": "src/modules/Pim.Module.Calendar/Services/CalendarAuditWriter.cs", "type": "tests" }
  ]
}
```
