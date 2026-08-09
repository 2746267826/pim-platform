# tests/Pim.UnitTests/Mobile/MobileDeviceServiceTests.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.UnitTests
- 职责：设备注册按用户+deviceId upsert。
- 主要依赖：`MobileDeviceService`、`MobileTestHelpers`
- 被谁使用：xUnit

## 函数级结构化伪代码

### RegisterAsync_UpsertsDeviceByUserAndDeviceId
- 两次 Register 仅一行，DisplayName/Model 更新，Id 稳定

## 近逐行中文伪代码

1. [L1-L26] upsert 测试
2. [L28-L38] Request 工厂

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/Pim.UnitTests/Mobile/MobileDeviceServiceTests.cs",
      "label": "MobileDeviceServiceTests",
      "path": "tests/Pim.UnitTests/Mobile/MobileDeviceServiceTests.cs",
      "doc": "docs/pseudocode/files/tests/Pim.UnitTests/Mobile/MobileDeviceServiceTests.cs.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "tests/Pim.UnitTests/Mobile/MobileDeviceServiceTests.cs", "to": "src/modules/Pim.Module.Mobile/Services/MobileDeviceService.cs", "type": "tests" },
    { "from": "tests/Pim.UnitTests/Mobile/MobileDeviceServiceTests.cs", "to": "tests/Pim.UnitTests/Mobile/MobileTestHelpers.cs", "type": "depends_on" }
  ]
}
```
