# src/client-web/eslint.config.js

## 元信息
- 语言：JavaScript (ESM)
- 程序集或包：client-web
- 职责：client-web 的 flat ESLint 配置：忽略构建产物；合并 JS/TS recommended；对 `**/*.{ts,tsx}` 启用 browser/es2022 全局、react-hooks 与 react-refresh 规则。
- 主要依赖：`@eslint/js`、`eslint-plugin-react-hooks`、`eslint-plugin-react-refresh`、`globals`、`typescript-eslint`
- 被谁使用：ESLint CLI / IDE / CI 在 `src/client-web` 目录运行

## 函数级结构化伪代码

### default export tseslint.config(...)
#### 配置块 1：ignores
- 输入：无
- 输出：忽略 `dist`、`../Pim.Api/wwwroot`、`node_modules`
- 副作用：这些路径不参与 lint
- 步骤：声明 ignores 数组
- 分支与异常：无
- 调用：无

#### 配置块 2–3：js + tseslint recommended
- 输入：无
- 输出：基础 JS 与 TS recommended 规则集
- 副作用：无
- 步骤：展开 `js.configs.recommended` 与 `...tseslint.configs.recommended`
- 分支与异常：无
- 调用：`tseslint.config`

#### 配置块 4：TS/TSX 专用
- 输入：匹配 `**/*.{ts,tsx}` 的文件
- 输出：languageOptions + plugins + rules
- 副作用：无
- 步骤：
  1. `ecmaVersion: 2022`；globals = browser ∪ es2022
  2. 插件 `react-hooks`、`react-refresh`
  3. 规则：react-hooks recommended；`react-refresh/only-export-components` 为 warn 且 `allowConstantExport: true`
- 分支与异常：无
- 调用：插件配置对象

## 近逐行中文伪代码

1. import js、reactHooks、reactRefresh、globals、tseslint
2. `export default tseslint.config(` 多个配置对象
3. 忽略 dist、API wwwroot、node_modules
4. 应用 JS recommended
5. 展开 TS recommended
6. 仅 ts/tsx：ECMA 2022 + browser/es2022 全局
7. 注册 react-hooks 与 react-refresh 插件
8. 启用 hooks recommended；only-export-components 警告并允许常量导出
9. 结束配置

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-web/eslint.config.js",
      "label": "eslint.config",
      "path": "src/client-web/eslint.config.js",
      "doc": "docs/pseudocode/files/src/client-web/eslint.config.js.md",
      "layer": "client-web",
      "kind": "other"
    }
  ],
  "edges": [
    { "from": "src/client-web/eslint.config.js", "to": "typescript-eslint", "type": "depends_on" },
    { "from": "src/client-web/eslint.config.js", "to": "eslint-plugin-react-hooks", "type": "depends_on" },
    { "from": "src/client-web/eslint.config.js", "to": "eslint-plugin-react-refresh", "type": "depends_on" }
  ]
}
```
