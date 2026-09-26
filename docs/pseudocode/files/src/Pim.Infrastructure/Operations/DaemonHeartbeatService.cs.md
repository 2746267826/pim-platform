# src/Pim.Infrastructure/Operations/DaemonHeartbeatService.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Infrastructure
- 职责：实现 `IDaemonHeartbeatService`：按设备+守护类型 upsert 心跳；查询最新心跳；校验 StatusJson。
- 主要依赖：`PimDbContext`、`DaemonHeartbeatEntity`、`DaemonHeartbeatRequest`/`DaemonHeartbeatDto`、`DomainException`、EF Core、`System.Text.Json`
- 被谁使用：DI 注册为 `IDaemonHeartbeatService`；`DaemonEndpoints` 调用

## 函数级结构化伪代码

### DaemonHeartbeatService
#### DaemonHeartbeatService(PimDbContext db)
- 输入：数据库上下文
- 输出：服务实例
- 副作用：无
- 步骤：保存 `_db`
- 分支与异常：无
- 调用：无

#### Task<DaemonHeartbeatDto> UpsertAsync(DaemonHeartbeatRequest request, CancellationToken ct = default)
- 输入：心跳请求；取消令牌
- 输出：持久化后的 `DaemonHeartbeatDto`
- 副作用：插入或更新 `daemon_heartbeats` 行；并发冲突时重试更新
- 步骤：
  1. `NormalizeStatusJson(request.StatusJson)`
  2. 按 `DeviceId`+`DaemonKind` 查唯一实体
  3. 无则新建并 `Add`；`isNew=true`
  4. `Apply` 写字段与 `ReceivedAt=UtcNow`
  5. `SaveChangesAsync`；若新建时 `DbUpdateException`：Clear tracker，再查；有则 Apply+Save，无则重抛
  6. `Map` 返回 DTO
- 分支与异常：StatusJson 非法 → `DomainException(3010)`；并发插入冲突走 catch 路径
- 调用：`NormalizeStatusJson`、`Apply`、`Map`、EF `SingleOrDefaultAsync`/`SaveChangesAsync`

#### Task<DaemonHeartbeatDto?> GetLatestAsync(string deviceId, CancellationToken ct = default)
- 输入：设备 Id
- 输出：该设备最新心跳 DTO 或 null
- 副作用：只读查询
- 步骤：AsNoTracking；按 DeviceId 过滤；按 ReceivedAt 降序 FirstOrDefault；Map
- 分支与异常：无记录 → null
- 调用：`Map`

#### Task<DaemonHeartbeatDto?> GetLatestWindowsAsync(CancellationToken ct = default)
- 输入：取消令牌
- 输出：DaemonKind=`windows` 的最新心跳或 null
- 副作用：只读查询
- 步骤：同 GetLatest 但过滤 `DaemonKind == "windows"`
- 分支与异常：无记录 → null
- 调用：`Map`

#### static DaemonHeartbeatDto Map(DaemonHeartbeatEntity entity)
- 输入：实体
- 输出：DTO（源状态字符串解析为枚举）
- 副作用：无
- 步骤：构造 DTO；`ParseSourceState` 处理 ActivityWatch/KeyStats
- 分支与异常：无
- 调用：`ParseSourceState`

#### static void Apply(DaemonHeartbeatRequest request, string statusJson, DaemonHeartbeatEntity entity)
- 输入：请求、规范化 JSON、目标实体
- 输出：无
- 副作用：就地改实体字段与 ReceivedAt
- 步骤：复制版本/URL/上传时间/错误/队列/源状态字符串/暂停/StatusJson；ReceivedAt=UtcNow
- 分支与异常：无
- 调用：无

#### static string NormalizeStatusJson(string statusJson)
- 输入：原始 StatusJson
- 输出：有效 JSON 字符串（空白 → `"{}"`）
- 副作用：无
- 步骤：空白返回 `{}`；否则 `JsonDocument.Parse` 校验
- 分支与异常：`JsonException` → `DomainException(3010, "StatusJson 必须是有效 JSON")`
- 调用：`JsonDocument.Parse`

#### static DaemonSourceState ParseSourceState(string value)
- 输入：实体中的状态字符串
- 输出：`DaemonSourceState` 枚举；解析失败 → `Unknown`
- 副作用：无
- 步骤：`Enum.TryParse` ignoreCase
- 分支与异常：失败默认 Unknown
- 调用：无

## 近逐行中文伪代码

1. 引入 Json、EF Core、DomainException、Operations、Data、Entities
2. 命名空间 `Pim.Infrastructure.Operations`
3. 密封类 `DaemonHeartbeatService` 实现 `IDaemonHeartbeatService`，注入 `PimDbContext`
4. `UpsertAsync`：规范化 StatusJson；按 DeviceId+DaemonKind 查
5. 不存在则新建实体并 Add
6. Apply 字段；SaveChanges
7. 新建时 DbUpdateException：Clear；再查；存在则 Apply 再 Save，否则抛出
8. Map 返回
9. `GetLatestAsync`：设备维度最新 ReceivedAt
10. `GetLatestWindowsAsync`：windows 类型最新
11. `Map`：实体 → DTO，源状态 Parse
12. `Apply`：请求字段写入实体，ReceivedAt=现在
13. `NormalizeStatusJson`：空→{}；Parse 失败 DomainException 3010
14. `ParseSourceState`：TryParse 否则 Unknown

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/Pim.Infrastructure/Operations/DaemonHeartbeatService.cs",
      "label": "DaemonHeartbeatService",
      "path": "src/Pim.Infrastructure/Operations/DaemonHeartbeatService.cs",
      "doc": "docs/pseudocode/files/src/Pim.Infrastructure/Operations/DaemonHeartbeatService.cs.md",
      "layer": "infrastructure",
      "kind": "service"
    }
  ],
  "edges": [
    { "from": "src/Pim.Infrastructure/Operations/DaemonHeartbeatService.cs", "to": "src/Pim.Core/Operations/DaemonHeartbeatDtos.cs", "type": "implements" },
    { "from": "src/Pim.Infrastructure/Operations/DaemonHeartbeatService.cs", "to": "src/Pim.Infrastructure/Data/PimDbContext.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Operations/DaemonHeartbeatService.cs", "to": "src/Pim.Core/Exceptions/DomainException.cs", "type": "depends_on" },
    { "from": "src/Pim.Api/Endpoints/DaemonEndpoints.cs", "to": "src/Pim.Infrastructure/Operations/DaemonHeartbeatService.cs", "type": "calls" },
    { "from": "tests/Pim.UnitTests/Operations/DaemonHeartbeatServiceTests.cs", "to": "src/Pim.Infrastructure/Operations/DaemonHeartbeatService.cs", "type": "tests" }
  ]
}
```
