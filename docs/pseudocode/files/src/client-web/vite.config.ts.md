# src/client-web/vite.config.ts

## 元信息
- 语言：TypeScript
- 程序集或包：client-web
- 职责：Vite 构建配置：开发代理、别名与构建选项。
- 主要依赖：无项目内相对导入（或仅外部包）
- 被谁使用：阅读时由总控/关系图汇总；本文件边中列出 depends_on

## 函数级结构化伪代码

### (file)
#### 模块顶层
- 输入：见导入与导出
- 输出：导出符号
- 副作用：见近逐行
- 步骤：
  1. 执行：export default defineConfig({
  2. 执行：plugins: [react(), tailwindcss()],
  3. 执行：resolve: {
  4. 执行：alias: { '@': path.resolve(__dirname, './src') }
  5. 执行：server: {
  6. 执行：port: 5173,
  7. 执行：'/api': { target: 'http://localhost:5858', changeOrigin: true }
  8. 执行：outDir: '../Pim.Api/wwwroot',
  9. 执行：emptyOutDir: true
  10. 执行：define: {
  11. 执行：__APP_VERSION__: JSON.stringify(process.env.VITE_APP_VERSION || '0.0.0(dev)')
- 分支与异常：无显著分支
- 调用：defineConfig、react、tailwindcss、path.resolve、JSON.stringify

## 近逐行中文伪代码

1. [L6] 执行：export default defineConfig({
2. [L7] 执行：plugins: [react(), tailwindcss()],
3. [L8] 执行：resolve: {
4. [L9] 执行：alias: { '@': path.resolve(__dirname, './src') }
5. [L11] 执行：server: {
6. [L12] 执行：port: 5173,
7. [L14] 执行：'/api': { target: 'http://localhost:5858', changeOrigin: true }
8. [L18] 执行：outDir: '../Pim.Api/wwwroot',
9. [L19] 执行：emptyOutDir: true
10. [L21] 执行：define: {
11. [L22] 执行：__APP_VERSION__: JSON.stringify(process.env.VITE_APP_VERSION || '0.0.0(dev)')

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-web/vite.config.ts",
      "label": "defineConfig",
      "path": "src/client-web/vite.config.ts",
      "doc": "docs/pseudocode/files/src/client-web/vite.config.ts.md",
      "layer": "client-web",
      "kind": "other"
    }
  ],
  "edges": []
}
```
