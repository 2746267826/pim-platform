# tests/Pim.UnitTests/Mobile/MobileQualityServiceTests.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.UnitTests
- 职责：覆盖 `MobileQualityService.GetQualityAsync` 组件键、真实问题汇总、心跳隔离与元数据缺口。
- 主要依赖：`MobileQualityService`、`MobileTestHelpers`、Mobile/Daemon 实体
- 被谁使用：xUnit

## 函数级结构化伪代码

### MobileQualityServiceTests
#### GetQualityAsync_ReturnsStableComponentKeys
- 空库仍含 android-heartbeat / mobile-usage-coverage / mobile-sync / mobile-location / mobile-app-metadata
#### GetQualityAsync_ReportsRealSyncLocationAndFallbackIssues
- 种子失败同步批、拒绝定位、fallback 摘要 → Warning 与多 issue code；组件 details 计数
#### GetQualityAsync_IgnoresHeartbeatsFromOtherUsersDevices
- 他设备心跳忽略 → heartbeat Unknown + mobile-heartbeat-missing
#### GetQualityAsync_WarnsWhenFallbackOrAppMetadataGapsRemainWithRealEvents
- 真实事件+fallback+队列错误+缺失 catalog → fallback/upload-queue/metadata-missing 警告

## 近逐行中文伪代码

1. [L1-L10] using 与类
2. [L12-L29] 稳定组件键
3. [L31-L124] 同步/定位/fallback 问题
4. [L126-L168] 心跳用户隔离
5. [L170-L258] 混合质量缺口

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/Pim.UnitTests/Mobile/MobileQualityServiceTests.cs",
      "label": "MobileQualityServiceTests",
      "path": "tests/Pim.UnitTests/Mobile/MobileQualityServiceTests.cs",
      "doc": "docs/pseudocode/files/tests/Pim.UnitTests/Mobile/MobileQualityServiceTests.cs.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "tests/Pim.UnitTests/Mobile/MobileQualityServiceTests.cs", "to": "src/modules/Pim.Module.Mobile/Services/MobileQualityService.cs", "type": "tests" },
    { "from": "tests/Pim.UnitTests/Mobile/MobileQualityServiceTests.cs", "to": "tests/Pim.UnitTests/Mobile/MobileTestHelpers.cs", "type": "depends_on" }
  ]
}
```
