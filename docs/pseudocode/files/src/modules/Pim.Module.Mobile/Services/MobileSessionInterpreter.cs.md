# src/modules/Pim.Module.Mobile/Services/MobileSessionInterpreter.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.Mobile
- 职责：按用户+设备+时间窗，从 `MobileUsageEventEntity` 重建 `MobileUsageSessionEntity`（前台到后台/切应用闭合会话）。
- 主要依赖：`PimDbContext`、`MobileUsageEventEntity`、`MobileUsageSessionEntity`
- 被谁使用：用量入库后会话重建流程（`MobileUsageIngestService` 等）；`MobileModule` DI 注册

## 函数级结构化伪代码

### MobileSessionInterpreter
#### 构造(PimDbContext db)
- 输入：db
- 输出：实例
- 副作用：无
- 步骤：捕获 `_db`
- 分支与异常：无
- 调用：无

#### Task RebuildSessionsAsync(userId, deviceId, rangeStartUtc, rangeEndUtc, ct)
- 输入：用户、设备、UTC 区间
- 输出：无
- 副作用：删除重叠旧会话；插入新会话；SaveChanges
- 步骤：
  1. 查询与区间重叠的已有会话（Start < end 且 End 空或 End > start）并 RemoveRange
  2. 拉取区间内事件：AsNoTracking，按 EventTimestampUtc 再 Id 排序
  3. open=null；遍历事件：
     - 前台：若已有 open → `AddSession(open, 当前时间, closed-by-app-switch)`；open=当前
     - 后台且 open 存在且 PackageName 相同 → `AddSession(open, 当前时间, [])`；open=null
  4. 循环后 open 仍在 → 以 rangeEndUtc 闭合，flags `open-ended`
  5. SaveChangesAsync
- 分支与异常：无显式 try；EF 异常上抛
- 调用：`AddSession`、EF Set/RemoveRange/Add/Save

#### void AddSession(startEvent, endUtc, qualityFlagsJson)
- 输入：起始前台事件、结束时刻、质量标志 JSON
- 输出：无
- 副作用：Add 新 `MobileUsageSessionEntity`
- 步骤：
  1. DurationMs = max(0, end-start 毫秒)
  2. 复制 UserId/DeviceId/PackageName；Start/End/Duration/QualityFlags；CreatedAt=UtcNow
- 分支与异常：无
- 调用：EF Add

#### static bool IsForeground / IsBackground(eventType)
- 输入：事件类型串
- 输出：是否 MOVE_TO_FOREGROUND / MOVE_TO_BACKGROUND（忽略大小写）
- 副作用：无
- 步骤：OrdinalIgnoreCase 字符串相等
- 分支与异常：无
- 调用：无

## 近逐行中文伪代码

1. 注入 `PimDbContext`
2. `RebuildSessionsAsync`：删重叠会话 → 有序拉事件 → 前台开/切应用闭 → 同包后台闭 → 区间末 open-ended → 保存
3. `AddSession` 写 DurationMs 与质量 JSON
4. 识别前台/后台事件类型常量

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.Mobile/Services/MobileSessionInterpreter.cs",
      "label": "MobileSessionInterpreter",
      "path": "src/modules/Pim.Module.Mobile/Services/MobileSessionInterpreter.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.Mobile/Services/MobileSessionInterpreter.cs.md",
      "layer": "module.mobile",
      "kind": "service"
    }
  ],
  "edges": [
    { "from": "src/modules/Pim.Module.Mobile/Services/MobileSessionInterpreter.cs", "to": "src/Pim.Infrastructure/Data/PimDbContext.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Mobile/Services/MobileSessionInterpreter.cs", "to": "src/modules/Pim.Module.Mobile/Entities/MobileUsageEventEntity.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Mobile/Services/MobileSessionInterpreter.cs", "to": "src/modules/Pim.Module.Mobile/Entities/MobileUsageSessionEntity.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Mobile/MobileModule.cs", "to": "src/modules/Pim.Module.Mobile/Services/MobileSessionInterpreter.cs", "type": "depends_on" }
  ]
}
```
