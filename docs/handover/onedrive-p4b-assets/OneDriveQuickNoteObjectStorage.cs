using Pim.Core.Exceptions;
using Pim.Core.Storage;

namespace Pim.Module.QuickNotes.Services;

/// <summary>
/// 支持直链的附件存储可选能力：实现者可把内容端点 302 到预授权直链，
/// 避免服务器代理流量（设计文档 §10）。
/// </summary>
public interface IQuickNoteDirectLinkStorage
{
    Task<string?> GetDirectLinkAsync(string objectKey, CancellationToken ct = default);
}

/// <summary>
/// 附件存 OneDrive 的 QuickNotes 存储实现：objectKey 即 driveItem id。
/// 依赖文件模块的 IOneDriveAttachmentStore（未绑定 OneDrive 时调用抛出明确领域错误）。
/// </summary>
public sealed class OneDriveQuickNoteObjectStorage(IOneDriveAttachmentStore store) : IQuickNoteObjectStorage, IQuickNoteDirectLinkStorage
{
    public const string ProviderName = "onedrive";

    public async Task<string> StoreAsync(
        string objectKey,
        Stream content,
        string contentType,
        long sizeBytes,
        CancellationToken ct = default)
    {
        var userId = ExtractUserId(objectKey)
            ?? throw new DomainException(5308, "附件 objectKey 缺少用户标识");
        return await store.StoreAsync(userId, objectKey, content, contentType, sizeBytes, ct);
    }

    public async Task<Stream> OpenReadAsync(string objectKey, CancellationToken ct = default)
    {
        var userId = ExtractUserId(objectKey)
            ?? throw new DomainException(5308, "附件 objectKey 缺少用户标识");
        return await store.OpenReadAsync(userId, objectKey, ct);
    }

    public async Task<string?> GetDirectLinkAsync(string objectKey, CancellationToken ct = default)
    {
        var userId = ExtractUserId(objectKey)
            ?? throw new DomainException(5308, "附件 objectKey 缺少用户标识");
        return await store.GetDirectLinkAsync(userId, objectKey, ct);
    }

    public async Task DeleteAsync(string objectKey, CancellationToken ct = default)
    {
        var userId = ExtractUserId(objectKey)
            ?? throw new DomainException(5308, "附件 objectKey 缺少用户标识");
        await store.DeleteAsync(userId, objectKey, ct);
    }

    /// <summary>objectKey 约定：quick-notes/{userId:N}/{attachmentId:N}/{fileName}。</summary>
    private static Guid? ExtractUserId(string objectKey)
    {
        var segments = objectKey.Split('/');
        return segments.Length >= 2 && Guid.TryParse(segments[1], out var userId) ? userId : null;
    }
}
