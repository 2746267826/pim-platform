# Microsoft 日历同步验收记录

- 日期：2026-07-14
- 分支：`codex/microsoft-calendar-sync`
- 提交：`1bcfa0b3`
- 账号类型：待填写
- 结果：待验收

> 只记录 PASS/FAIL 和必要的错误摘要。不要记录 token、device code、user code、MSAL cache、Authorization header 或日程正文。

## 自动化前置检查

- [x] `dotnet test Pim.sln`：1092/1092 通过
- [x] `npm --prefix src/client-web run test:schedule-workbench`：通过
- [x] `npm --prefix src/client-web run build`：通过
- [ ] `npm --prefix src/client-web run lint`：仓库既有 18 errors / 22 warnings；Microsoft 日历改动文件为 0 error / 2 warnings
- [x] PR #13：API、Web CI 通过；Android、Windows 因路径过滤跳过

## 真实账号验收

- [ ] 仅按页面引导完成 Entra 注册和 Device Code 授权
- [ ] 发现默认、分组、课程表和未分组日历
- [ ] 普通同步、full-resources、range-instances 与手动强制获取全部日程
- [ ] UTC+8 展示、全天边界、重复实例/系列
- [ ] Outlook -> PIM 新增、修改、移动、删除自动应用
- [ ] PIM -> Outlook 新建、修改、删除均经过二次确认
- [ ] ETag 412 停止覆盖并展示最新远端
- [ ] token 静默续期、取消、部分失败和失败日历人工重试
- [ ] 永久历史可查，断开保留数据，本地清理不影响 Outlook

## 失败记录

无。真实账号验收尚未开始。
