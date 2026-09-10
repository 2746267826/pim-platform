# src/Pim.Infrastructure/Storage/KopiaService.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Infrastructure
- 职责：封装 `kopia` CLI：连接仓库、创建/列表/恢复/删除快照；解析 JSON 快照列表为 `KopiaSnapshotInfo`。
- 主要依赖：`System.Diagnostics.Process`、`System.Text.Json`、环境变量 `KOPIA_PASSWORD`
- 被谁使用：`ServiceCollectionExtensions` Singleton 注册；备份/恢复相关调用方

## 函数级结构化伪代码

### KopiaService
#### 构造函数 KopiaService(string repositoryPath, string password)
- 输入：仓库路径、密码
- 输出：服务实例
- 副作用：无（密码存字段，后续写入子进程环境）
- 步骤：保存 `_repositoryPath`、`_password`
- 分支与异常：无
- 调用：无

#### Task\<string\> CreateSnapshotAsync(string sourcePath, string description, CancellationToken ct = default)
- 输入：源路径、描述、取消令牌
- 输出：kopia JSON 标准输出原文
- 副作用：执行 `kopia snapshot create` 写仓库
- 步骤：拼参数 `--description` + `--json`；`RunKopiaAsync`
- 分支与异常：进程失败见 `RunKopiaAsync`
- 调用：`RunKopiaAsync`

#### Task\<IReadOnlyList\<KopiaSnapshotInfo\>\> ListSnapshotsAsync(string sourcePath, CancellationToken ct = default)
- 输入：源路径；取消令牌
- 输出：快照信息列表
- 副作用：只读 CLI
- 步骤：`snapshot list --json` → `ParseSnapshotList`
- 分支与异常：JSON 非法由解析抛出
- 调用：`RunKopiaAsync`、`ParseSnapshotList`

#### Task\<Stream\> RestoreSnapshotAsync(string snapshotId, string targetPath, CancellationToken ct = default)
- 输入：快照 Id、目标路径；取消令牌
- 输出：打开 `targetPath` 的只读文件流
- 副作用：恢复文件到目标路径
- 步骤：`snapshot restore`；`File.OpenRead(targetPath)`
- 分支与异常：CLI 失败或文件不存在抛异常
- 调用：`RunKopiaAsync`、`File.OpenRead`

#### Task DeleteSnapshotAsync(string snapshotId, CancellationToken ct = default)
- 输入：快照 Id；取消令牌
- 输出：无
- 副作用：`snapshot delete --unsafe-ignore-source` 删除快照
- 步骤：`RunKopiaAsync`
- 分支与异常：CLI 非 0 退出抛 `InvalidOperationException`
- 调用：`RunKopiaAsync`

#### Task ConnectRepositoryAsync(CancellationToken ct = default)
- 输入：取消令牌
- 输出：无
- 副作用：`repository connect filesystem --path=...`
- 步骤：使用构造时的 `_repositoryPath`
- 分支与异常：连接失败抛异常
- 调用：`RunKopiaAsync`

#### private Task\<string\> RunKopiaAsync(string arguments, CancellationToken ct)
- 输入：CLI 参数串；取消令牌
- 输出：标准输出全文
- 副作用：启动 `kopia` 进程；设置 `KOPIA_PASSWORD`
- 步骤：
  1. `ProcessStartInfo`：FileName=kopia，重定向 stdout/stderr，无窗口
  2. 环境变量写入密码
  3. Start；null → `FileNotFoundException`
  4. 读 stdout/stderr；WaitForExit
  5. ExitCode!=0 → `InvalidOperationException` 含 stderr
  6. 返回 stdout
- 分支与异常：见上；取消令牌作用于 Read/Wait
- 调用：`Process.Start`

#### private IReadOnlyList\<KopiaSnapshotInfo\> ParseSnapshotList(string json)
- 输入：JSON 数组字符串
- 输出：`KopiaSnapshotInfo` 列表
- 副作用：无
- 步骤：
  1. 空白 → 空数组
  2. `JsonDocument.Parse` 根数组
  3. 每项取 id/description/startTime、stats.totalSize
  4. 构造 record 列表
- 分支与异常：缺属性或非数组 → Json 异常
- 调用：`JsonDocument`

### KopiaSnapshotInfo（record）
#### 位置参数构造
- 输入：Id、Description、StartTime、TotalSize
- 输出：不可变快照摘要
- 副作用：无
- 步骤：record 自动属性
- 分支与异常：无
- 调用：由 `ParseSnapshotList` 创建

## 近逐行中文伪代码

1. 引入 Diagnostics 与 JSON
2. 命名空间 `Pim.Infrastructure.Storage`
3. 类 `KopiaService`：字段仓库路径与密码
4. 构造赋值
5. CreateSnapshot：args=`snapshot create "path" --description=... --json`，返回输出
6. ListSnapshots：list --json，ParseSnapshotList
7. Restore：restore id target；OpenRead 返回流
8. Delete：delete id --unsafe-ignore-source
9. Connect：repository connect filesystem --path=repo
10. RunKopia：配置 ProcessStartInfo；设 KOPIA_PASSWORD
11. Start 失败抛 FileNotFoundException
12. 读输出与错误；WaitForExit；非 0 抛 InvalidOperationException
13. ParseSnapshotList：空串→空列表；解析数组；取 id/description/startTime/stats.totalSize
14. 定义 record `KopiaSnapshotInfo(Id, Description, StartTime, TotalSize)`

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/Pim.Infrastructure/Storage/KopiaService.cs",
      "label": "KopiaService",
      "path": "src/Pim.Infrastructure/Storage/KopiaService.cs",
      "doc": "docs/pseudocode/files/src/Pim.Infrastructure/Storage/KopiaService.cs.md",
      "layer": "infrastructure",
      "kind": "service"
    }
  ],
  "edges": [
    { "from": "src/Pim.Infrastructure/Storage/KopiaService.cs", "to": "System.Diagnostics.Process", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Extensions/ServiceCollectionExtensions.cs", "to": "src/Pim.Infrastructure/Storage/KopiaService.cs", "type": "depends_on" }
  ]
}
```
