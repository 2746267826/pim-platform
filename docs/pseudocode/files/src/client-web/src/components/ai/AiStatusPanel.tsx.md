# src/client-web/src/components/ai/AiStatusPanel.tsx

## 元信息
- 语言：TypeScript/TSX
- 程序集或包：client-web
- 职责：展示 LiteLLM/AI 状态字段；触发健康检查与测试连接 mutation，并失效相关 React Query 缓存。
- 主要依赖：`@tanstack/react-query`、`../../api/ai`、`AiStatus`、`StatusBadge`
- 被谁使用：AI 管理/状态页面

## 函数级结构化伪代码

### formatDateTime(value?)
- 输入：可选 ISO/日期字符串
- 输出：中文本地化时间或 `'未检查'`/原值
- 副作用：无
- 步骤：空 → 未检查；Invalid Date → 原字符串；否则 `toLocaleString('zh-CN')`
- 分支与异常：NaN 时间
- 调用：`Date`

### Field({ label, value })
- 输入：标签与展示值
- 输出：dt/dd 小字段 UI
- 副作用：无
- 步骤：渲染 truncate 标签与 break-all 值（空显示 `-`）
- 分支与异常：无
- 调用：无

### AiStatusPanel({ status, isLoading, error })
- 输入：可选状态、加载、错误
- 输出：状态面板 JSX
- 副作用：mutation 触发 API；成功 invalidate 查询
- 步骤：
  1. `useQueryClient`；定义 `invalidateAiQueries` 失效 ai-status/usage/requests/detail
  2. `healthMutation` = runAiHealthCheck；`testMutation` = runAiTest；onSuccess 失效
  3. busy = 任一 pending；actionError = 任一 error
  4. 渲染标题、启用 StatusBadge、加载/错误提示、字段网格、操作错误、两个按钮
- 分支与异常：加载文案；error/actionError 红框；busy 禁用按钮
- 调用：`runAiHealthCheck`、`runAiTest`、`invalidateQueries`

## 近逐行中文伪代码

1. 引入 useMutation/useQueryClient、runAiHealthCheck/runAiTest、AiStatus、StatusBadge
2. Props：status/isLoading/error
3. formatDateTime：空/非法/本地化
4. Field 展示 label/value
5. AiStatusPanel：拿 queryClient
6. invalidate 四个 ai 相关 queryKey
7. health/test 两个 mutation，成功后 invalidate
8. busy 与 actionError 聚合
9. section：标题「LiteLLM 状态」+ 启用徽章
10. 加载中/error 提示
11. dl 网格：服务商、默认模型、Base URL、上次健康检查、最近成功、最后错误
12. actionError 提示
13. 按钮：健康检查 / 测试连接（pending 文案与 disabled）

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-web/src/components/ai/AiStatusPanel.tsx",
      "label": "AiStatusPanel",
      "path": "src/client-web/src/components/ai/AiStatusPanel.tsx",
      "doc": "docs/pseudocode/files/src/client-web/src/components/ai/AiStatusPanel.tsx.md",
      "layer": "client-web",
      "kind": "ui"
    }
  ],
  "edges": [
    { "from": "src/client-web/src/components/ai/AiStatusPanel.tsx", "to": "src/client-web/src/api/ai.ts", "type": "calls" },
    { "from": "src/client-web/src/components/ai/AiStatusPanel.tsx", "to": "src/client-web/src/ui/StatusBadge", "type": "depends_on" },
    { "from": "src/client-web/src/components/ai/AiStatusPanel.tsx", "to": "src/client-web/src/types", "type": "depends_on" }
  ]
}
```
