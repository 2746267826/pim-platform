# tests/client-web/filesApiPath.test.ts

## 元信息
- 语言：TypeScript
- 程序集或包：tests/client-web
- 职责：断言 `fileApiPaths` 各路由拼接与查询编码正确。
- 主要依赖：`src/client-web/src/api/files` 的 `fileApiPaths`
- 被谁使用：Node 测试脚本

## 函数级结构化伪代码

### 路径断言集合
- providers / bindNextcloud / providerTest / providerSync
- items(path 编码) / item / upload / download / move / rename
- trash / trashRestore（trashId query 编码）
- versions / versionDownload / restore-preview / restore
- index / search(q+mode) / suggestions / dismiss / accept
- openLink(mode)

## 近逐行中文伪代码

1. 固定 UUID 样例 id。
2. 逐条 `assert.equal` 期望 REST 路径。
3. 验证 path/query 的 encodeURI 行为（如 `/Reports`、`trash/report.docx`、空格变 `+`）。

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/client-web/filesApiPath.test.ts",
      "label": "filesApiPath.test",
      "path": "tests/client-web/filesApiPath.test.ts",
      "doc": "docs/pseudocode/files/tests/client-web/filesApiPath.test.ts.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    {
      "from": "tests/client-web/filesApiPath.test.ts",
      "to": "src/client-web/src/api/files.ts",
      "type": "tests"
    }
  ]
}
```
