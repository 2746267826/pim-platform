# src/client-web/src/main.tsx

## 元信息
- 语言：TypeScript/TSX
- 程序集或包：client-web
- 职责：Web 应用入口：挂载 React 根节点与全局样式。
- 主要依赖：`src/client-web/src/App.tsx`
- 被谁使用：阅读时由总控/关系图汇总；本文件边中列出 depends_on

## 函数级结构化伪代码

### (file)
#### 模块顶层
- 输入：见导入与导出
- 输出：导出符号
- 副作用：见近逐行
- 步骤：
  1. 赋值 `queryClient` = new QueryClient()
  2. 执行：ReactDOM.createRoot(document.getElementById('root')!).render(
  3. 执行：<React.StrictMode>
  4. 执行：<QueryClientProvider client={queryClient}>
  5. 执行：<BrowserRouter>
  6. 执行：</BrowserRouter>
  7. 执行：</QueryClientProvider>
  8. 执行：</React.StrictMode>
- 分支与异常：无显著分支
- 调用：QueryClient、ReactDOM.createRoot、document.getElementById、render

## 近逐行中文伪代码

1. [L8] 赋值 `queryClient` = new QueryClient()
2. [L10] 执行：ReactDOM.createRoot(document.getElementById('root')!).render(
3. [L11] 执行：<React.StrictMode>
4. [L12] 执行：<QueryClientProvider client={queryClient}>
5. [L13] 执行：<BrowserRouter>
6. [L15] 执行：</BrowserRouter>
7. [L16] 执行：</QueryClientProvider>
8. [L17] 执行：</React.StrictMode>

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-web/src/main.tsx",
      "label": "main",
      "path": "src/client-web/src/main.tsx",
      "doc": "docs/pseudocode/files/src/client-web/src/main.tsx.md",
      "layer": "client-web",
      "kind": "entrypoint"
    }
  ],
  "edges": [
    {
      "from": "src/client-web/src/main.tsx",
      "to": "src/client-web/src/App.tsx",
      "type": "depends_on"
    }
  ]
}
```
