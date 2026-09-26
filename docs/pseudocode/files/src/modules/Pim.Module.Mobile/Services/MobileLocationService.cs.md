# src/modules/Pim.Module.Mobile/Services/MobileLocationService.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.Mobile
- 职责：接收与查询移动端定位点；校验坐标与精度，落库并映射 DTO。
- 主要依赖：`PimDbContext`、`ICurrentUserService`、`TimeProvider`、`MobileLocationPointEntity`、`MobileLocationPointRequest/Dto`、`DomainException`、`MobileUserContext`
- 被谁使用：Mobile 模块端点/模块注册

## 函数级结构化伪代码

### MobileLocationService
#### 构造函数(PimDbContext, ICurrentUserService, TimeProvider)
- 输入：DbContext、当前用户、时间提供器
- 输出：服务实例
- 副作用：缓存依赖字段
- 步骤：
  1. 保存 `_db`、`_currentUser`、`_timeProvider`。
- 分支与异常：无
- 调用：无

#### SubmitAsync(MobileLocationPointRequest, CancellationToken)
- 输入：定位点请求、取消令牌
- 输出：`MobileLocationPointDto`（可用点）
- 副作用：始终尝试落库；精度不足时以 rejected 质量写入后抛错
- 步骤：
  1. 校验纬度 [-90,90]、经度 [-180,180]；非法则 `DomainException(6201)`。
  2. `MobileUserContext.RequireUserId` 取当前用户。
  3. 若水平精度 `>= MaxUsableAccuracyMeters`(50)：`SavePointAsync(..., "rejected")` 后 `DomainException(6202)`。
  4. 否则 `SavePointAsync(..., "usable")` 并 `Map` 返回。
- 分支与异常：6201 坐标非法；6202 精度不可用
- 调用：`SavePointAsync`、`Map`、`MobileUserContext.RequireUserId`

#### SavePointAsync(Guid, MobileLocationPointRequest, string quality, CancellationToken) [private]
- 输入：用户 Id、请求、质量标签、取消令牌
- 输出：`MobileLocationPointEntity`
- 副作用：Insert + SaveChanges
- 步骤：
  1. 构造实体：设备、时间、经纬度/精度 decimal 化、Provider/Source、可选高度速度方位、IsMock、RawJson、Quality、CreatedAt=now。
  2. `_db.Set<MobileLocationPointEntity>().Add`；`SaveChangesAsync`；返回实体。
- 分支与异常：EF 持久化异常向上抛
- 调用：`Decimal`/`DecimalOrNull`/`JsonOrDefault`、`TimeProvider.GetUtcNow`

#### GetHistoryAsync(deviceId?, rangeStartUtc?, rangeEndUtc?, maxAccuracyMeters, CancellationToken)
- 输入：可选设备与时间范围、最大精度（默认 50m）
- 输出：最多 500 条 `MobileLocationPointDto` 列表（按 RecordedAtUtc 降序）
- 副作用：只读查询
- 步骤：
  1. 取当前用户；AsNoTracking 过滤 `UserId`。
  2. 可选过滤 DeviceId、时间窗 [start, end)。
  3. 要求水平精度 `< maxAccuracyMeters` 且 `Quality != "rejected"`。
  4. 排序 Take(500) 投影 `Map` 为列表。
- 分支与异常：无用户时由 RequireUserId 抛错
- 调用：EF、`Map`、`Decimal`

#### Map(MobileLocationPointEntity) [private static]
- 输入：实体
- 输出：DTO（含 IsAuto 由 Source=="auto" 判定）
- 副作用：无
- 步骤：字段映射；decimal 转 double；RawJson 透传。
- 分支与异常：无
- 调用：`DecimalToDouble`

#### Decimal / DecimalOrNull / DecimalToDouble / JsonOrDefault [private static]
- 输入：double/decimal/string 可空值
- 输出：转换后的 decimal/double 或默认 `"{}"`
- 副作用：无
- 步骤：Convert 或空值分支；Json 空白则 `"{}"`。
- 分支与异常：无
- 调用：`Convert`

## 近逐行中文伪代码

1. 引入 EF、DomainException、Auth、Data、Mobile DTOs/Entities。
2. 命名空间 Services；sealed 类 `MobileLocationService`。
3. 常量最大可用精度 50 米；注入 Db、当前用户、TimeProvider。
4. SubmitAsync：校验经纬度范围，非法抛 6201。
5. 取 userId；精度 >= 50 则保存 rejected 并抛 6202。
6. 否则保存 usable 并 Map 返回。
7. SavePointAsync：从 request 填实体（decimal 化数值字段、Quality、CreatedAt）。
8. Add + SaveChangesAsync 返回实体。
9. GetHistoryAsync：按用户过滤，可选设备/时间；排除精度差与 rejected。
10. 按记录时间降序最多 500 条 Map 输出。
11. Map 构造 DTO；Source 忽略大小写等于 auto 则 IsAuto true。
12. 工具方法：double↔decimal、空 JSON 默认 `{}`。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.Mobile/Services/MobileLocationService.cs",
      "label": "MobileLocationService",
      "path": "src/modules/Pim.Module.Mobile/Services/MobileLocationService.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.Mobile/Services/MobileLocationService.cs.md",
      "layer": "module.mobile",
      "kind": "service"
    }
  ],
  "edges": [
    { "from": "src/modules/Pim.Module.Mobile/Services/MobileLocationService.cs", "to": "src/Pim.Infrastructure/Data/PimDbContext.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Mobile/Services/MobileLocationService.cs", "to": "src/Pim.Infrastructure/Auth/CurrentUserService.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Mobile/Services/MobileLocationService.cs", "to": "src/modules/Pim.Module.Mobile/Entities/MobileLocationPointEntity.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Mobile/Services/MobileLocationService.cs", "to": "src/modules/Pim.Module.Mobile/DTOs/MobileDtos.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Mobile/Services/MobileLocationService.cs", "to": "src/Pim.Core/Exceptions/DomainException.cs", "type": "depends_on" }
  ]
}
```
