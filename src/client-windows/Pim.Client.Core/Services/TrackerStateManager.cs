using System.Text.Json;

namespace Pim.Client.Core.Services;

public sealed class TrackerPersistentState
{
    public DateTimeOffset? LastPollTime { get; set; }
    public DateTimeOffset? LastActiveTime { get; set; }
    public string? DeviceId { get; set; }
}

public sealed class TrackerStateManager
{
    private readonly string _stateFilePath;
    private readonly object _lock = new();

    public TrackerStateManager(string? stateFilePath = null)
    {
        if (!string.IsNullOrWhiteSpace(stateFilePath))
        {
            _stateFilePath = stateFilePath;
        }
        else
        {
            var baseDir = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData), "PimDaemon");
            _stateFilePath = Path.Combine(baseDir, "tracker_state.json");
        }
    }

    public TrackerPersistentState? LoadState()
    {
        lock (_lock)
        {
            try
            {
                if (!File.Exists(_stateFilePath))
                    return null;

                var json = File.ReadAllText(_stateFilePath);
                return JsonSerializer.Deserialize<TrackerPersistentState>(json);
            }
            catch
            {
                return null;
            }
        }
    }

    public void SaveState(DateTimeOffset lastPollTime, DateTimeOffset? lastActiveTime = null, string? deviceId = null)
    {
        lock (_lock)
        {
            try
            {
                var dir = Path.GetDirectoryName(_stateFilePath);
                if (!string.IsNullOrEmpty(dir) && !Directory.Exists(dir))
                {
                    Directory.CreateDirectory(dir);
                }

                var state = new TrackerPersistentState
                {
                    LastPollTime = lastPollTime,
                    LastActiveTime = lastActiveTime ?? lastPollTime,
                    DeviceId = deviceId ?? Environment.MachineName
                };

                var json = JsonSerializer.Serialize(state);
                var tempPath = _stateFilePath + ".tmp";
                File.WriteAllText(tempPath, json);
                File.Move(tempPath, _stateFilePath, overwrite: true);
            }
            catch
            {
                // Best-effort persistence
            }
        }
    }
}
