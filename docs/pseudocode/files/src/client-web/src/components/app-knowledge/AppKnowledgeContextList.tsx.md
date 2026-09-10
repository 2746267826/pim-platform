# src/client-web/src/components/app-knowledge/AppKnowledgeContextList.tsx

## 元信息
- 语言：TypeScript/TSX
- 程序集或包：client-web
- 职责：展示某 App 的知识上下文模式列表（加载中/空态/条目卡片），支持删除。
- 主要依赖：`appKnowledge` 类型、`AppKnowledgeImpactSummary`
- 被谁使用：App 知识管理页面

## 函数级结构化伪代码

### 模块级
#### patternLabels / getSourceLabel(source) / renderTarget(context)
- 输入：模式类型、来源字符串、上下文对象
- 输出：中文标签、来源文案、目标类别/项目标签 JSX
- 副作用：无
- 步骤：
  1. 模式映射 app-default/domain/title/url-path/source-family
  2. source：builtin/learned/custom/system/manual → 中文，否则原样
  3. 无目标类别且无项目标签 → 「未分配目标」；否则渲染徽章
- 调用：无

### AppKnowledgeContextList
#### default function AppKnowledgeContextList({ contexts, isLoading, onDelete })
- 输入：上下文数组、加载标志、删除回调
- 输出：列表 UI
- 副作用：按钮点击调用 `onDelete`
- 步骤：
  1. isLoading → 虚线边框加载提示
  2. 空数组 → 暂无模式提示
  3. 否则 map 每条 article：模式徽章、停用标记、patternValue/scopeSummary、进程名·来源、删除按钮、目标、`AppKnowledgeImpactSummary`
- 分支与异常：无
- 调用：`renderTarget`、`getSourceLabel`、`AppKnowledgeImpactSummary`

## 近逐行中文伪代码

1. 导入上下文类型与影响摘要组件
2. Props：contexts、isLoading、onDelete
3. patternLabels 中文映射五种模式
4. getSourceLabel：内置/学习/自定义/系统/手动
5. renderTarget：无目标显示灰字；否则类别绿徽章 + 项目 indigo 徽章
6. 组件：加载中/空态 early return
7. 有数据：space-y 列表
8. 每条：左信息（标签、模式、停用、值、进程·来源）+ 删除按钮
9. 下方目标与影响摘要（记录数/时长）

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-web/src/components/app-knowledge/AppKnowledgeContextList.tsx",
      "label": "AppKnowledgeContextList",
      "path": "src/client-web/src/components/app-knowledge/AppKnowledgeContextList.tsx",
      "doc": "docs/pseudocode/files/src/client-web/src/components/app-knowledge/AppKnowledgeContextList.tsx.md",
      "layer": "client-web",
      "kind": "ui"
    }
  ],
  "edges": [
    { "from": "src/client-web/src/components/app-knowledge/AppKnowledgeContextList.tsx", "to": "src/client-web/src/api/appKnowledge.ts", "type": "depends_on" },
    { "from": "src/client-web/src/components/app-knowledge/AppKnowledgeContextList.tsx", "to": "src/client-web/src/components/app-knowledge/AppKnowledgeImpactSummary.tsx", "type": "calls" }
  ]
}
```
