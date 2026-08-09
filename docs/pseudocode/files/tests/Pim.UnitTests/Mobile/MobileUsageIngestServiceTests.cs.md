# tests/Pim.UnitTests/Mobile/MobileUsageIngestServiceTests.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.UnitTests
- 职责：手机用量批次入库幂等、逐条 ack、并发/重试胜者、校验拒绝、元数据 upsert、分析陈旧标记。
- 主要依赖：`MobileUsageIngestService`、SessionInterpreter、SyncBatch 信封、竞态/重试双 DbContext
- 被谁使用：dotnet test

## 函数级结构化伪代码

### 逐条结果与类型
- 每 item 稳定 outcome；app/event/summary 三类均有结果

### 幂等与并发
- 重复批次返回持久化 ItemResults；并发 insert 读胜者；ExecutionStrategy 重试再查库

### 遗留与校验
- 遗留项确定性 key；旧 envelope 不伪造 ItemResults；invalid-package-name；字段长度约束 code

### 业务
- 幂等+fallback 分表；派生失败不写 ack；package upsert；跨批跳过重复事件；已存在批不重跑派生；标记 analytics stale

## 近逐行中文伪代码

1. [L16-148] 稳定结果/类型/重复/竞态/策略重试
2. [L151-267] 遗留 key/旧 envelope/拒绝码/字段约束
3. [L270-527+] 幂等、失败回滚、upsert、去重、stale
4. 尾部 UploadRequest/Event 工厂与测试专用 DbContext

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/Pim.UnitTests/Mobile/MobileUsageIngestServiceTests.cs",
      "label": "MobileUsageIngestServiceTests",
      "path": "tests/Pim.UnitTests/Mobile/MobileUsageIngestServiceTests.cs",
      "doc": "docs/pseudocode/files/tests/Pim.UnitTests/Mobile/MobileUsageIngestServiceTests.cs.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "tests/Pim.UnitTests/Mobile/MobileUsageIngestServiceTests.cs", "to": "src/Pim.Module.Mobile/Services/MobileUsageIngestService.cs", "type": "tests" },
    { "from": "tests/Pim.UnitTests/Mobile/MobileUsageIngestServiceTests.cs", "to": "src/Pim.Module.Mobile/Services/MobileSessionInterpreter.cs", "type": "depends_on" }
  ]
}
```
