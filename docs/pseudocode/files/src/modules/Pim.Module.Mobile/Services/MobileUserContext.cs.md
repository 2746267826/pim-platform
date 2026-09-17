# src/modules/Pim.Module.Mobile/Services/MobileUserContext.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.Mobile
- 职责：从当前用户服务提取必填 UserId，未登录则抛领域异常。
- 主要依赖：`ICurrentUserService`、`DomainException`
- 被谁使用：Mobile 模块各需认证的服务/端点逻辑

## 函数级结构化伪代码

### MobileUserContext（internal static）
#### `static Guid RequireUserId(ICurrentUserService currentUser)`
- 输入：当前用户服务
- 输出：非空 `Guid` 用户 ID
- 副作用：无
- 步骤：
  1. 若 `currentUser.UserId` 有值则返回。
  2. 否则抛 `DomainException(6200, "Mobile endpoints require an authenticated user.")`。
- 分支与异常：未认证 → DomainException 6200
- 调用：读取 `ICurrentUserService.UserId`

## 近逐行中文伪代码

1. 引入 `Pim.Core.Exceptions`、`Pim.Infrastructure.Auth`。
2. internal static 类 `MobileUserContext`。
3. `RequireUserId`：`UserId ?? throw DomainException(6200, 需认证英文消息)`。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.Mobile/Services/MobileUserContext.cs",
      "label": "MobileUserContext",
      "path": "src/modules/Pim.Module.Mobile/Services/MobileUserContext.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.Mobile/Services/MobileUserContext.cs.md",
      "layer": "module.mobile",
      "kind": "service"
    }
  ],
  "edges": [
    { "from": "src/modules/Pim.Module.Mobile/Services/MobileUserContext.cs", "to": "src/Pim.Infrastructure/Auth/CurrentUserService.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Mobile/Services/MobileUserContext.cs", "to": "src/Pim.Core/Exceptions/DomainException.cs", "type": "depends_on" }
  ]
}
```
