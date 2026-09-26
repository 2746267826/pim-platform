# tests/Pim.UnitTests/Operations/SystemStatusServiceTests.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.UnitTests
- 职责：系统状态汇总：缺心跳/Noop 后台/陈旧心跳/库失败细节。
- 主要依赖：`SystemStatusService`、`IBackgroundJobStatusService`
- 被谁使用：xUnit

## 函数级结构化伪代码

1. 无心跳 → Unknown「系统状态未知」
2. 有心跳但 Noop 后台 Unknown
3. 心跳 >15min Warning「部分系统需要关注」
4. 陈旧+Noop 仍 Warning
5. Dispose Db 后 GetDetail Critical database/windows-daemon

## 近逐行中文伪代码

1. [L1-L139] 五场景
2. [L141-L145] FakeBackgroundJobStatusService

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/Pim.UnitTests/Operations/SystemStatusServiceTests.cs",
      "label": "SystemStatusServiceTests",
      "path": "tests/Pim.UnitTests/Operations/SystemStatusServiceTests.cs",
      "doc": "docs/pseudocode/files/tests/Pim.UnitTests/Operations/SystemStatusServiceTests.cs.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "tests/Pim.UnitTests/Operations/SystemStatusServiceTests.cs", "to": "src/Pim.Infrastructure/Operations/SystemStatusService.cs", "type": "tests" }
  ]
}
```
