# src/client-web/src/components/ai/AiRequestDetailPanel.tsx

## 元信息
- 语言：TypeScript/React
- 程序集或包：client-web
- 职责：AI 请求日志详情面板——空态/加载/错误/完整字段与 JSON 代码块展示。
- 主要依赖：`AiRequestLogDetail`/`AiRequestStatus` 类型、`MetricCard`、`StatusBadge`
- 被谁使用：AI 请求列表页/工作台选中详情区

## 函数级结构化伪代码

### 辅助
#### `statusLabels` / `statusTone(status)`
- 中文标签映射；Succeeded→activity；FailedValidation|Blocked→warning；其余→danger。

#### `formatNumber` / `formatCost` / `formatJson`
- 空值 `-`；数字 zh-CN 本地化；成本 currency+toFixed(4)；JSON 尝试 pretty-print，失败返回原文。

#### `InfoRow` / `CodeBlock`
- 展示 label/value 与标题+pre/code 代码块（max-h-72 滚动）。

### `AiRequestDetailPanel({ detail, isLoading, error })`
- 输入：可选详情、加载与错误
- 输出：React 节点
- 副作用：无（纯展示）
- 步骤：
  1. 无 detail 且非加载且无错误 → 空态「选择请求」。
  2. 否则渲染标题、id、StatusBadge。
  3. 加载文案；错误 message。
  4. 有 detail：网格 InfoRow（模块/用途/模型/服务商/Correlation/LiteLLM/尝试/来源对象）。
  5. 四枚 MetricCard：输入/输出/总 Token、预估成本。
  6. 有错误码/信息/摘要则红框展示。
  7. 六个 CodeBlock：消息列表、请求载荷、响应文本、原始响应、解析 JSON、结构校验错误。
- 分支与异常：空态/加载/错误/有数据四态
- 调用：MetricCard、StatusBadge、format*

## 近逐行中文伪代码

1. 引入类型与 MetricCard/StatusBadge。
2. Props：detail/isLoading/error。
3. 状态中文与 tone 映射；数字/成本/JSON 格式化。
4. InfoRow、CodeBlock 小组件。
5. 主组件：三无则空态引导。
6. 有内容时显示标题与徽章；加载/错误提示。
7. detail 存在时渲染元信息网格、Token/成本卡片、错误区、多段 JSON/文本块。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-web/src/components/ai/AiRequestDetailPanel.tsx",
      "label": "AiRequestDetailPanel",
      "path": "src/client-web/src/components/ai/AiRequestDetailPanel.tsx",
      "doc": "docs/pseudocode/files/src/client-web/src/components/ai/AiRequestDetailPanel.tsx.md",
      "layer": "client-web",
      "kind": "ui"
    }
  ],
  "edges": [
    { "from": "src/client-web/src/components/ai/AiRequestDetailPanel.tsx", "to": "src/client-web/src/types", "type": "depends_on" },
    { "from": "src/client-web/src/components/ai/AiRequestDetailPanel.tsx", "to": "src/client-web/src/ui/MetricCard", "type": "depends_on" },
    { "from": "src/client-web/src/components/ai/AiRequestDetailPanel.tsx", "to": "src/client-web/src/ui/StatusBadge", "type": "depends_on" }
  ]
}
```
