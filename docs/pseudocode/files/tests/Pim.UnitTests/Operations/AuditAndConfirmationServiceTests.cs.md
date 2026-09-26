# tests/Pim.UnitTests/Operations/AuditAndConfirmationServiceTests.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.UnitTests
- 职责：审计写入；确认服务完整生命周期、过期、拒绝、用户隔离、系统确认、非法 JSON。
- 主要依赖：AuditLogService / OperationConfirmationService
- 被谁使用：dotnet test

## 函数级结构化伪代码

### AuditLogService_RecordsAudit
### OperationConfirmationService_HandlesLifecycle / Expires / Rejects / WrongUser / List* / System / Global / Expired / Ordered / Repeated / Executing / InvalidJson*

## 近逐行中文伪代码

1. 审计落库
2. 确认：创建→确认/拒绝/过期/列表过滤/系统全局
3. 非 pending/过期/非法 JSON 拒绝

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/Pim.UnitTests/Operations/AuditAndConfirmationServiceTests.cs",
      "label": "AuditAndConfirmationServiceTests.cs",
      "path": "tests/Pim.UnitTests/Operations/AuditAndConfirmationServiceTests.cs",
      "doc": "docs/pseudocode/files/tests/Pim.UnitTests/Operations/AuditAndConfirmationServiceTests.cs.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [{"from":"tests/Pim.UnitTests/Operations/AuditAndConfirmationServiceTests.cs","to":"src/Pim.Infrastructure/Operations/OperationConfirmationService.cs","type":"tests"},{"from":"tests/Pim.UnitTests/Operations/AuditAndConfirmationServiceTests.cs","to":"src/Pim.Infrastructure/Operations/AuditLogService.cs","type":"tests"}]
}
```