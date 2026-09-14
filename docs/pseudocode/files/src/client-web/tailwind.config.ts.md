# src/client-web/tailwind.config.ts

## 元信息
- 语言：TypeScript
- 程序集或包：client-web
- 职责：Tailwind CSS 配置入口。
- 主要依赖：无项目内相对导入（或仅外部包）
- 被谁使用：阅读时由总控/关系图汇总；本文件边中列出 depends_on

## 函数级结构化伪代码

### (file)
#### 模块顶层
- 输入：见导入与导出
- 输出：导出符号
- 副作用：见近逐行
- 步骤：
  1. 执行：export default {
  2. 执行：content: ['./index.html', './src/**/*.{ts,tsx}'],
  3. 执行：theme: { extend: {} },
  4. 执行：plugins: []
- 分支与异常：无显著分支
- 调用：无

## 近逐行中文伪代码

1. [L2] 执行：export default {
2. [L3] 执行：content: ['./index.html', './src/**/*.{ts,tsx}'],
3. [L4] 执行：theme: { extend: {} },
4. [L5] 执行：plugins: []

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-web/tailwind.config.ts",
      "label": "tailwind.config",
      "path": "src/client-web/tailwind.config.ts",
      "doc": "docs/pseudocode/files/src/client-web/tailwind.config.ts.md",
      "layer": "client-web",
      "kind": "other"
    }
  ],
  "edges": []
}
```
