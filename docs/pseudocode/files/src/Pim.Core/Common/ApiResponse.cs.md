# src/Pim.Core/Common/ApiResponse.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Core
- 职责：统一 API 响应包装类型，提供成功/失败工厂方法
- 主要依赖：无（仅 BCL `DateTimeOffset`）
- 被谁使用：`src/Pim.Api` 各 Endpoints、`src/modules/*` 模块端点、`src/Pim.Api/Middleware/ExceptionMiddleware.cs`；Windows 客户端与 Web 侧有同名契约镜像

## 函数级结构化伪代码

### ApiResponse\<T\>
#### 记录主构造 ApiResponse\<T\>(Code, Message, Data, Timestamp)
- 输入：`Code` 业务码；`Message` 说明；`Data` 载荷（可空）；`Timestamp` 时间戳
- 输出：不可变响应记录实例
- 副作用：无
- 步骤：
  1. 以位置参数保存四字段
- 分支与异常：无
- 调用：无

#### static Ok(T data)
- 输入：`data` 成功载荷
- 输出：`ApiResponse<T>`，`Code=0`，`Message="success"`，`Data=data`，`Timestamp=UtcNow`
- 副作用：读取当前 UTC 时间
- 步骤：
  1. 构造 `new(0, "success", data, DateTimeOffset.UtcNow)`
  2. 返回该实例
- 分支与异常：无
- 调用：`DateTimeOffset.UtcNow`

#### static Error(int code, string message)
- 输入：`code` 错误码；`message` 错误说明
- 输出：`ApiResponse<T>`，`Data=default`，`Timestamp=UtcNow`
- 副作用：读取当前 UTC 时间
- 步骤：
  1. 构造 `new(code, message, default, DateTimeOffset.UtcNow)`
  2. 返回该实例
- 分支与异常：无
- 调用：`DateTimeOffset.UtcNow`

## 近逐行中文伪代码

1. 命名空间 `Pim.Core.Common`
2. 定义泛型记录 `ApiResponse<T>`，字段：`Code`、`Message`、`Data`（可空）、`Timestamp`
3. 静态方法 `Ok(data)`：返回码 0、消息 `"success"`、数据为入参、时间戳为 UTC 现在
4. 静态方法 `Error(code, message)`：返回指定码与消息、数据为默认值、时间戳为 UTC 现在

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/Pim.Core/Common/ApiResponse.cs",
      "label": "ApiResponse<T>",
      "path": "src/Pim.Core/Common/ApiResponse.cs",
      "doc": "docs/pseudocode/files/src/Pim.Core/Common/ApiResponse.cs.md",
      "layer": "core",
      "kind": "dto"
    }
  ],
  "edges": [
    { "from": "src/Pim.Api/Middleware/ExceptionMiddleware.cs", "to": "src/Pim.Core/Common/ApiResponse.cs", "type": "depends_on" },
    { "from": "src/Pim.Api/Endpoints/DaemonEndpoints.cs", "to": "src/Pim.Core/Common/ApiResponse.cs", "type": "depends_on" },
    { "from": "src/Pim.Api/Endpoints/AuthEndpoints.cs", "to": "src/Pim.Core/Common/ApiResponse.cs", "type": "depends_on" },
    { "from": "src/Pim.Api/Endpoints/AiEndpoints.cs", "to": "src/Pim.Core/Common/ApiResponse.cs", "type": "depends_on" },
    { "from": "src/Pim.Api/Endpoints/StatusEndpoints.cs", "to": "src/Pim.Core/Common/ApiResponse.cs", "type": "depends_on" },
    { "from": "src/Pim.Api/Endpoints/TodayEndpoints.cs", "to": "src/Pim.Core/Common/ApiResponse.cs", "type": "depends_on" },
    { "from": "src/Pim.Api/Endpoints/OperationsEndpoints.cs", "to": "src/Pim.Core/Common/ApiResponse.cs", "type": "depends_on" },
    { "from": "src/Pim.Api/Endpoints/EndpointEndpoints.cs", "to": "src/Pim.Core/Common/ApiResponse.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Stats/StatsModule.cs", "to": "src/Pim.Core/Common/ApiResponse.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.QuickNotes/QuickNotesModule.cs", "to": "src/Pim.Core/Common/ApiResponse.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Mobile/MobileModule.cs", "to": "src/Pim.Core/Common/ApiResponse.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Calendar/CalendarModule.cs", "to": "src/Pim.Core/Common/ApiResponse.cs", "type": "depends_on" }
  ]
}
```
