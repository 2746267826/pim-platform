# src/modules/Pim.Module.Mobile/Services/MobileDeviceService.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.Mobile
- 职责：移动设备注册（upsert）与当前用户设备列表查询
- 主要依赖：`PimDbContext`、`ICurrentUserService`、`MobileUserContext`、`MobileDeviceEntity`、Mobile 设备 DTO
- 被谁使用：Mobile 设备相关 API 端点

## 函数级结构化伪代码

### MobileDeviceService
#### 构造函数
- 输入：`PimDbContext db`、`ICurrentUserService currentUser`
- 输出：服务实例
- 副作用：无
- 步骤：保存 `_db`、`_currentUser`
- 分支与异常：无
- 调用：无

#### `async Task<MobileDeviceDto> RegisterAsync(MobileDeviceRegisterRequest request, ct)`
- 输入：注册请求（DeviceId、DeviceHash、显示名、厂商/品牌/型号、系统与 API、App 版本、MetadataJson）
- 输出：映射后的 `MobileDeviceDto`
- 副作用：插入或更新 `MobileDeviceEntity` 并 `SaveChanges`
- 步骤：
  1. `MobileUserContext.RequireUserId` 取当前用户
  2. now = UtcNow
  3. 按 UserId+DeviceId 查单条；不存在则新建（UserId、DeviceId、RegisteredAtUtc、CreatedAt）并 Add
  4. 覆盖：DeviceHash、DisplayName、Manufacturer、Brand、Model、OsVersion、ApiLevel、AppVersion
  5. MetadataJson = `JsonOrDefault`（空白→`{}`）
  6. LastSeenAtUtc、UpdatedAt = now；Save；`Map` 返回
- 分支与异常：未登录由 RequireUserId 抛出；重复键由 EF 约束
- 调用：EF、`JsonOrDefault`、`Map`

#### `async Task<IReadOnlyList<MobileDeviceDto>> ListAsync(ct)`
- 输入：无
- 输出：当前用户设备列表，按 LastSeenAtUtc 降序
- 副作用：只读 AsNoTracking 查询
- 步骤：RequireUserId → Where UserId → OrderByDescending LastSeen → Select Map → ToList
- 分支与异常：未登录
- 调用：EF、`Map`

#### `private static MobileDeviceDto Map(entity)`
- 输入：实体
- 输出：DTO（Id、DeviceId、Hash、DisplayName、厂商链、OsVersion、ApiLevel、AppVersion、MetadataJson、RegisteredAt、LastSeen）
- 副作用：无
- 步骤：位置参数构造 record
- 分支与异常：无
- 调用：无

#### `private static string JsonOrDefault(string? value)`
- 输入：可选 JSON 字符串
- 输出：空白则 `{}`，否则原值
- 副作用：无
- 步骤：IsNullOrWhiteSpace 判断
- 分支与异常：无
- 调用：无

## 近逐行中文伪代码

1. 引入 EF、Auth、Data、DTOs、Entities
2. sealed 服务；字段 `_db`、`_currentUser`；构造注入
3. RegisterAsync：取 userId 与 now；按 UserId+DeviceId SingleOrDefault
4. 无实体则新建并 Add（RegisteredAt/CreatedAt=now）
5. 更新设备指纹与硬件/系统/App 元数据；Metadata 空则 `{}`；LastSeen/Updated=now；Save；Map
6. ListAsync：当前用户 AsNoTracking，LastSeen 降序，Map 列表
7. Map：实体字段投影到 MobileDeviceDto
8. JsonOrDefault：空白 → `{}`

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.Mobile/Services/MobileDeviceService.cs",
      "label": "MobileDeviceService",
      "path": "src/modules/Pim.Module.Mobile/Services/MobileDeviceService.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.Mobile/Services/MobileDeviceService.cs.md",
      "layer": "module.mobile",
      "kind": "service"
    }
  ],
  "edges": [
    { "from": "src/modules/Pim.Module.Mobile/Services/MobileDeviceService.cs", "to": "src/Pim.Infrastructure/Data/PimDbContext.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Mobile/Services/MobileDeviceService.cs", "to": "src/Pim.Infrastructure/Auth/CurrentUserService.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Mobile/Services/MobileDeviceService.cs", "to": "src/modules/Pim.Module.Mobile/Entities/MobileDeviceEntity.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Mobile/Services/MobileDeviceService.cs", "to": "src/modules/Pim.Module.Mobile/DTOs", "type": "depends_on" }
  ]
}
```
