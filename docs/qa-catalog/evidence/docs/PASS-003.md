# PASS-003 | docs/operations/backup-restore.md | 合格 | 备份与恢复清单
- 验证方式：read_file + grep `Kopia__RepositoryPath` `DataProtection__KeysPath` `Jwt__PrivateKeyPath`  + ls `sql/` 与 `volumes`
- 验证点：文档列出备份项 PostgreSQL pim、MinIO、/data 卷、keys/jwt_private.pem、.env、%LOCALAPPDATA%\PIM\config.json；未自动备份项 bin/obj/dist/wwwroot
- 代码实际：`docker-compose.prod.yml:42-45` 挂载 `pim_data:/data`、`/data/pim/logs`、`/data/keys`；`src/Pim.Api/appsettings.json:7-9` 配置 `Jwt:PrivateKeyPath /data/keys/jwt_private.pem` 与 `DataProtection:KeysPath`；`src/client-windows/Pim.Client.Core/Services/AuthService.cs:101` 写入 `%LOCALAPPDATA%/PIM/token.json`；`.gitignore` 排除 `bin/obj/build/dist/publish`
- 结论：备份范围与实际持久化路径一致，未发现承诺的备份项在代码中缺失，标记为通过
