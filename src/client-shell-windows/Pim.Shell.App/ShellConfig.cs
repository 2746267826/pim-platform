using System.IO;
using System.Text.Json;

namespace Pim.Shell.App;

public class ShellConfig
{
    public string ServerUrl { get; set; } = "";

    private static string DefaultPath() => Path.Combine(
        Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData), "PIM", "shell", "config.json");

    public static ShellConfig Load() => Load(DefaultPath());
    public void Save() => Save(DefaultPath());

    public static ShellConfig Load(string path)
    {
        try
        {
            if (File.Exists(path))
                return JsonSerializer.Deserialize<ShellConfig>(File.ReadAllText(path)) ?? new();
        }
        catch { }
        return new();
    }

    public void Save(string path)
    {
        var dir = Path.GetDirectoryName(path)!;
        Directory.CreateDirectory(dir);
        File.WriteAllText(path, JsonSerializer.Serialize(this));
    }
}
