using System.Text;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Logging;
using Pim.Core.Exceptions;
using Pim.Core.Storage;
using Pim.Infrastructure.Data;
using Pim.Module.Files.Entities;
using Pim.Module.Files.Providers;

namespace Pim.Module.Files.Services;

/// <summary>
/// IOneDriveAttachmentStore 的文件模块实现（设计文档 §10）：
/// 附件按约定目录 /PIM/{objectKey} 存入用户 OneDrive（≤4MB 简单上传），
/// objectKey 持久化为 driveItem id；内容读取瞬态经过不落盘。
/// </summary>
public sealed class OneDriveAttachmentStore(
    PimDbContext db,
    IOneDriveGraphClient client,
    OneDriveTokenService tokens,
    ILogger<OneDriveAttachmentStore>? logger = null) : IOneDriveAttachmentStore
{
    public const string ProviderName = "onedrive";

    public async Task<string> StoreAsync(
        Guid userId,
        string objectKey,
        Stream content,
        string contentType,
        long sizeBytes,
        CancellationToken ct = default)
    {
        var provider = await LoadConnectedProviderAsync(userId, ct);
        var token = await tokens.GetAccessTokenAsync(provider.Id, ct);

        using var buffer = new MemoryStream();
        await content.CopyToAsync(buffer, ct);
        var bytes = buffer.ToArray();
        if (bytes.Length > OneDriveContentService.MaxSaveBytes)
        {
            throw new DomainException(5331, $"附件超过 {OneDriveContentService.MaxSaveBytes / 1024 / 1024}MB，请使用 OneDrive 客户端");
        }

        var drivePath = OneDriveAttachmentPath(objectKey);
        var driveItemId = await client.PutNewFileByPathAsync(token, drivePath, bytes, contentType, ct);
        logger?.LogInformation("OneDrive attachment stored: user {UserId}, key {ObjectKey}", userId, objectKey);
        return driveItemId;
    }

    public async Task<Stream> OpenReadAsync(Guid userId, string objectKey, CancellationToken ct = default)
    {
        var provider = await LoadConnectedProviderAsync(userId, ct);
        var token = await tokens.GetAccessTokenAsync(provider.Id, ct);
        var downloaded = await client.DownloadSmallAsync(token, objectKey, OneDriveContentService.MaxSaveBytes, ct)
            ?? throw new DomainException(5104, "附件不存在或已被删除");
        return new MemoryStream(downloaded.Bytes);
    }

    public async Task<string?> GetDirectLinkAsync(Guid userId, string objectKey, CancellationToken ct = default)
    {
        var provider = await LoadConnectedProviderAsync(userId, ct);
        var token = await tokens.GetAccessTokenAsync(provider.Id, ct);
        return await client.GetDownloadUrlAsync(token, objectKey, ct);
    }

    public async Task DeleteAsync(Guid userId, string objectKey, CancellationToken ct = default)
    {
        var provider = await LoadConnectedProviderAsync(userId, ct);
        var token = await tokens.GetAccessTokenAsync(provider.Id, ct);
        await client.DeleteItemAsync(token, objectKey, ct);
        logger?.LogInformation("OneDrive attachment deleted: user {UserId}, key {ObjectKey}", userId, objectKey);
    }

    /// <summary>objectKey（quick-notes/{userId}/{id}/{name}）映射为 OneDrive 路径 /PIM/{objectKey}。</summary>
    public static string OneDriveAttachmentPath(string objectKey)
        => "/PIM/" + objectKey.TrimStart('/');

    private async Task<FileProviderEntity> LoadConnectedProviderAsync(Guid userId, CancellationToken ct)
    {
        var provider = await db.Set<FileProviderEntity>()
            .SingleOrDefaultAsync(row => row.UserId == userId && row.Provider == "onedrive", ct)
            ?? throw new DomainException(5320, "尚未绑定 OneDrive，附件功能不可用");
        if (provider.Status != "connected" || provider.RefreshTokenEncrypted is not { Length: > 0 })
        {
            throw new DomainException(5321, "OneDrive 尚未完成绑定，附件功能不可用");
        }

        return provider;
    }
}
