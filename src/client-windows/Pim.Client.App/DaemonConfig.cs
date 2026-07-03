using System.IO;
using System.Text.Json;
using Pim.Client.Core;

namespace Pim.Client.App;

public class DaemonConfig
{
    public string ServerUrl { get; set; } = ClientDefaults.DefaultServerUrl;
    public bool AutoStart { get; set; } = true;

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
            File.WriteAllText(FilePath, JsonSerializer.Serialize(this));
        }
        catch { }
    }
}
