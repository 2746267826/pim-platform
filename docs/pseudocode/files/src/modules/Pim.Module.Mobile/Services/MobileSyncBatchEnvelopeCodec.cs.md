# src/modules/Pim.Module.Mobile/Services/MobileSyncBatchEnvelopeCodec.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.Mobile
- 职责：序列化/反序列化移动同步批次错误信封（SchemaVersion + ItemResults + BatchErrors），并提取可读错误消息。
- 主要依赖：`System.Text.Json`、`Pim.Module.Mobile.DTOs.MobileIngestItemResult`
- 被谁使用：Mobile 同步/摄取路径写入 `ErrorJson` 或从批次实体读回错误展示

## 函数级结构化伪代码

### MobileSyncBatchEnvelope（record）
#### 构造字段
- 输入：`SchemaVersion`、`ItemResults`、`BatchErrors`
- 输出：不可变信封
- 副作用：无
- 步骤：记录三字段
- 分支与异常：无
- 调用：无

### MobileSyncBatchEnvelopeCodec
#### const int CurrentSchemaVersion = 1
- 输入：无
- 输出：当前 schema 版本常量
- 副作用：无
- 步骤：固定为 1
- 分支与异常：无
- 调用：无

#### string Serialize(IReadOnlyList\<MobileIngestItemResult\> itemResults, IReadOnlyList\<string\> batchErrors)
- 输入：条目结果列表、批次级错误列表
- 输出：JSON 字符串
- 副作用：无
- 步骤：构造 `MobileSyncBatchEnvelope(CurrentSchemaVersion, ...)` 后 `JsonSerializer.Serialize`
- 分支与异常：序列化异常向上抛
- 调用：`JsonSerializer.Serialize`

#### bool TryDeserialize(string? value, out MobileSyncBatchEnvelope envelope)
- 输入：可空 JSON 字符串
- 输出：成功标志；成功时 out 信封
- 副作用：无
- 步骤：
  1. 空白 → false，envelope 设 null!
  2. `Deserialize`；若 null、SchemaVersion≠当前、ItemResults/BatchErrors 为 null → false
  3. 否则赋值 envelope 并 true
  4. 捕获 `JsonException` → false
- 分支与异常：JSON 异常吞掉返回 false
- 调用：`JsonSerializer.Deserialize`

#### string? ErrorMessage(string? value)
- 输入：存储的错误 JSON 或原始文本
- 输出：人类可读消息或 null
- 副作用：无
- 步骤：
  1. 空白或 `"{}"` → null
  2. 若无法反序列化为信封 → 原样返回 value
  3. 过滤非空白 BatchErrors；无则 null；有则 `"; "` 拼接
- 分支与异常：无
- 调用：`TryDeserialize`

## 近逐行中文伪代码

1. 引入 System.Text.Json 与 Mobile DTOs
2. 命名空间 `Pim.Module.Mobile.Services`
3. record `MobileSyncBatchEnvelope`：版本、条目结果、批次错误
4. 静态类 Codec；`CurrentSchemaVersion = 1`
5. `Serialize`：包当前版本并序列化
6. `TryDeserialize`：空白/版本不匹配/字段空/JSON 异常 → false
7. `ErrorMessage`：空对象 null；非信封原文返回；否则拼接 BatchErrors

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.Mobile/Services/MobileSyncBatchEnvelopeCodec.cs",
      "label": "MobileSyncBatchEnvelopeCodec",
      "path": "src/modules/Pim.Module.Mobile/Services/MobileSyncBatchEnvelopeCodec.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.Mobile/Services/MobileSyncBatchEnvelopeCodec.cs.md",
      "layer": "module.mobile",
      "kind": "service"
    }
  ],
  "edges": [
    { "from": "src/modules/Pim.Module.Mobile/Services/MobileSyncBatchEnvelopeCodec.cs", "to": "src/modules/Pim.Module.Mobile/DTOs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Mobile/Services/MobileSyncBatchEnvelopeCodec.cs", "to": "System.Text.Json", "type": "depends_on" }
  ]
}
```
