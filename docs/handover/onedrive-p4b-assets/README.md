# P4b 参考实现（未接线，仅供参考）

这三个文件是 P4b 开发中途产出的**可复用资产**，从未编译进任何项目（它们不在任何
`.csproj` 的编译范围内），保存于此是为了让接手人不必从头设计附件存储抽象。

| 文件 | 目标路径 | 内容 |
|---|---|---|
| `IOneDriveAttachmentStore.cs` | `src/Pim.Core/Storage/` | 用户级附件存储抽象（`Guid userId` 参数化） |
| `OneDriveAttachmentStore.cs` | `src/modules/Pim.Module.Files/Services/` | 文件模块实现：附件存 `/PIM/{objectKey}`，objectKey = driveItem id |
| `OneDriveQuickNoteObjectStorage.cs` | `src/modules/Pim.Module.QuickNotes/Services/` | QuickNotes 适配（含 `IQuickNoteDirectLinkStorage` 直链能力） |

**使用方式**：搬入目标路径 → 补齐 QuickNotes DI 接线与下载端点 302 分支 →
按交接文档 §4.5 继续。**注意**：这些文件未经编译验证（当时的破坏点在
`FileOperationService` 的退役裁剪，不在这些文件本身），搬运后需自行编译与补测试。

详见 [`../2026-09-19-onedrive-files-handover.md`](../2026-09-19-onedrive-files-handover.md) §4。
