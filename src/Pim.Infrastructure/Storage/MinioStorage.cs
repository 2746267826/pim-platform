using Minio;
using Minio.DataModel.Args;

namespace Pim.Infrastructure.Storage;

public class MinioStorage
{
    private readonly IMinioClient _client;
    private const string BucketName = "pim-files";

    public MinioStorage(string endpoint, string accessKey, string secretKey)
    {
        _client = new MinioClient()
            .WithEndpoint(endpoint)
            .WithCredentials(accessKey, secretKey)
            .Build();
    }

    public async Task EnsureBucketAsync(CancellationToken ct = default)
    {
        var exists = await _client.BucketExistsAsync(
            new BucketExistsArgs().WithBucket(BucketName), ct);
        if (!exists)
        {
            await _client.MakeBucketAsync(
                new MakeBucketArgs().WithBucket(BucketName), ct);
        }
    }

    public async Task<string> UploadAsync(
        string objectName, Stream data, string contentType, long size,
        CancellationToken ct = default)
    {
        await _client.PutObjectAsync(new PutObjectArgs()
            .WithBucket(BucketName)
            .WithObject(objectName)
            .WithStreamData(data)
            .WithObjectSize(size)
            .WithContentType(contentType), ct);

        return objectName;
    }

    public async Task<Stream> DownloadAsync(string objectName, CancellationToken ct = default)
    {
        var stream = new MemoryStream();
        await _client.GetObjectAsync(new GetObjectArgs()
            .WithBucket(BucketName)
            .WithObject(objectName)
            .WithCallbackStream(s => s.CopyTo(stream)), ct);
        stream.Position = 0;
        return stream;
    }

    public async Task<string> GetPresignedUrlAsync(
        string objectName, int expirySeconds = 300, CancellationToken ct = default)
    {
        return await _client.PresignedGetObjectAsync(new PresignedGetObjectArgs()
            .WithBucket(BucketName)
            .WithObject(objectName)
            .WithExpiry(expirySeconds));
    }

    public async Task DeleteAsync(string objectName, CancellationToken ct = default)
    {
        await _client.RemoveObjectAsync(new RemoveObjectArgs()
            .WithBucket(BucketName)
            .WithObject(objectName), ct);
    }
}
