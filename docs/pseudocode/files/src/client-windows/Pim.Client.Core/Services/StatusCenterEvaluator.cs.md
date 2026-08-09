# src/client-windows/Pim.Client.Core/Services/StatusCenterEvaluator.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Client.Core
- 职责：根据认证、ActivityWatch/KeyStats 状态、跳过原因与 AW 队列深度，汇总状态中心评级文案。
- 主要依赖：无外部服务（纯静态逻辑）
- 被谁使用：Windows 客户端状态窗口/状态中心 UI 逻辑

## 函数级结构化伪代码

### StatusCenterEvaluator
#### static string Rate(bool authenticated, string activityWatchState, string keyStatsState, string? keyStatsSkipReason, int awQueueCount)
- 输入：是否已认证；AW/KeyStats 状态字符串；KeyStats 跳过原因；AW 队列条数
- 输出：`"不可用"` | `"正常"` | `"部分异常"`
- 副作用：无
- 步骤：
  1. awOk = activityWatchState 等于 `"Available"`（忽略大小写）
  2. ksOk = keyStatsState 等于 `"Available"`
  3. hasSkip = keyStatsSkipReason 非空白
  4. hasQueue = awQueueCount > 0
  5. 若未认证，或 AW 与 KeyStats 都不可用 → `"不可用"`
  6. 若两者都 Available 且无 skip 且无队列 → `"正常"`
  7. 否则 → `"部分异常"`
- 分支与异常：无异常；状态字符串比较 OrdinalIgnoreCase
- 调用：`string.Equals`、`string.IsNullOrWhiteSpace`

## 近逐行中文伪代码

1. 命名空间 `Pim.Client.Core.Services`
2. 静态类 `StatusCenterEvaluator`
3. `Rate`：计算 awOk/ksOk/hasSkip/hasQueue
4. 未登录或双采集均非 Available → 不可用
5. 双 Available 且无跳过无队列 → 正常
6. 其余 → 部分异常

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-windows/Pim.Client.Core/Services/StatusCenterEvaluator.cs",
      "label": "StatusCenterEvaluator",
      "path": "src/client-windows/Pim.Client.Core/Services/StatusCenterEvaluator.cs",
      "doc": "docs/pseudocode/files/src/client-windows/Pim.Client.Core/Services/StatusCenterEvaluator.cs.md",
      "layer": "client-windows",
      "kind": "service"
    }
  ],
  "edges": [
    { "from": "src/client-windows/Pim.Client.App/StatusWindow.xaml.cs", "to": "src/client-windows/Pim.Client.Core/Services/StatusCenterEvaluator.cs", "type": "calls" }
  ]
}
```
