# tests/Pim.UnitTests/Calendar/OutlookGraphTestDoubles.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.UnitTests
- 职责：Outlook Graph 测试替身：Fake 客户端、事件工厂、密钥保护器。
- 主要依赖：`IMicrosoftGraphClient`、`ISecretProtector`
- 被谁使用：Outlook* 单元测试

## 函数级结构化伪代码

### FakeMicrosoftGraphClient
- DeviceCode/Token 可配；DeltaPages 队列；PatchRequests 记录
- RequestDeviceCode/Poll/Refresh/GetDeltaPage/PatchEvent 返回固定或出队

### GraphEventFactory.Create
- 构造 GraphEvent 默认时间/地点/changeKey

### FakeSecretProtector
- Protect 前缀 base64；Unprotect 解码

## 近逐行中文伪代码

1. [L1-L69] FakeMicrosoftGraphClient
2. [L71-L90] GraphEventFactory
3. [L92-L98] FakeSecretProtector

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/Pim.UnitTests/Calendar/OutlookGraphTestDoubles.cs",
      "label": "OutlookGraphTestDoubles",
      "path": "tests/Pim.UnitTests/Calendar/OutlookGraphTestDoubles.cs",
      "doc": "docs/pseudocode/files/tests/Pim.UnitTests/Calendar/OutlookGraphTestDoubles.cs.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "tests/Pim.UnitTests/Calendar/OutlookGraphTestDoubles.cs", "to": "src/modules/Pim.Module.Calendar/Services/IMicrosoftGraphClient.cs", "type": "depends_on" },
    { "from": "tests/Pim.UnitTests/Calendar/OutlookGraphTestDoubles.cs", "to": "src/Pim.Infrastructure/Secrets/ISecretProtector.cs", "type": "depends_on" }
  ]
}
```
