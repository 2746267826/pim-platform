# src/client-web/src/api/appSignatures.ts

## 元信息
- 语言：TypeScript
- 程序集或包：client-web
- 职责：PC 应用签名（app signatures）REST 客户端：列表/计数/lookup/创建/删除。
- 主要依赖：`./client` 的 `apiGet`/`apiPost`/`apiDelete`；`ApiResponse`
- 被谁使用：PC 分类 / 应用知识相关页面

## 函数级结构化伪代码

### 类型 AppSignature / SaveAppSignatureRequest
- 输入/输出：DTO 字段（id、processName、displayName、categoryPath、productivity 等）
- 副作用：无
- 步骤：类型声明
- 分支与异常：无
- 调用：无

### getAppSignatures(search?: string)
- 输入：可选搜索串
- 输出：`Promise<AppSignature[]>`（`r.data`）
- 副作用：GET `/pc/app-signatures/` 或带 `?search=`
- 步骤：有 search 则 encode 拼 query；`apiGet` 后取 data
- 分支与异常：透传 HTTP
- 调用：`apiGet`

### getAppSignatureCount()
- 输入：无
- 输出：`Promise<number>`
- 副作用：GET `.../count`
- 步骤：`apiGet` → data
- 分支与异常：透传
- 调用：`apiGet`

### lookupAppSignature(processName: string)
- 输入：进程名
- 输出：`Promise<AppSignature>`
- 副作用：GET `.../lookup/{processName}`
- 步骤：URI encode 路径段；`apiGet` → data
- 分支与异常：透传
- 调用：`apiGet`

### createAppSignature(data: SaveAppSignatureRequest)
- 输入：保存请求体
- 输出：`Promise<AppSignature>`
- 副作用：POST `.../`
- 步骤：`apiPost` → data
- 分支与异常：透传
- 调用：`apiPost`

### deleteAppSignature(id: string)
- 输入：签名 id
- 输出：`Promise<string>`
- 副作用：DELETE `.../{id}`
- 步骤：`apiDelete` → data
- 分支与异常：透传
- 调用：`apiDelete`

## 近逐行中文伪代码

1. 定义 `AppSignature` 与 `SaveAppSignatureRequest` 接口。
2. `basePath = /pc/app-signatures`。
3. 列表：可选 search query；count/lookup/create/delete 对应 REST 动词。
4. 统一 `then(r => r.data)` 解包 `ApiResponse`。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-web/src/api/appSignatures.ts",
      "label": "appSignatures",
      "path": "src/client-web/src/api/appSignatures.ts",
      "doc": "docs/pseudocode/files/src/client-web/src/api/appSignatures.ts.md",
      "layer": "client-web",
      "kind": "other"
    }
  ],
  "edges": [
    { "from": "src/client-web/src/api/appSignatures.ts", "to": "src/client-web/src/api/client.ts", "type": "depends_on" },
    { "from": "src/client-web/src/api/appSignatures.ts", "to": "src/client-web/src/api/client.ts", "type": "calls" },
    { "from": "src/client-web/src/api/appSignatures.ts", "to": "/pc/app-signatures", "type": "http" }
  ]
}
```
