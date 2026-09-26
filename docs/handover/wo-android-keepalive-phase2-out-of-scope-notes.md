# 阶段二范围外发现记录

本文件只记录**本次未修**的范围外问题（工单要求：发现范围外问题只记录证据并单独提出）。

## 1. `pcHeatmapCharts.test.tsx` 依赖运行环境时区（预存在，非本 PR 引入）

| 项 | 内容 |
|---|---|
| 文件 | `tests/client-web/pcHeatmapCharts.test.tsx`（与 `origin/master` **逐字节一致**，本 PR 未改动） |
| 现象 | 在非 UTC 时区下断言失败：<br>`AssertionError: single Monday-anchored week row`<br>`actual: ['2026-08-03','2026-08-10']` / `expected: ['2026-08-10']` |
| 复现 | `cd src/client-web && TZ=Asia/Shanghai npx tsx ../../tests/client-web/pcHeatmapCharts.test.tsx` → 失败<br>`TZ=UTC npx tsx ../../tests/client-web/pcHeatmapCharts.test.tsx` → 通过 |
| 根因 | 该用例把 `bucket('2026-08-10T04:00:00', …)` 这类**不带时区偏移**的时间字面量交给 `buildActivityHeatmapOption`；在 UTC 下 04:00 仍属 08-10，在 UTC+8 下同一时刻被解析成 08-09 所在的那一周，于是多出一行周。即用例隐含假设"运行环境是 UTC"。 |
| 影响 | **不影响 CI**：GitHub runner 默认即 UTC，`build-web` 作业未设置其它时区，因此线上始终通过。仅在本地用非 UTC 时区直接跑该文件时会失败。 |
| 判定 | 与本 PR 范围（阶段二保活）无关；属测试自身的环境假设问题。按工单「范围外一律不改」，**本次不修**，仅在此记录并单独提出。 |
| 建议 | 单独处理：给该用例显式设定 `TZ=UTC`（例如在测试入口设置），或把时间字面量写成带偏移的 ISO 字符串，使断言不依赖宿主时区。 |
