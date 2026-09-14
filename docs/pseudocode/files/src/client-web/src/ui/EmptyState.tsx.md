# src/client-web/src/ui/EmptyState.tsx

## 元信息
- 语言：TypeScript/TSX
- 程序集或包：client-web
- 职责：`EmptyState`：见源文件职责（EmptyState.tsx）。
- 主要依赖：无项目内相对导入（或仅外部包）
- 被谁使用：阅读时由总控/关系图汇总；本文件边中列出 depends_on

## 函数级结构化伪代码

### (file)
#### 模块顶层
- 输入：见导入与导出
- 输出：导出符号
- 副作用：见近逐行
- 步骤：
  1. 默认导出函数 `EmptyState`
  2. 执行：description,
  3. 执行：title: string;
  4. 执行：description?: string;
  5. 执行：action?: ReactNode;
  6. 返回 JSX/结构
  7. 执行：<div className="pim-card p-6 text-center">
  8. 执行：<p className="text-sm font-medium text-slate-700">{title}</p>
  9. 执行：{description && <p className="text-sm text-slate-500 mt-1">{description}</p>}
  10. 执行：{action && <div className="mt-4">{action}</div>}
- 分支与异常：无显著分支
- 调用：EmptyState

## 近逐行中文伪代码

1. [L3] 默认导出函数 `EmptyState`
2. [L5] 执行：description,
3. [L8] 执行：title: string;
4. [L9] 执行：description?: string;
5. [L10] 执行：action?: ReactNode;
6. [L12] 返回 JSX/结构
7. [L13] 执行：<div className="pim-card p-6 text-center">
8. [L14] 执行：<p className="text-sm font-medium text-slate-700">{title}</p>
9. [L15] 执行：{description && <p className="text-sm text-slate-500 mt-1">{description}</p>}
10. [L16] 执行：{action && <div className="mt-4">{action}</div>}

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-web/src/ui/EmptyState.tsx",
      "label": "EmptyState",
      "path": "src/client-web/src/ui/EmptyState.tsx",
      "doc": "docs/pseudocode/files/src/client-web/src/ui/EmptyState.tsx.md",
      "layer": "client-web",
      "kind": "ui"
    }
  ],
  "edges": []
}
```
