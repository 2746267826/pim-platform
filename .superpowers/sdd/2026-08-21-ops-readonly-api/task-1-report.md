# 任务1报告：Ops鉴权底座

## 实现内容
- OpsOptions：绑定 PIM_OPS_KEY / PIM_OPS_ALLOWED_CIDRS / PIM_OPS_RO_CONNECTION
- OpsKeyValidator：逗号多值解析、Trim、FixedTimeEquals常时比较、大小写敏感、CIDR校验（IPv4/IPv6，空CIDR放行，非法IP拒绝）
- OpsKeyMiddleware：仅拦截 /api/v1/ops/*，空密钥503、缺/错key 401、CIDR不匹配403，成功注入 ClaimsPrincipal(role=ops-reader)
- Program.cs：注册 OpsOptions 配置，UseMiddleware<OpsKeyMiddleware> 在 UseAuthentication 之前

## 测试
- OpsKeyValidatorTests：6条Theory（null/空/trim/多值/大小写）+ CIDR范围 + 空CIDR放行 + HasKeys，共9用例
- OpsEndpointsTests：6个集成测试（TestServer）：无key 401、有key 200、错key 401、未配置key 503、CIDR拦截403、非ops路径放行
- TDD证据：
  - RED：`dotnet test --filter OpsKeyValidatorTests` => CS0234 Ops不存在 FAIL
  - GREEN：实现后 => Passed 9/9；Ops全量 => Passed 29/29
- 全量回归：`dotnet test Pim.sln --no-restore` => 1639 passed, 0 failed

## 修改文件
- 新建：src/Pim.Api/Infrastructure/Ops/OpsOptions.cs
- 新建：src/Pim.Api/Infrastructure/Ops/OpsKeyValidator.cs
- 新建：src/Pim.Api/Infrastructure/Ops/OpsKeyMiddleware.cs
- 修改：src/Pim.Api/Program.cs
- 新建：tests/Pim.UnitTests/Api/OpsKeyValidatorTests.cs
- 新建：tests/Pim.UnitTests/Api/OpsEndpointsTests.cs

## 自审
- 完整性：计划要求的3个新建+1修改+2测试全部完成，职责单一，遵循现有模式
- 质量：FixedTimeEquals+长度校验防时序，非空trim，CIDR位掩码逐字节比较，支持多CIDR逗号分隔
- 纪律：TDD先红后绿，最小实现，聚焦测试后再全量回归
- 测试：覆盖鉴权核心路径，边界（空/大小写/CIDR/非ops）均覆盖

## 问题
- 无阻塞问题。后续任务2/3可直接依赖 OpsKeyValidator 与 Middleware。

---

## 修复轮次 1（2026-08-21，第1/5轮，唤回原实现者）

### 审查发现修复
- **快照问题** `OpsKeyMiddleware.cs:33-36` / `OpsKeyValidator.cs:92-95`：移除构造时一次性 `new OpsKeyValidator(cfg[...])`，改为 `IConfiguration` 持有 + `InvokeAsync` 内每请求 `CreateValidator()` 实时读取 `PIM_OPS_KEY` / `PIM_OPS_ALLOWED_CIDRS`，配置热更新即时生效。
- **JWT覆盖风险** `OpsKeyMiddleware.cs:70` / `Program.cs:240`：将 `UseMiddleware<OpsKeyMiddleware>` 从 `UseAuthentication` 之前移至 `UseAuthentication` 之后、`UseAuthorization` 之前；并将 `ctx.User = new ClaimsPrincipal(...)` 改为 `ctx.User.AddIdentity(new ClaimsIdentity(ClaimTypes.Role+"role"雙寫))` 合并身份，避免被认证中间件覆盖，保持与 JWT 正交。
- **CIDR静默fail-open** `OpsKeyValidator.cs:130-144`：`ParseCidrs` 改为严格模式，非法条目收集后抛 `OptionsValidationException(nameof(OpsOptions), ...)`；`Program.cs` 新增 `AddOptions<OpsOptions>().Validate(...).ValidateOnStart()` 启动时校验，无效 `PIM_OPS_ALLOWED_CIDRS` 直接阻断启动，不再空列表放行。
- **次要**：`Claim("role")` 改为 `ClaimTypes.Role` 主声明并兼容保留 `"role"`；时序侧信道长度泄露标注可接受，保留 `FixedTimeEquals+长度校验` 模式。

### 实测验证
- `dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter FullyQualifiedName~Ops --no-restore` => Passed 29/29, Failed 0
- `dotnet test Pim.sln --no-restore` => Passed 1639/1639, Failed 0, Skipped 0
- 覆盖文件：`OpsKeyValidatorTests`（9用例）+ `OpsEndpointsTests`（6用例）+ 其余 Ops 相关共29用例

### 提交
- Base: 0de90632
- Head: e55ad97de52cb296f00f90c3878cbd49fe6f582f（本轮修复提交）

### 遗留
- 时序侧信道长度泄露已评估为可接受风险，未做额外填充比对。
