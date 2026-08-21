using System.Threading.Tasks;
using System.Windows;

namespace Pim.Shell.App;

public partial class QuickNoteWindow : Window
{
    private readonly string _serverUrl;
    public QuickNoteWindow(string serverUrl) { InitializeComponent(); _serverUrl = serverUrl; Loaded += async (_, _) => await InitAsync(); }
    private async Task InitAsync()
    {
        await Web.EnsureCoreWebView2Async();
        await Web.CoreWebView2.AddScriptToExecuteOnDocumentCreatedAsync(ShellBridge.Script);
        Web.CoreWebView2.Navigate($"{_serverUrl.TrimEnd('/')}/quick-notes?embed=1");
    }
}
