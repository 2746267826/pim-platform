# src/client-web/src/pages/SyncPage.tsx

## 元信息
- 语言：TypeScript/React
- 程序集或包：client-web
- 职责：Outlook/微软同步设置、设备代码登录、运行同步、冲突列表、批次时间线。
- 主要依赖：calendar API、OutlookConflictResolver、PageHeader
- 被谁使用：路由同步页；导出 `outlookSyncInvalidationKeys`

## 函数级结构化伪代码

### formatDateTime / mutationError
- 时间格式与错误消息

### outlookSyncInvalidationKeys
- 同步成功后需失效的 query key 常量数组

### SyncPage
- 状态：tenantId/clientId/scopes，由 settings 回填
- 查询：outlook-settings、sync-batches(45s)、data-center sync-conflict
- mutations：更新设置、创建设备码、轮询设备码、runOutlookSync
- UI：设置表单、设备代码区、健康指标四卡、ConflictResolver、批次 steps

## 近逐行中文伪代码

1. 加载设置与批次；查询 outlook 冲突。
2. settings 到位后同步表单字段。
3. 保存设置、请求设备码、完成连接、运行同步。
4. 同步成功批量 invalidate 工作台/今日/确认等 keys。
5. 展示 token/delta/writeback/conflict 策略与批次计数。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-web/src/pages/SyncPage.tsx",
      "label": "SyncPage",
      "path": "src/client-web/src/pages/SyncPage.tsx",
      "doc": "docs/pseudocode/files/src/client-web/src/pages/SyncPage.tsx.md",
      "layer": "client-web",
      "kind": "ui"
    }
  ],
  "edges": [
    { "from": "src/client-web/src/pages/SyncPage.tsx", "to": "src/client-web/src/api/calendar.ts", "type": "depends_on" },
    { "from": "src/client-web/src/pages/SyncPage.tsx", "to": "src/client-web/src/components/schedule/OutlookConflictResolver.tsx", "type": "depends_on" },
    { "from": "src/client-web/src/pages/SyncPage.tsx", "to": "src/client-web/src/ui/PageHeader.tsx", "type": "depends_on" },
    { "from": "src/client-web/src/pages/SyncPage.tsx", "to": "src/client-web/src/types/index.ts", "type": "depends_on" }
  ]
}
```
