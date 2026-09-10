# tests/client-web/appKnowledgeNavigation.test.tsx

## 元信息
- 语言：TypeScript / TSX
- 程序集或包：tests/client-web
- 职责：校验 App 知识库导航：侧栏项、Tabs 次级页、AppLayout 旧路由重定向。
- 主要依赖：`primaryNavItems`、`AppKnowledgeTabs`、typescript AST、react-router `matchRoutes`、react-dom/server
- 被谁使用：Node 测试执行

## 函数级结构化伪代码

### 辅助
#### test(name, run)
- 步骤：立即执行 run（无框架 runner）

#### findJsxAttribute / readStringAttribute / readElementAttribute
- 步骤：在 JsxAttributes 上按名找属性；读字符串字面量或表达式中的字符串；读 JSX 表达式元素

#### extractAppLayoutRoutes(): AppLayoutRoute[]
- 输入：磁盘 `AppLayout.tsx`
- 输出：`{path, redirectTo?}[]`
- 副作用：读文件
- 步骤：TS createSourceFile → 遍历 JsxSelfClosingElement 标签 Route → 解析 path 与 Navigate.to
- 调用：`ts.createSourceFile`、`readFileSync`

#### resolveAppLayoutPath(routes, initialPath)
- 步骤：matchRoutes 循环跟随 redirectTo；检测环；无匹配返回 null
- 调用：`matchRoutes`

### 用例
#### sidebar exposes app knowledge but not standalone classification pages
- 步骤：primaryNavItems labels 含「App知识库」；不含「分类管理」「分类树」

#### app knowledge tabs include category tree as a secondary page
- 步骤：SSR 渲染 AppKnowledgeTabs(active=categories)；HTML 含 App 列表/分类树/路径

#### app layout legacy knowledge routes resolve to current app knowledge pages
- 步骤：`/pc-categories` → categories；`/pc-classification` → `/app-knowledge-base`

## 近逐行中文伪代码

1. [L1-14] 导入并注入 global React
2. [L21-58] AST 属性读取辅助
3. [L60-89] 从 AppLayout 提取 Route
4. [L91-114] 解析重定向链
5. [L116-122] 侧栏用例
6. [L124-136] Tabs SSR 用例
7. [L138-142] 旧路径重定向用例

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/client-web/appKnowledgeNavigation.test.tsx",
      "label": "appKnowledgeNavigation.test",
      "path": "tests/client-web/appKnowledgeNavigation.test.tsx",
      "doc": "docs/pseudocode/files/tests/client-web/appKnowledgeNavigation.test.tsx.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    {
      "from": "tests/client-web/appKnowledgeNavigation.test.tsx",
      "to": "src/client-web/src/layout/Sidebar.tsx",
      "type": "tests"
    },
    {
      "from": "tests/client-web/appKnowledgeNavigation.test.tsx",
      "to": "src/client-web/src/components/app-knowledge/AppKnowledgeTabs.tsx",
      "type": "tests"
    },
    {
      "from": "tests/client-web/appKnowledgeNavigation.test.tsx",
      "to": "src/client-web/src/layout/AppLayout.tsx",
      "type": "tests"
    }
  ]
}
```
