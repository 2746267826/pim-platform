# src/modules/Pim.Module.Calendar/Services/OutlookTokenService.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.Calendar
- 职责：Outlook 连接的 OAuth 令牌加密存取、过期前刷新、清空；通过 `ISecretProtector` 保护 access/refresh token。
- 主要依赖：
  - `PimDbContext`、`ISecretProtector`
  - `OutlookConnectionEntity`
  - `IMicrosoftGraphClient`、`TokenResult`（刷新）
- 被谁使用：Outlook 连接/同步相关服务

## 函数级结构化伪代码

### OutlookTokenService
#### 构造
- 输入：db、secretProtector
- 输出：实例
- 副作用：无
- 步骤：赋值字段
- 分支与异常：无
- 调用：无

#### `void StoreTokens(connection, token, now)`
- 输入：连接实体、TokenResult、当前时间
- 输出：void（改连接字段）
- 副作用：写加密字节与元数据到 connection（未 Save）
- 步骤：
  1. Access/Refresh 经 `Protect` 写入 *Encrypted。
  2. 过期时间 = now + max(0, ExpiresInSeconds)。
  3. Scopes：token 有值则覆盖，否则保留。
  4. Status=connected；TokenHealth=healthy；LastError=null；UpdatedAt=now。
- 分支与异常：无
- 调用：`Protect`

#### `Task<string?> GetValidAccessTokenAsync(connection, graph, ct)`
- 输入：连接、Graph 客户端、取消令牌
- 输出：明文 access token 或 null
- 副作用：可能刷新并 SaveChanges；更新 TokenHealth
- 步骤：
  1. AccessTokenEncrypted 空 → TokenHealth=missing，返回 null。
  2. 已过期 → TokenHealth=expired（标记）。
  3. 若 ≤ now+5 分钟且有 refresh 与 ClientId：调用 `graph.RefreshAsync` → StoreTokens → Save；失败 TokenHealth=refresh-failed、Save、null。
  4. 否则/成功后：`Unprotect` access 返回。
- 分支与异常：刷新 catch 任意异常降级
- 调用：graph.RefreshAsync、StoreTokens、SaveChanges、Unprotect

#### `void ClearTokens(connection)`
- 输入：连接
- 输出：void
- 副作用：清空令牌字段与状态（未 Save）
- 步骤：Access=[]；Refresh=null；Expires=null；Status=not-connected；TokenHealth=missing；UpdatedAt=UtcNow。
- 分支与异常：无
- 调用：无

#### `string Unprotect(byte[] protectedValue)` / `byte[] Protect(string value)`
- 输入：受保护字节 / 明文
- 输出：明文 / UTF8 保护字节
- 副作用：无
- 步骤：
  1. Unprotect：UTF8 字符串 → `_secretProtector.Unprotect`。
  2. Protect：Protect 后 UTF8.GetBytes。
- 分支与异常：保护器异常向上
- 调用：`ISecretProtector`

## 近逐行中文伪代码

1. 注入 Db 与 ISecretProtector。
2. StoreTokens：加密双令牌、设过期与 scopes、connected/healthy。
3. GetValidAccessToken：无 token→missing；过期标记；5 分钟内且可刷新则 Refresh+Save，失败 refresh-failed；返回解密 access。
4. ClearTokens：清空并 not-connected/missing。
5. Protect/Unprotect 经 UTF8 与 SecretProtector 往返。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.Calendar/Services/OutlookTokenService.cs",
      "label": "OutlookTokenService",
      "path": "src/modules/Pim.Module.Calendar/Services/OutlookTokenService.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.Calendar/Services/OutlookTokenService.cs.md",
      "layer": "module.calendar",
      "kind": "service"
    }
  ],
  "edges": [
    { "from": "src/modules/Pim.Module.Calendar/Services/OutlookTokenService.cs", "to": "src/Pim.Infrastructure/Data/PimDbContext.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Calendar/Services/OutlookTokenService.cs", "to": "src/Pim.Infrastructure/Secrets/ISecretProtector.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Calendar/Services/OutlookTokenService.cs", "to": "src/modules/Pim.Module.Calendar/Entities/OutlookConnectionEntity.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Calendar/Services/OutlookTokenService.cs", "to": "src/modules/Pim.Module.Calendar/Services/IMicrosoftGraphClient.cs", "type": "calls" }
  ]
}
```
