# src/Pim.Core/Exceptions/DomainException.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Core
- 职责：领域层可预期业务异常，携带整数 `ErrorCode` 与消息
- 主要依赖：`System.Exception`
- 被谁使用：领域服务/用例在业务规则失败时抛出；API 中间件或过滤器映射为 HTTP 错误响应

## 函数级结构化伪代码

### DomainException
#### DomainException(int errorCode, string message) : Exception
- 输入：`errorCode` 业务错误码；`message` 错误描述
- 输出：构造完成的异常实例
- 副作用：无（构造期写只读属性）
- 步骤：
  1. 调用基类 `Exception(message)` 保存消息
  2. 将 `errorCode` 赋给属性 `ErrorCode`
- 分支与异常：无额外分支
- 调用：`Exception` 基类构造

#### int ErrorCode { get }
- 输入：无
- 输出：构造时写入的错误码
- 副作用：无
- 步骤：
  1. 返回只读属性值
- 分支与异常：无
- 调用：无

## 近逐行中文伪代码

1. 声明命名空间 `Pim.Core.Exceptions`
2. 定义类 `DomainException`，继承 `Exception`
3. 公开只读属性 `ErrorCode`（int）
4. 构造函数接收 `errorCode` 与 `message`
5. 以 `message` 调用基类 `Exception` 构造函数
6. 将 `errorCode` 赋给 `ErrorCode` 属性

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/Pim.Core/Exceptions/DomainException.cs",
      "label": "DomainException",
      "path": "src/Pim.Core/Exceptions/DomainException.cs",
      "doc": "docs/pseudocode/files/src/Pim.Core/Exceptions/DomainException.cs.md",
      "layer": "core",
      "kind": "other"
    }
  ],
  "edges": [
    { "from": "src/Pim.Core/Exceptions/DomainException.cs", "to": "System.Exception", "type": "extends" }
  ]
}
```
