using System.IO;
using System.Text.Json;
using Pim.Client.Core;
using Pim.Client.Core.Models;

namespace Pim.Client.App;

public class DaemonConfig
{
    public string ServerUrl { get; set; } = ClientDefaults.DefaultServerUrl;
    public bool AutoStart { get; set; } = true;
    public TrackerConfig Tracker { get; set; } = new();

    private static readonly string Dir = Path.Combine(
        Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData), "PIM");
    private static readonly string FilePath = Path.Combine(Dir, "config.json");

    public static DaemonConfig Load()
    {
        try
        {
            if (File.Exists(FilePath))
            {
                var json = File.ReadAllText(FilePath);
                return JsonSerializer.Deserialize<DaemonConfig>(json) ?? new();
            }
        }
        catch { }
        return new();
    }

    public void Save()
    {
        try
        {
            Directory.CreateDirectory(Dir);
            var json = JsonSerializer.Serialize(this);
            var tmp = FilePath + ".tmp";
            File.WriteAllText(tmp, json);
            // Atomic replace: move temp over target
            File.Move(tmp, FilePath, overwrite: true);
        }
        catch { }
    }
}
