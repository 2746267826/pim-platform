# tests/Pim.UnitTests/Calendar/OutlookGraphDeviceCodeFlowTests.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.UnitTests
- 职责：验证 Outlook 设备码流程加密存 token 并更新连接健康。
- 主要依赖：`OutlookSyncService`、`FakeMicrosoftGraphClient`、`FakeSecretProtector`
- 被谁使用：xUnit

## 函数级结构化伪代码

### DeviceCodeFlowStoresEncryptedTokensAndUpdatesConnectionHealth
- UpdateSettings → CreateDeviceCode → PollDeviceCode
- Status connected/healthy；密文不含明文 access-token；Unprotect 可还原；过期 >55min

## 近逐行中文伪代码

1. [L1-L16] using/UserId
2. [L17-L54] 设备码流程与加密断言
3. [L56-L79] CreateService/CreateDb/StubHttp

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/Pim.UnitTests/Calendar/OutlookGraphDeviceCodeFlowTests.cs",
      "label": "OutlookGraphDeviceCodeFlowTests",
      "path": "tests/Pim.UnitTests/Calendar/OutlookGraphDeviceCodeFlowTests.cs",
      "doc": "docs/pseudocode/files/tests/Pim.UnitTests/Calendar/OutlookGraphDeviceCodeFlowTests.cs.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "tests/Pim.UnitTests/Calendar/OutlookGraphDeviceCodeFlowTests.cs", "to": "src/modules/Pim.Module.Calendar/Services/OutlookSyncService.cs", "type": "tests" },
    { "from": "tests/Pim.UnitTests/Calendar/OutlookGraphDeviceCodeFlowTests.cs", "to": "src/modules/Pim.Module.Calendar/Services/OutlookTokenService.cs", "type": "depends_on" }
  ]
}
```
