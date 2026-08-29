# PASS-015 | AGENTS.md (剩余章节) | README.md (剩余章节) | 合格 | 规范与说明主体
- 验证方式：read_file AGENTS.md 120 行 + README.md 348 行 + grep `git worktree` `master` `origin/master` `dotnet test Pim.sln` `npm --prefix src/client-web run build` `http://127.0.0.1:5858`
- 验证点：AGENTS 的沟通规划、分支/PR、并行代理、工作实践、生产日志访问、Gates 除 DOC-004 外；README 的简介、技术栈 .NET 8、功能特性表格、架构总览模块划分、部署全家桶、反向代理要点、客户端构建命令、配置模板结构
- 代码实际：`Pim.sln` 存在，`src/Pim.Api/Pim.Api.csproj:25` `net8.0`，`src/client-web/package.json` `vite build`，`docker-compose.yml` 开发全家桶与 `docker-compose.prod.yml` 生产形态分离，`AGENTS` 所述 `git worktree` 约束未在本次只读审计中触发违规
- 结论：除 DOC-001~006 已单列的不一致外，两份入口文档的主体描述与代码仓库现状一致，标记为通过
