# tests/Pim.UnitTests/Calendar/DataCenterGovernanceTests.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.UnitTests
- 职责：数据中心批预览/审计导出严格治理；确认归档批软删与审计；非严格确认拒绝。
- 主要依赖：DataCenter 治理服务 / OperationConfirmation
- 被谁使用：dotnet test

## 函数级结构化伪代码

### BatchPreviewAndAuditExportExposeStrictGovernanceMetadata
### ExecuteConfirmedArchiveBatchSoftDeletesTaskAndRecordsAuditVersion
### ExecuteBatchRejectsNonStrictConfirmation

## 近逐行中文伪代码

1. 批预览与导出元数据
2. 严格确认后归档软删+AuditVersion
3. 非严格确认拒绝执行

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/Pim.UnitTests/Calendar/DataCenterGovernanceTests.cs",
      "label": "DataCenterGovernanceTests.cs",
      "path": "tests/Pim.UnitTests/Calendar/DataCenterGovernanceTests.cs",
      "doc": "docs/pseudocode/files/tests/Pim.UnitTests/Calendar/DataCenterGovernanceTests.cs.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": {"from":"tests/Pim.UnitTests/Calendar/DataCenterGovernanceTests.cs","to":"src/Pim.Module.Calendar/Services","type":"tests"}
}
```