using System.Windows;
using System.Windows.Interop;

namespace Pim.Shell.App;

public partial class App : System.Windows.Application
{
    private TrayManager? _tray;
    private HotKeyManager? _hotKey;

    protected override void OnStartup(System.Windows.StartupEventArgs e)
    {
        base.OnStartup(e);
        var config = ShellConfig.Load();
        var normalized = ServerAddress.Normalize(config.ServerUrl);
        Window main;
        if (normalized is null) main = new SetupWindow();
        else main = new ShellWindow(normalized);
        main.Show();
        _tray = new TrayManager();
        _tray.Show(normalized ?? "", () => { main.Show(); main.Activate(); }, () => new QuickNoteWindow(normalized ?? config.ServerUrl).Show(), () => { new SetupWindow().Show(); }, () => Shutdown());
        main.SourceInitialized += (_, _) => _hotKey = new HotKeyManager(new WindowInteropHelper(main).Handle);
    }
    protected override void OnExit(System.Windows.ExitEventArgs e) { _hotKey?.Dispose(); _tray?.Dispose(); base.OnExit(e); }
}
