using System.Reflection;

namespace Pim.Client.App;

public record TrayMenuItem(string Text);

public class TrayMenu
{
    public List<TrayMenuItem> Items { get; } = new();
}

public partial class TrayIcon
{
    public static TrayMenu BuildMenu(string? version = null, string? serverUrl = null)
    {
        var v = string.IsNullOrWhiteSpace(version) ? GetVersion() : version!.Trim();
        var menu = new TrayMenu();
        menu.Items.Add(new TrayMenuItem($"关于 PIM v{v}"));
        menu.Items.Add(new TrayMenuItem("检查更新"));
        return menu;
    }

    private static string GetVersion()
    {
        try
        {
            var asm = typeof(TrayIcon).Assembly;
            var attr = asm.GetCustomAttributes(false).OfType<AssemblyInformationalVersionAttribute>().FirstOrDefault();
            if (attr != null && !string.IsNullOrWhiteSpace(attr.InformationalVersion))
                return attr.InformationalVersion;
            var appType = Type.GetType("Pim.Client.App.App, Pim.Client.App");
            if (appType != null)
            {
                var appAttr = appType.Assembly.GetCustomAttributes(false).OfType<AssemblyInformationalVersionAttribute>().FirstOrDefault();
                if (appAttr != null && !string.IsNullOrWhiteSpace(appAttr.InformationalVersion))
                    return appAttr.InformationalVersion;
            }
        }
        catch { }
        return "0.0.0-local";
    }

}
