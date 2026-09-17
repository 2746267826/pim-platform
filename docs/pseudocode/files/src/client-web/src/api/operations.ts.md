# src/client-web/src/api/operations.ts

## 元信息
- 语言：TypeScript
- 程序集或包：client-web
- 职责：运维确认单与审计时间线/恢复预览/导出的 Web API 路径与请求封装。
- 主要依赖：`./client` 的 `apiGet`/`apiPost`；`../types` 中确认与审计类型
- 被谁使用：运维/确认相关页面与组件

## 函数级结构化伪代码

### operationsApiPaths
#### pendingConfirmations / detail / confirm / confirmSecondLevel / confirmStrict / reject / auditTimeline / restorePreview / auditExport
- 输入：id、objectType/objectId、auditVersionId（按方法）
- 输出：相对 API 路径字符串（id 经 encodeURIComponent）
- 副作用：无
- 步骤：拼接 `/operations/confirmations...` 或 `/operations/audit...`
- 分支与异常：无
- 调用：`encodeURIComponent`

### 异步 API 函数
#### getPendingConfirmations()
- 输入：无
- 输出：`OperationConfirmation[]`（`r.data`）
- 副作用：GET pending
- 步骤：`apiGet` → 返回 data
- 分支与异常：委托 client
- 调用：`apiGet`、`operationsApiPaths.pendingConfirmations`

#### getConfirmationDetail(id)
- 输入：确认单 id
- 输出：`OperationConfirmation`
- 副作用：GET detail
- 步骤：同上
- 分支与异常：委托 client
- 调用：`apiGet`

#### confirmOperation / confirmOperationSecondLevel / confirmOperationStrict / rejectOperation(id)
- 输入：确认单 id
- 输出：更新后的 `OperationConfirmation`
- 副作用：POST 空 body 到对应 confirm/reject 路径
- 步骤：`apiPost(path, {})` → data
- 分支与异常：委托 client
- 调用：`apiPost`

#### getAuditTimeline(objectType, objectId)
- 输入：对象类型与 Id
- 输出：`AuditTimelineResponse`
- 副作用：GET 审计时间线
- 步骤：apiGet → data
- 分支与异常：委托 client
- 调用：`apiGet`

#### getRestorePreview(auditVersionId)
- 输入：审计版本 Id
- 输出：`RestorePreviewResponse`
- 副作用：POST 空 body 预览恢复
- 步骤：apiPost → data
- 分支与异常：委托 client
- 调用：`apiPost`

#### exportAudit()
- 输入：无
- 输出：`AuditExportResponse`
- 副作用：GET 导出
- 步骤：apiGet → data
- 分支与异常：委托 client
- 调用：`apiGet`

## 近逐行中文伪代码

1. 从 client 引入 apiGet/apiPost；从 types 引入响应类型
2. 导出 `operationsApiPaths` 对象：pending/detail/三级 confirm/reject/auditTimeline/restorePreview/export 路径工厂
3. getPendingConfirmations：GET pending，返 data
4. getConfirmationDetail：GET detail
5. confirm* / reject：POST 空对象，返 data
6. getAuditTimeline：GET 按对象类型/Id
7. getRestorePreview：POST 预览
8. exportAudit：GET 导出

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-web/src/api/operations.ts",
      "label": "operations",
      "path": "src/client-web/src/api/operations.ts",
      "doc": "docs/pseudocode/files/src/client-web/src/api/operations.ts.md",
      "layer": "client-web",
      "kind": "other"
    }
  ],
  "edges": [
    { "from": "src/client-web/src/api/operations.ts", "to": "src/client-web/src/api/client.ts", "type": "depends_on" },
    { "from": "src/client-web/src/api/operations.ts", "to": "src/client-web/src/types", "type": "depends_on" },
    { "from": "src/client-web/src/api/operations.ts", "to": "src/Pim.Api/Endpoints/OperationsEndpoints.cs", "type": "http" }
  ]
}
```
