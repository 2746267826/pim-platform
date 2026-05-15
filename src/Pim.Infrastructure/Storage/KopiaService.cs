using System.Diagnostics;
using System.Text.Json;

namespace Pim.Infrastructure.Storage;

public class KopiaService
{
    private readonly string _repositoryPath;
    private readonly string _password;

    public KopiaService(string repositoryPath, string password)
    {
        _repositoryPath = repositoryPath;
        _password = password;
    }

    public async Task<string> CreateSnapshotAsync(
        string sourcePath, string description, CancellationToken ct = default)
    {
        var args = $"snapshot create \"{sourcePath}\" --description=\"{description}\" --json";
        var output = await RunKopiaAsync(args, ct);
        return output;
    }

    public async Task<IReadOnlyList<KopiaSnapshotInfo>> ListSnapshotsAsync(
        string sourcePath, CancellationToken ct = default)
    {
        var args = $"snapshot list \"{sourcePath}\" --json";
        var output = await RunKopiaAsync(args, ct);
        return ParseSnapshotList(output);
    }

    public async Task<Stream> RestoreSnapshotAsync(
        string snapshotId, string targetPath, CancellationToken ct = default)
    {
        var args = $"snapshot restore {snapshotId} \"{targetPath}\"";
        await RunKopiaAsync(args, ct);
        return File.OpenRead(targetPath);
    }

    public async Task DeleteSnapshotAsync(
        string snapshotId, CancellationToken ct = default)
    {
        var args = $"snapshot delete {snapshotId} --unsafe-ignore-source";
        await RunKopiaAsync(args, ct);
    }

    public async Task ConnectRepositoryAsync(CancellationToken ct = default)
    {
        var args = $"repository connect filesystem --path=\"{_repositoryPath}\"";
        await RunKopiaAsync(args, ct);
    }

    private async Task<string> RunKopiaAsync(string arguments, CancellationToken ct)
    {
        var psi = new ProcessStartInfo
        {
            FileName = "kopia",
            Arguments = arguments,
            RedirectStandardOutput = true,
            RedirectStandardError = true,
            UseShellExecute = false,
            CreateNoWindow = true
        };

        psi.EnvironmentVariables["KOPIA_PASSWORD"] = _password;

        using var process = Process.Start(psi);
        if (process == null)
            throw new FileNotFoundException("kopia executable not found on PATH");
        var output = await process.StandardOutput.ReadToEndAsync(ct);
        var error = await process.StandardError.ReadToEndAsync(ct);
        await process.WaitForExitAsync(ct);

        if (process.ExitCode != 0)
            throw new InvalidOperationException($"Kopia failed with exit code {process.ExitCode}: {error}");

        return output;
    }

    private IReadOnlyList<KopiaSnapshotInfo> ParseSnapshotList(string json)
    {
        if (string.IsNullOrWhiteSpace(json))
            return Array.Empty<KopiaSnapshotInfo>();

        using var doc = JsonDocument.Parse(json);
        var root = doc.RootElement;

        var results = new List<KopiaSnapshotInfo>(root.GetArrayLength());
        foreach (var item in root.EnumerateArray())
        {
            var id = item.GetProperty("id").GetString() ?? string.Empty;
            var description = item.GetProperty("description").GetString() ?? string.Empty;
            var startTime = item.GetProperty("startTime").GetDateTimeOffset();
            var totalSize = item.GetProperty("stats").GetProperty("totalSize").GetInt64();
            results.Add(new KopiaSnapshotInfo(id, description, startTime, totalSize));
        }

        return results;
    }
}

public record KopiaSnapshotInfo(
    string Id,
    string Description,
    DateTimeOffset StartTime,
    long TotalSize
);
