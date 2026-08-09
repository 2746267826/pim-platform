# src/client-web/src/api/status.ts

## 元信息
- 语言：TypeScript
- 程序集或包：client-web
- 职责：系统状态 API 客户端；将后端数字/字符串健康枚举与组件字段规范化为前端 `SystemStatusSummary`/`SystemStatusDetail`。
- 主要依赖：`./client`（`apiGet`）、`../types`
- 被谁使用：状态页/仪表盘健康展示

## 函数级结构化伪代码

### 映射表与内部类型
#### statusByNumber / statusNames / healthStatusLabels / componentKindLabels
- 输入：无
- 输出：状态数字→枚举、合法名集合、中文标签、组件 kind 中文映射
- 副作用：无
- 步骤：静态定义 0–3、Unknown/Healthy/Warning/Critical、Api/Database/Daemon 等标签
- 调用：无

### 规范化
#### normalizeHealthStatus(value: unknown): PimHealthStatus
- 输入：任意原始 status
- 输出：标准四态之一
- 步骤：number 查表；纯数字字符串转 number；合法字符串直接用；否则 Unknown
- 调用：无

#### textOrEmpty(value) / normalizeLabel / normalizeKind / normalizeDetails
- 输入：未知字段
- 输出：字符串、展示标签、kind 字符串、details 字典
- 步骤：null/undefined→空；label 若是枚举英文名则换中文；kind 纯数字→空；details 非对象→{}
- 调用：`getHealthStatusLabel`、`textOrEmpty`

#### getHealthStatusLabel(status) / getComponentKindLabel(kind)
- 输入：状态或 kind
- 输出：中文（或原 kind）
- 调用：映射表

#### normalizeStatusSummary(raw) / normalizeStatusComponent(raw) / normalizeStatusDetail(raw)
- 输入：原始 JSON
- 输出：类型安全的 summary/component/detail
- 步骤：强制 object 断言；规范化 status/label/message/checkedAt；components 数组 map；nextSteps 过滤空串
- 调用：上述 normalize*

### API
#### getStatusSummary() / getStatusDetail()
- 输入：无
- 输出：规范化后的摘要或详情
- 副作用：GET `/status/summary`、`/status/`
- 步骤：`apiGet` → 取 `response.data` → normalize
- 调用：`apiGet`、`normalizeStatusSummary`/`normalizeStatusDetail`

## 近逐行中文伪代码

1. 导入 `apiGet` 与状态相关类型
2. `statusApiPaths.summary` = `/status/summary`；`detail` = `/status/`
3. 数字 0–3 映射 Unknown/Healthy/Warning/Critical
4. 中文健康标签与组件 kind 标签表
5. 定义 Raw* 宽松类型
6. `normalizeHealthStatus`：数字/数字串/合法名，否则 Unknown
7. `textOrEmpty`：转字符串或空
8. `normalizeLabel`：若 label 本身是枚举英文则用中文健康文案
9. `normalizeKind`：纯数字 kind 清空
10. `normalizeDetails`：对象逐值转字符串
11. 导出 `getHealthStatusLabel`、`getComponentKindLabel`
12. `normalizeStatusSummary`/`normalizeStatusComponent`/`normalizeStatusDetail`
13. `getStatusSummary`/`getStatusDetail`：请求后规范化返回

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-web/src/api/status.ts",
      "label": "statusApi",
      "path": "src/client-web/src/api/status.ts",
      "doc": "docs/pseudocode/files/src/client-web/src/api/status.ts.md",
      "layer": "client-web",
      "kind": "other"
    }
  ],
  "edges": [
    { "from": "src/client-web/src/api/status.ts", "to": "src/client-web/src/api/client.ts", "type": "depends_on" },
    { "from": "src/client-web/src/api/status.ts", "to": "src/client-web/src/types", "type": "depends_on" },
    { "from": "src/client-web/src/api/status.ts", "to": "/status", "type": "http" }
  ]
}
```
