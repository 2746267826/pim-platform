# src/modules/Pim.Module.PcTracker/Services/PcActivityRecordKeyService.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.PcTracker
- 职责：为 PC 明细活动记录生成稳定或降级的 RecordKey，并序列化源事件/桶 Id 元数据。
- 主要依赖：`System.Security.Cryptography`、`System.Text`、`System.Text.Json`、`PcDetailRecord` DTO
- 被谁使用：PcTracker 同步/去重/明细入库相关服务

## 函数级结构化伪代码

### PcActivityRecordKeyResult
#### 记录类型字段
- 输入：构造参数
- 输出：`RecordKey`、`KeyVersion`、`Stability`、`SourceType`、`SourceEventIdsJson`、`SourceBucketIdsJson`
- 副作用：无
- 步骤：sealed record 承载 Build 结果。
- 分支与异常：无
- 调用：无

### PcActivityRecordKeyService
#### BuildKey(PcDetailRecord record)
- 输入：明细记录
- 输出：`PcActivityRecordKeyResult`
- 副作用：无
- 步骤：
  1. 委托静态 `Build(record)`。
- 分支与异常：见 Build
- 调用：`Build`

#### Build(PcDetailRecord record) [static]
- 输入：明细记录（非 null）
- 输出：键结果（稳定 aw 键或 fallback 哈希键）
- 副作用：无
- 步骤：
  1. `ArgumentNullException.ThrowIfNull(record)`。
  2. 取排序去重后的 `SourceEventIds` 与 `SourceBucketIds`。
  3. 若两者均非空：
     - eventPart = 事件 Id 用 `-` 连接。
     - bucketPart = 单桶直接用 Id；多桶则 `HashPart(用 | 连接)`。
     - 返回 `pc-aw-v1:{bucketPart}:{eventPart}`，版本 `pc-aw-v1`，Stability=`stable`，SourceType=`aw`，JSON 序列化两列表。
  4. 否则拼 fallback 载荷行：RecordType、DeviceId、Start、End(缺省 Start)、事件串、桶串、AppName、Domain、Path、Title（空串兜底）。
  5. 返回 `pc-fallback-v1:{HashPart(payload)}`，版本 `pc-fallback-v1`，Stability=`low`，SourceType=`fallback`。
- 分支与异常：record null 抛 ArgumentNullException
- 调用：`SourceEventIds`、`SourceBucketIds`、`HashPart`、`JsonSerializer.Serialize`

#### SourceEventIds(PcDetailRecord) [static]
- 输入：记录
- 输出：升序去重 long 列表
- 副作用：无
- 步骤：
  1. 优先 `SourceWebEventIds`（Count>0）；否则 `SourceWindowEventIds`；否则空。
  2. Distinct + OrderBy 返回列表。
- 分支与异常：无
- 调用：LINQ

#### SourceBucketIds(PcDetailRecord) [static]
- 输入：记录
- 输出：序数排序去重的非空白桶 Id 列表
- 副作用：无
- 步骤：
  1. 取 `SourceBucketIds` 或空；过滤空白；Trim；Ordinal Distinct；Ordinal Order。
- 分支与异常：无
- 调用：LINQ

#### HashPart(string payload) [private static]
- 输入：字符串载荷
- 输出：SHA256 十六进制小写前 32 字符
- 副作用：无
- 步骤：
  1. UTF8 字节 → SHA256 → Hex 小写 → 截取前 32。
- 分支与异常：无
- 调用：`SHA256.HashData`、`Convert.ToHexString`

## 近逐行中文伪代码

1. 引入 Cryptography、Text、Json 与 PcTracker DTOs。
2. 定义结果 record：键、版本、稳定性、源类型、事件/桶 JSON。
3. 实例方法 BuildKey 调静态 Build。
4. Build 空参检查后收集源事件 Id 与桶 Id。
5. 两者都有则拼 `pc-aw-v1` 稳定键（多桶哈希）；否则拼多行字段 fallback 载荷。
6. fallback 键 `pc-fallback-v1` + 载荷 SHA256 前 32 hex，稳定性 low。
7. SourceEventIds：Web 优先于 Window 事件 Id，去重排序。
8. SourceBucketIds：去空白 Trim、去重排序。
9. HashPart：UTF8 SHA256 小写 hex 截断 32。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.PcTracker/Services/PcActivityRecordKeyService.cs",
      "label": "PcActivityRecordKeyService",
      "path": "src/modules/Pim.Module.PcTracker/Services/PcActivityRecordKeyService.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.PcTracker/Services/PcActivityRecordKeyService.cs.md",
      "layer": "module.pctracker",
      "kind": "service"
    }
  ],
  "edges": [
    { "from": "src/modules/Pim.Module.PcTracker/Services/PcActivityRecordKeyService.cs", "to": "src/modules/Pim.Module.PcTracker/DTOs", "type": "depends_on" }
  ]
}
```
