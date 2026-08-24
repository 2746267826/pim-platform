# PASS-008 | docs/operations/windows-keystats-session-fix.md | 合格 | Session 0 零值修复方案
- 验证方式：read_file + grep `KeyStatsProcessManager` `KeyStatsHealthProbe` `SessionId` `fix-keystats-session.ps1`
- 验证点：文档描述根因 Session 0 僵尸进程占 18080 导致计数为 0，修复为 `SessionId==0 || !UserInteractive` 拒启、`PimKeyStats` 任务 `/rl limited`、daemon 收敛至单 Session 1 进程、健康探针分级 stale-zero/missing/unreachable、上报真实 AW/KeyStats 状态、状态中心一键修复
- 代码实际：`src/client-windows/Pim.Client.Core/Services/KeyStatsProcessManager.cs` 实现会话收敛；`KeyStatsHealthProbe.cs` 分类探针；`src/client-windows/Pim.Client.App/StatusWindow.xaml.cs:86` 探测 `127.0.0.1:5600`；`fix-keystats-session.ps1` 位于安装目录仅杀进程不提权
- 结论：文档所述的根因、提交与客户端修复逻辑与代码实现一致，标记为通过
