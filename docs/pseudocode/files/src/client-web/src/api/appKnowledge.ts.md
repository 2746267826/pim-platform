# src/client-web/src/api/appKnowledge.ts

## 元信息
- 语言：TypeScript
- 程序集或包：client-web
- 职责：App 知识库 REST 客户端——路径常量、类型与 apps/contexts/suggestions 的 GET/POST/DELETE 封装。
- 主要依赖：`./client`（`apiGet`/`apiPost`/`apiDelete`）、`../types`
- 被谁使用：App 知识 UI 组件与分类建议确认流

## 函数级结构化伪代码

### 类型与路径
#### AppKnowledgePatternType / AppKnowledgeApp / AppKnowledgeContextPattern / SaveAppKnowledgeContextRequest / AppKnowledgeSuggestionPreview / AppKnowledgeSuggestionApply
- 输入：服务端 JSON 形状
- 输出：前端类型别名与接口
- 副作用：无
- 步骤：声明模式枚举联合、App/上下文/保存请求/建议预览与应用结果
- 分支与异常：无
- 调用：无

#### appKnowledgeApiPaths
- 输入：可选 search、appId、suggestion id
- 输出：相对 API 路径
- 步骤：`apps` 可带 `?search=`；`appContexts`；`contexts`；`suggestionPreview`/`suggestionApply`
- 调用：`encodeURIComponent`

### 导出函数
#### getAppKnowledgeApps(search?)
- 输入：可选搜索串
- 输出：`AppKnowledgeApp[]`（解包 `response.data`）
- 副作用：HTTP GET
- 步骤：`apiGet` → `then(r => r.data)`
- 调用：`apiGet`、`appKnowledgeApiPaths.apps`

#### getAppKnowledgeContexts(appId)
- 输入：App 签名 Id
- 输出：该 App 的上下文模式列表
- 副作用：HTTP GET
- 步骤：`apiGet(appContexts)` → data
- 调用：`apiGet`

#### saveAppKnowledgeContext(request)
- 输入：`SaveAppKnowledgeContextRequest`
- 输出：保存后的 `AppKnowledgeContextPattern`
- 副作用：HTTP POST `/pc/app-knowledge/contexts`
- 调用：`apiPost`

#### deleteAppKnowledgeContext(id)
- 输入：上下文 Id
- 输出：删除结果字符串 data
- 副作用：HTTP DELETE `contexts/{id}`
- 调用：`apiDelete`

#### previewAppKnowledgeSuggestion(id, request)
- 输入：建议 Id；类别/项目标签/日期范围
- 输出：`AppKnowledgeSuggestionPreview`
- 副作用：HTTP POST `suggestions/{id}/preview`
- 调用：`apiPost`

#### applyAppKnowledgeSuggestion(id, request)
- 输入：同上请求体
- 输出：`AppKnowledgeSuggestionApply`（含 auditId、status、message）
- 副作用：HTTP POST `suggestions/{id}/apply`
- 调用：`apiPost`

## 近逐行中文伪代码

1. 从 `./client` 导入 `apiGet`/`apiPost`/`apiDelete`
2. 导入 `ApiResponse`、`ActivityClassificationPreview` 类型
3. 导出模式联合类型：`app-default|domain|title|url-path|source-family`
4. 定义 `AppKnowledgeApp`：进程名、展示名、类别、生产力、来源、置信度、上下文计数等
5. 定义 `AppKnowledgeContextPattern`：模式类型/值、目标类别、项目标签、影响计数等
6. 定义保存请求、建议预览、建议应用结果接口
7. `appKnowledgeApiPaths`：组装 apps/contexts/suggestions 路径
8. `getAppKnowledgeApps`：GET apps，返回 data
9. `getAppKnowledgeContexts`：GET app 下 contexts
10. `saveAppKnowledgeContext`：POST contexts
11. `deleteAppKnowledgeContext`：DELETE contexts/id
12. `previewAppKnowledgeSuggestion`：POST preview
13. `applyAppKnowledgeSuggestion`：POST apply

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-web/src/api/appKnowledge.ts",
      "label": "appKnowledgeApi",
      "path": "src/client-web/src/api/appKnowledge.ts",
      "doc": "docs/pseudocode/files/src/client-web/src/api/appKnowledge.ts.md",
      "layer": "client-web",
      "kind": "other"
    }
  ],
  "edges": [
    { "from": "src/client-web/src/api/appKnowledge.ts", "to": "src/client-web/src/api/client.ts", "type": "depends_on" },
    { "from": "src/client-web/src/api/appKnowledge.ts", "to": "src/client-web/src/types", "type": "depends_on" },
    { "from": "src/client-web/src/api/appKnowledge.ts", "to": "/pc/app-knowledge", "type": "http" }
  ]
}
```
