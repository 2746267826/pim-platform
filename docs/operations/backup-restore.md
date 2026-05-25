# PIM Backup And Restore

## What To Back Up

- PostgreSQL database `pim`.
- MinIO data volume.
- API `/data` volume, including logs when needed.
- JWT private key files under `keys/` or `/data/keys`.
- Local deployment `.env` values.
- Windows daemon config at `%LOCALAPPDATA%\PIM\config.json`.

## What Is Not Backed Up Automatically

- Generated `bin/`, `obj/`, `build/`, `dist/`, and API `wwwroot` build artifacts.
- npm caches and temporary `.dotnet-*` directories.
- Local logs unless the operator explicitly copies them.

## Manual Restore Verification

1. Restore PostgreSQL and MinIO data.
2. Restore keys and environment values.
3. Start the API at `http://127.0.0.1:5858`.
4. Open Web and confirm login works.
5. Open `状态信息` and confirm API and database are healthy.
6. Start the Windows daemon and confirm its heartbeat appears.
7. Run `dotnet test Pim.sln`.
8. Run `npm --prefix src/client-web run build`.
