# src/Pim.Infrastructure/Data/Entities/AiProviderSettingEntity.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Infrastructure
- 职责：AI 提供商运行时配置的 EF 实体，映射表 `ai_provider_settings`（基址、加密密钥、默认模型、健康状态等）。
- 主要依赖：`System.ComponentModel.DataAnnotations` / `Schema`（`[Table]`、`[Key]`、`[Column]`、`[MaxLength]`）
- 被谁使用：`PimDbContext.AiProviderSettings`；`AiProviderHealthService` 读写健康检查结果；相关 EF Migration / Snapshot

## 函数级结构化伪代码

### AiProviderSettingEntity
#### 属性组（实体 POCO，无方法）
- 输入：EF 物化行；或业务代码 `new AiProviderSettingEntity { ... }`
- 输出：可持久化的提供商设置实例
- 副作用：无（纯数据）；写入 DB 由 `DbContext` 完成
- 步骤：
  1. `Id`：主键 Guid，默认 `NewGuid()`
  2. `Provider`：提供商标识，最长 32，默认 `"litellm"`
  3. `BaseUrl`：API 基址，最长 512，默认空串
  4. `VirtualKeySecretEncrypted`：虚拟密钥密文 `byte[]`，默认空数组（列名 `virtual_key_secret`）
  5. `DefaultModel`：默认模型名，最长 128，默认空串
  6. `Status`：状态字符串，最长 32，默认 `"disabled"`
  7. `LastHealthCheckAt`：最近健康检查时间（可空）
  8. `LastError`：最近错误文本（可空）
  9. `CreatedAt` / `UpdatedAt`：创建/更新时间，默认 `UtcNow`
- 分支与异常：无；长度与非空由注解与 DB 约束约束
- 调用：无主动调用

## 近逐行中文伪代码

1. 引入 DataAnnotations 与 Schema 命名空间
2. 命名空间：`Pim.Infrastructure.Data.Entities`
3. 表映射：`[Table("ai_provider_settings")]`
4. 密封类 `AiProviderSettingEntity`
5. `Id`：Guid 主键，列 `id`，默认新 Guid
6. `Provider`：字符串列 `provider`，MaxLength 32，默认 litellm
7. `BaseUrl`：字符串列 `base_url`，MaxLength 512，默认空
8. `VirtualKeySecretEncrypted`：字节数组列 `virtual_key_secret`，默认空数组
9. `DefaultModel`：字符串列 `default_model`，MaxLength 128，默认空
10. `Status`：字符串列 `status`，MaxLength 32，默认 disabled
11. `LastHealthCheckAt`：可空 DateTimeOffset 列 `last_health_check_at`
12. `LastError`：可空字符串列 `last_error`
13. `CreatedAt`：DateTimeOffset 列 `created_at`，默认 UtcNow
14. `UpdatedAt`：DateTimeOffset 列 `updated_at`，默认 UtcNow

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/Pim.Infrastructure/Data/Entities/AiProviderSettingEntity.cs",
      "label": "AiProviderSettingEntity",
      "path": "src/Pim.Infrastructure/Data/Entities/AiProviderSettingEntity.cs",
      "doc": "docs/pseudocode/files/src/Pim.Infrastructure/Data/Entities/AiProviderSettingEntity.cs.md",
      "layer": "infrastructure",
      "kind": "entity"
    }
  ],
  "edges": [
    { "from": "src/Pim.Infrastructure/Data/PimDbContext.cs", "to": "src/Pim.Infrastructure/Data/Entities/AiProviderSettingEntity.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Ai/AiProviderHealthService.cs", "to": "src/Pim.Infrastructure/Data/Entities/AiProviderSettingEntity.cs", "type": "depends_on" }
  ]
}
```
