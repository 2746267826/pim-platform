# tests/Pim.UnitTests/Ai/AiPersistenceModelTests.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.UnitTests
- 职责：验证 AI 请求日志与服务商设置实体可持久化及 UpdatedAt 刷新。
- 主要依赖：`AiRequestLogEntity`、`AiProviderSettingEntity`、`PimDbContext`
- 被谁使用：xUnit

## 函数级结构化伪代码

### AiRequestLogs_PersistCompleteAttemptTrace
- 写入完整 attempt 字段后读回 Provider/Tokens/ParsedOutput
### AiProviderSettings_PersistSystemProviderState
- 加密密钥字节；无 VirtualKeySecret 明文属性
### AiProviderSettings_UpdateRefreshesUpdatedAt
- 改 Status 后 UpdatedAt 前进

## 近逐行中文伪代码

1. [L1-L12] 类与日志持久化
2. [L62-L85] 服务商设置
3. [L87-L116] UpdatedAt 刷新

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/Pim.UnitTests/Ai/AiPersistenceModelTests.cs",
      "label": "AiPersistenceModelTests",
      "path": "tests/Pim.UnitTests/Ai/AiPersistenceModelTests.cs",
      "doc": "docs/pseudocode/files/tests/Pim.UnitTests/Ai/AiPersistenceModelTests.cs.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "tests/Pim.UnitTests/Ai/AiPersistenceModelTests.cs", "to": "src/Pim.Infrastructure/Data/Entities/AiRequestLogEntity.cs", "type": "tests" },
    { "from": "tests/Pim.UnitTests/Ai/AiPersistenceModelTests.cs", "to": "src/Pim.Infrastructure/Data/Entities/AiProviderSettingEntity.cs", "type": "tests" }
  ]
}
```
