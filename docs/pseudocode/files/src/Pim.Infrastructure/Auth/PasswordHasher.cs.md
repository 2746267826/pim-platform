# src/Pim.Infrastructure/Auth/PasswordHasher.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Infrastructure
- 职责：基于 BCrypt 的密码哈希与校验静态工具（workFactor=10）
- 主要依赖：`BCrypt.Net.BCrypt`
- 被谁使用：`Pim.Api/Endpoints/AuthEndpoints.cs`（注册 `Hash`、登录 `Verify`）

## 函数级结构化伪代码

### PasswordHasher
#### static string Hash(string password)
- 输入：明文密码
- 输出：BCrypt 哈希字符串
- 副作用：无（纯计算）
- 步骤：
  1. 若 `password` 为 null/空白 → 抛 `ArgumentException`
  2. 调用 `BCrypt.HashPassword(password, workFactor: 10)` 并返回
- 分支与异常：空密码抛异常
- 调用：`BCrypt.Net.BCrypt.HashPassword`

#### static bool Verify(string password, string hash)
- 输入：明文密码；已存哈希
- 输出：是否匹配
- 副作用：无
- 步骤：
  1. 密码空白 → 抛 `ArgumentException`
  2. 哈希空白 → 抛 `ArgumentException`
  3. 返回 `BCrypt.Verify(password, hash)`
- 分支与异常：参数校验失败抛异常
- 调用：`BCrypt.Net.BCrypt.Verify`

## 近逐行中文伪代码

1. 命名空间 `Pim.Infrastructure.Auth`
2. 静态类 `PasswordHasher`
3. `Hash`：空白密码则抛「Password cannot be null or whitespace.」
4. 以 workFactor=10 调用 BCrypt 哈希并返回
5. `Verify`：空白密码抛同样参数异常
6. 空白 hash 抛「Hash cannot be null or whitespace.」
7. 返回 BCrypt 校验结果

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/Pim.Infrastructure/Auth/PasswordHasher.cs",
      "label": "PasswordHasher",
      "path": "src/Pim.Infrastructure/Auth/PasswordHasher.cs",
      "doc": "docs/pseudocode/files/src/Pim.Infrastructure/Auth/PasswordHasher.cs.md",
      "layer": "infrastructure",
      "kind": "service"
    }
  ],
  "edges": [
    { "from": "src/Pim.Infrastructure/Auth/PasswordHasher.cs", "to": "BCrypt.Net.BCrypt", "type": "calls" },
    { "from": "src/Pim.Api/Endpoints/AuthEndpoints.cs", "to": "src/Pim.Infrastructure/Auth/PasswordHasher.cs", "type": "calls" }
  ]
}
```
