# tests/Pim.UnitTests/Today/TodaySectionServiceTests.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.UnitTests
- 职责：Today 路径常量、非法日期响应、注册表/单节查询、PC 业务日 4 点切日、Provider 异常与取消。
- 主要依赖：`TodaySectionService`、`TodayEndpointPaths`、`TodayEndpoints`、`ITodaySectionProvider`
- 被谁使用：xUnit

## 函数级结构化伪代码

### TodaySectionServiceTests
#### TodayEndpointPaths_AreStable()
- 输入：无
- 输出：无
- 副作用：无
- 步骤：Sections 路径；Section id；含 `?` 的 id 编码
- 分支与异常：无
- 调用：`TodayEndpointPaths`

#### ToInvalidDateResult_ReturnsBadRequest()
- 输入：无
- 输出：无
- 副作用：无
- 步骤：400 + 固定英文错误消息
- 分支与异常：无
- 调用：`TodayEndpoints.ToInvalidDateResult`

#### GetRegistryAsync_ReturnsProviderMetadataWithoutUiFields()
- 输入：FakeProvider
- 输出：无
- 副作用：无
- 步骤：Date/PcBusinessDate；Available；无 Details 链；Self 带 date
- 分支与异常：无
- 调用：`TodaySectionService.GetRegistryAsync`

#### GetRegistryAsync_UsesPreviousPcBusinessDateBeforeFourAm / ExplicitAmTime
- 输入：`T03:30:00` / `3 AM` 时间串
- 输出：无
- 副作用：无
- 步骤：registry.Date 仍日历日；PcBusinessDate 前一日
- 分支与异常：无
- 调用：GetRegistryAsync

#### GetSectionAsync_ReturnsProviderPayload / Null / Unavailable / Cancellation
- 输入：Fake / 未知 id / Throwing / 已取消 token
- 输出：无
- 副作用：BuildCount 递增
- 步骤：
  1. 正常 Normal + Build 一次
  2. 未知 null
  3. 抛错 → Unavailable、中文消息、不泄露 boom
  4. 取消 → OperationCanceledException
- 分支与异常：取消向上抛
- 调用：`GetSectionAsync`

#### CreateService / FakeProvider / ThrowingProvider / CancelingProvider
- 输入：providers
- 输出：服务与替身
- 副作用：无
- 步骤：NullLogger；Build 返回 DTO 或抛/取消
- 分支与异常：Throwing/Canceling 见上
- 调用：`TodaySectionService` 构造

## 近逐行中文伪代码

1. 路径稳定性与 URL 编码
2. 非法日期 BadRequest
3. 注册表元数据与 Self 链接
4. 4 点前 PC 业务日回退（两种时间解析）
5. 单节成功/未知/故障降级/取消
6. 三个 Provider 替身

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/Pim.UnitTests/Today/TodaySectionServiceTests.cs",
      "label": "TodaySectionServiceTests",
      "path": "tests/Pim.UnitTests/Today/TodaySectionServiceTests.cs",
      "doc": "docs/pseudocode/files/tests/Pim.UnitTests/Today/TodaySectionServiceTests.cs.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "tests/Pim.UnitTests/Today/TodaySectionServiceTests.cs", "to": "src/Pim.Api/Today/TodaySectionService.cs", "type": "tests" },
    { "from": "tests/Pim.UnitTests/Today/TodaySectionServiceTests.cs", "to": "src/Pim.Api/Endpoints/TodayEndpoints.cs", "type": "tests" }
  ]
}
```
