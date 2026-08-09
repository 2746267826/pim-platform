# src/Pim.Api/DTOs/AuthDtos.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Api
- 职责：认证 API 请求/响应 DTO（注册、登录、刷新、令牌响应、用户信息）
- 主要依赖：`System.ComponentModel.DataAnnotations`（`Required`/`MaxLength`/`MinLength`/`EmailAddress`）
- 被谁使用：`AuthEndpoints` 绑定请求体与返回 `AuthResponse`/`UserInfo`

## 函数级结构化伪代码

### RegisterRequest
#### record RegisterRequest(Username, Email, Password, DisplayName?)
- 输入：用户名（必填≤50）、邮箱（必填≤255 且 Email）、密码（必填 8..100）、可选显示名（≤100）
- 输出：注册请求 DTO
- 副作用：无（由模型验证管线校验注解）
- 步骤：
  1. 以位置参数构造不可变 record
- 分支与异常：注解违反时由 ASP.NET 模型验证产生 400
- 调用：无

### LoginRequest
#### record LoginRequest(Username, Password)
- 输入：用户名、密码（均 Required）
- 输出：登录请求 DTO
- 副作用：无
- 步骤：构造 record
- 分支与异常：缺字段 → 模型验证失败
- 调用：无

### RefreshRequest
#### record RefreshRequest(RefreshToken)
- 输入：刷新令牌字符串（Required）
- 输出：刷新请求 DTO
- 副作用：无
- 步骤：构造 record
- 分支与异常：缺字段 → 模型验证失败
- 调用：无

### AuthResponse
#### record AuthResponse(AccessToken, RefreshToken, ExpiresAt, User)
- 输入：访问令牌、刷新令牌、过期时间、`UserInfo`
- 输出：认证成功响应
- 副作用：无
- 步骤：绑定令牌与用户信息
- 分支与异常：无
- 调用：`UserInfo`

### UserInfo
#### record UserInfo(Id, Username, DisplayName, Role)
- 输入：用户 Guid、用户名、显示名、角色
- 输出：面向客户端的用户摘要
- 副作用：无
- 步骤：构造 record
- 分支与异常：无
- 调用：无

## 近逐行中文伪代码

1. 引入 DataAnnotations
2. 命名空间 `Pim.Api.DTOs`
3. `RegisterRequest`：Username/Email/Password 带长度与邮箱校验；DisplayName 可空
4. `LoginRequest`：Username + Password 必填
5. `RefreshRequest`：RefreshToken 必填
6. `AuthResponse`：AccessToken、RefreshToken、ExpiresAt、User
7. `UserInfo`：Id、Username、DisplayName、Role

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/Pim.Api/DTOs/AuthDtos.cs",
      "label": "AuthDtos",
      "path": "src/Pim.Api/DTOs/AuthDtos.cs",
      "doc": "docs/pseudocode/files/src/Pim.Api/DTOs/AuthDtos.cs.md",
      "layer": "api",
      "kind": "dto"
    }
  ],
  "edges": [
    { "from": "src/Pim.Api/Endpoints/AuthEndpoints.cs", "to": "src/Pim.Api/DTOs/AuthDtos.cs", "type": "depends_on" }
  ]
}
```
