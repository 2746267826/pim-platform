using System;
using System.Windows;
using Microsoft.Web.WebView2.Core;

namespace Pim.Shell.App;

public partial class ShellWindow : Window
{
    private readonly string _serverUrl;

    public ShellWindow(string serverUrl)
    {
        InitializeComponent();
        _serverUrl = serverUrl;
        Loaded += async (_, _) => await InitializeAsync();
        StateChanged += OnStateChanged;
    }

    private async Task InitializeAsync()
    {
        await Web.EnsureCoreWebView2Async();
        Web.CoreWebView2.Settings.AreDefaultContextMenusEnabled = true;
        Web.CoreWebView2.Settings.AreDevToolsEnabled = true;
        await Web.CoreWebView2.AddScriptToExecuteOnDocumentCreatedAsync(ShellBridge.Script);
        Web.CoreWebView2.DocumentTitleChanged += (_, _) =>
            Title = string.IsNullOrWhiteSpace(Web.CoreWebView2.DocumentTitle) ? "PIM" : Web.CoreWebView2.DocumentTitle;
        Web.CoreWebView2.NavigationCompleted += OnNavigationCompleted;
        Web.CoreWebView2.Navigate(_serverUrl);
    }

    private void OnNavigationCompleted(object? sender, CoreWebView2NavigationCompletedEventArgs e)
        => ErrorOverlay.Visibility = e.IsSuccess ? Visibility.Collapsed : Visibility.Visible;

    private void OnRetryClick(object sender, RoutedEventArgs e)
    {
        ErrorOverlay.Visibility = Visibility.Collapsed;
        Web.CoreWebView2.Navigate(_serverUrl);
    }

    private void OnChangeServerClick(object sender, RoutedEventArgs e)
    {
        new SetupWindow().Show();
        Close();
    }

    private void OnStateChanged(object? sender, EventArgs e)
    {
        if (WindowState == WindowState.Minimized) Hide();
    }
}
