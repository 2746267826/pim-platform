# src/modules/Pim.Module.Files/Services/FileAiService.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.Files
- 职责：基于已索引文件 chunk 证据，经 `IAiGateway` 生成摘要/标签（upsert `FileAiResultEntity`）与组织建议（插入 `FileSuggestionEntity`）；并向 `IAiSchemaRegistry` 注册 JSON Schema。
- 主要依赖：
  - `PimDbContext`、`ICurrentUserService`、`IAiGateway`
  - File 实体/DTO、`DomainException`、System.Text.Json、EF Core
- 被谁使用：Files AI 相关端点/后台任务；模块启动时 `RegisterSchemas`

## 函数级结构化伪代码

### FileAiService（primary constructor: db, currentUser, aiGateway）
#### 常量
- `SummarySchemaName` = `files.summary.v1`
- `SuggestionsSchemaName` = `files.organization_suggestions.v1`
- `SchemaVersion` = `"1"`

#### 属性 `UserId`
- 无登录 → DomainException(1002, "未登录")

#### `Task<FileAiResultDto?> GenerateSummaryAndTagsAsync(fileItemId, ct)`
- 输入：文件项 Id
- 输出：DTO 或 null（AI 未成功）
- 副作用：upsert FileAiResultEntity + Save
- 步骤：
  1. `LoadContextAsync`（item+current version+最多 8 chunks）。
  2. `BuildGatewayRequest` purpose=`file.summary`、Summary schema、摘要 prompt。
  3. `aiGateway.CompleteAsync`；`IsSuccessful` 否则 null。
  4. 解析 JSON：tags 数组过滤空白；evidenceChunkIds=全部 chunk Id。
  5. 按 FileItemId+VersionId 查已有结果，无则 Add。
  6. 写入 Summary/TagsJson/Language/Sensitivity/GeneratedAt/Model/AiRequestLogId/EvidenceChunkIdsJson。
  7. Save → `MapAiResult`。
- 分支与异常：5300/5304/5311/1002；AI 失败返回 null
- 调用：LoadContext、BuildGatewayRequest、CompleteAsync、ParseOutput、MapAiResult

#### `Task<IReadOnlyList<FileSuggestionDto>> GenerateOrganizationSuggestionsAsync(fileItemId, ct)`
- 输入：文件项 Id
- 输出：建议 DTO 列表（失败或无 suggestions 数组 → 空列表）
- 副作用：AddRange FileSuggestionEntity + Save
- 步骤：
  1. LoadContext + gateway purpose=`file.organization_suggestions`。
  2. 成功解析后遍历 suggestions 元素；缺 suggestionType/title/reason 则 skip。
  3. confidence clamp 0..1；payload 缺省 `{}`；Status=pending。
  4. 批量插入 → MapSuggestion 列表。
- 分支与异常：上下文异常；AI 失败 []
- 调用：同上 + MapSuggestion

#### `static void RegisterSchemas(IAiSchemaRegistry registry)`
- 副作用：注册两个 AiSchemaDefinition（summary 要求 summary+tags；suggestions 数组项含 enum suggestionType 等）
- 调用：`registry.Register`

#### `LoadContextAsync(fileItemId, ct)` private
- 查 FileItem Include Provider：属当前用户且未删除；null → 5300
- CurrentVersionId null → 5304
- Version：Id 匹配、Source=current、IsCurrent；null → 5304
- Chunks：同 item+version，OrderBy ChunkIndex，Take(8)；0 条 → 5311
- 返回 `FileAiContext`

#### `BuildGatewayRequest(context, purpose, schemaName, prompt)` private
- 构造 AiGatewayRequest：module=files、objectType=file、objectId=Item.Id
- System+User 消息；SchemaName/Version；MaxOutputTokens 摘要 800 / 建议 1200；MaxAttempts=1；Metadata 含 fileId/versionId/evidenceChunkIds

#### `BuildMetadata` / `BuildSummaryPrompt` / `BuildSuggestionsPrompt` / `FormatChunks`
- 元数据字典；prompt 含文件元信息 + `[chunkId] text` 列表

#### `IsSuccessful(AiResult)`
- Status=Succeeded 且 ParsedOutputJson 或 ResponseText 非空白

#### `ParseOutput`
- 优先 ParsedOutputJson 否则 ResponseText 否则 `{}` → JsonDocument.Parse

#### `ReadString(JsonElement, property)`
- 属性存在且为 string 则 GetString，否则 null

#### `MapAiResult` / `MapSuggestion`
- 实体 → DTO；Tags/Evidence 反序列化失败用空列表

#### private record `FileAiContext(Item, Version, Chunks)`

## 近逐行中文伪代码

1. 主构造注入 db、currentUser、aiGateway；常量 schema 名与版本。
2. **摘要**：加载上下文 → 调 AI → 失败 null → 解析 tags/字段 → upsert FileAiResult → 映射 DTO。
3. **建议**：加载上下文 → 调 AI → 失败 [] → 校验 suggestions 数组 → 过滤不完整项 → 插入 pending 建议 → 映射。
4. **RegisterSchemas**：注册 summary 与 organization_suggestions 的 JSON Schema 与描述。
5. **LoadContext**：用户隔离文件 → 当前版本 → 最多 8 条 chunk 证据；缺证据 5311。
6. 网关请求固定 module files、单次尝试、元数据带证据 chunk 列表。
7. 辅助：成功判定、JSON 解析、读字符串、DTO 映射。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.Files/Services/FileAiService.cs",
      "label": "FileAiService",
      "path": "src/modules/Pim.Module.Files/Services/FileAiService.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.Files/Services/FileAiService.cs.md",
      "layer": "module.files",
      "kind": "service"
    }
  ],
  "edges": [
    { "from": "src/modules/Pim.Module.Files/Services/FileAiService.cs", "to": "src/Pim.Infrastructure/Data/PimDbContext.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Files/Services/FileAiService.cs", "to": "src/Pim.Infrastructure/Auth/CurrentUserService.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Files/Services/FileAiService.cs", "to": "src/Pim.Core/Ai/IAiGateway.cs", "type": "calls" },
    { "from": "src/modules/Pim.Module.Files/Services/FileAiService.cs", "to": "src/Pim.Core/Ai/IAiSchemaRegistry.cs", "type": "calls" },
    { "from": "src/modules/Pim.Module.Files/Services/FileAiService.cs", "to": "src/Pim.Core/Exceptions/DomainException.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Files/Services/FileAiService.cs", "to": "src/modules/Pim.Module.Files/Entities", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Files/Services/FileAiService.cs", "to": "src/modules/Pim.Module.Files/DTOs", "type": "depends_on" }
  ]
}
```
