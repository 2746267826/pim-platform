# PASS-083 | /tasks | PASS | 任务日历获取正常（修正）
- 描述：经二次验证，使用正确路径 /api/v1/calendar/calendars?kind=task 可正常创建/查询任务本，前次WEB-009为脚本路径错误误报，已撤销。
- 证据：直接API 201 创建 TaskBook2-xxx 成功，见 /tmp/db-write2.log
