# src/modules/Pim.Module.Calendar/Services/OutlookGraphModels.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.Calendar
- 职责：定义 Microsoft Graph / 设备码 OAuth 客户端契约与相关 DTO（设备码、令牌、Delta 页、事件、时区时间）。
- 主要依赖：无外部程序集（仅 BCL + 本文件类型）
- 被谁使用：Outlook Graph 客户端实现类；Outlook 同步服务调用 `IMicrosoftGraphClient`

## 函数级结构化伪代码

### IMicrosoftGraphClient
#### Task<DeviceCodeResult> RequestDeviceCodeAsync(string tenant, string clientId, string scopes, CancellationToken ct)
- 输入：租户、客户端 Id、作用域、取消令牌
- 输出：设备码流程启动结果
- 副作用：对外 HTTP 请求（实现侧）
- 步骤：
  1. 向身份端点请求设备码
  2. 返回 `DeviceCode`/`UserCode`/`VerificationUri` 等
- 分支与异常：网络/协议错误由实现抛出；可取消
- 调用：实现侧 HTTP

#### Task<TokenResult> PollDeviceCodeAsync(string tenant, string clientId, string deviceCode, CancellationToken ct)
- 输入：租户、客户端 Id、设备码、取消令牌
- 输出：访问/刷新令牌结果
- 副作用：轮询令牌端点（实现侧）
- 步骤：
  1. 使用设备码换取 token
- 分支与异常：授权未完成/过期等由实现处理
- 调用：实现侧 HTTP

#### Task<TokenResult> RefreshAsync(string tenant, string clientId, string refreshToken, string scopes, CancellationToken ct)
- 输入：租户、客户端 Id、刷新令牌、作用域、取消令牌
- 输出：新的 `TokenResult`
- 副作用：刷新令牌 HTTP 调用
- 步骤：
  1. 用 refresh token 换新 access/refresh token
- 分支与异常：刷新失败由实现抛出
- 调用：实现侧 HTTP

#### Task<GraphDeltaPage> GetDeltaPageAsync(string accessToken, string url, CancellationToken ct)
- 输入：访问令牌、Graph delta/next URL、取消令牌
- 输出：一页事件 + NextLink/DeltaLink
- 副作用：Graph GET（实现侧）
- 步骤：
  1. 带 token 请求 url，反序列化为事件列表与链接
- 分支与异常：401/网络错误由实现处理
- 调用：实现侧 HTTP

#### Task<GraphEvent> PatchEventAsync(string accessToken, string eventId, string changeKey, object patch, CancellationToken ct)
- 输入：访问令牌、事件 Id、变更键、补丁对象、取消令牌
- 输出：更新后的 `GraphEvent`
- 副作用：Graph PATCH（实现侧）
- 步骤：
  1. 对指定事件应用 patch（含并发 changeKey）
- 分支与异常：冲突/权限错误由实现抛出
- 调用：实现侧 HTTP

### DeviceCodeResult / TokenResult / GraphDeltaPage / GraphEvent / GraphDateTimeTimeZone
#### 记录类型（不可变 DTO）
- 输入：构造位置参数
- 输出：只读数据载体
- 副作用：无
- 步骤：
  1. `DeviceCodeResult`：设备码、用户码、验证 URI、消息、过期秒数
  2. `TokenResult`：AccessToken、RefreshToken、过期秒数、Scopes
  3. `GraphDeltaPage`：Events 列表、NextLink、DeltaLink
  4. `GraphEvent`：Id、Subject、BodyPreview、Start/End、LastModified、ICalUId、ChangeKey、ETag、Location、WebLink
  5. `GraphDateTimeTimeZone`：DateTime 字符串 + 可选 TimeZone
- 分支与异常：无
- 调用：无

## 近逐行中文伪代码

1. 命名空间 `Pim.Module.Calendar.Services`
2. 接口 `IMicrosoftGraphClient` 声明五类异步操作：
3.   请求设备码、轮询设备码换 token、刷新 token、拉取 delta 页、PATCH 事件
4. 密封记录 `DeviceCodeResult`：设备码流程展示字段
5. 密封记录 `TokenResult`：访问/刷新令牌与过期、作用域
6. 密封记录 `GraphDeltaPage`：事件页与 next/delta 链接
7. 密封记录 `GraphEvent`：Graph 日历事件核心字段
8. 密封记录 `GraphDateTimeTimeZone`：带时区的日期时间字符串对
9. 文件无实现逻辑，纯契约与 DTO

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.Calendar/Services/OutlookGraphModels.cs",
      "label": "OutlookGraphModels",
      "path": "src/modules/Pim.Module.Calendar/Services/OutlookGraphModels.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.Calendar/Services/OutlookGraphModels.cs.md",
      "layer": "module.calendar",
      "kind": "dto"
    }
  ],
  "edges": [
    { "from": "src/modules/Pim.Module.Calendar/Services", "to": "src/modules/Pim.Module.Calendar/Services/OutlookGraphModels.cs", "type": "depends_on" }
  ]
}
```
