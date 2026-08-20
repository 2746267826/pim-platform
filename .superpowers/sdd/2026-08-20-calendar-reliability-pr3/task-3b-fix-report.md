# Task 3b Fix Report — Critical defects (PR3)

## 修复内容
- `CalendarService.DeleteEventAsync`：补齐 Outlook 绑定校验（02009）、master 解析后的二次校验、IgnoreQueryFilters 幂等、DeletedByOperationId/Kind、TimeProvider 统一、recurrenceId 归一化与校验、审计写入（CalendarAuditWriter）。
- `CalendarDeleteService`：注入 TimeProvider 替换 DateTimeOffset.UtcNow。
- `CalendarModule` PUT /events/{id}：合并 recurrenceId 时校验 ISO 可解析性、归一化到 O 格式、body/query 不一致抛 02009。
- `calendar.ts`：updateEvent/deleteEvent 支持 scope/recurrenceId 查询参数。
- `EventEditorDialog.tsx`：新增原生系列范围单选（此实例/整个系列）、合成 occurrence 通过 originalEventId/seriesMasterId + scope + recurrenceId 调用后端，修复合成 Id 导致的 02001。

## 验证
- `dotnet build Pim.sln --no-restore`：通过（0 error）。
- `dotnet test --filter Calendar`：747 passed。
- `npx tsc -b`：通过；vite build 因容器 Node 20.18 与 rolldown 原生绑定缺失失败，属环境问题非代码问题。

## 遗留
- 前端 vite 构建需 Node >=20.19，CI 环境满足即可；本地容器需升级 Node 或重装依赖。
