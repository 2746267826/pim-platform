using System.Windows;
using Microsoft.Extensions.DependencyInjection;
using Pim.Client.Core.Services;

namespace Pim.Client.App;

public partial class LoginWindow : Window
{
    private readonly AuthService _authService;

    public LoginWindow()
    {
        InitializeComponent();
        _authService = App.Services.GetRequiredService<AuthService>();
    }

    private async void OnLogin(object sender, RoutedEventArgs e)
    {
        var username = UsernameBox.Text.Trim();
        var password = PasswordBox.Password;

        if (string.IsNullOrEmpty(username) || string.IsNullOrEmpty(password))
        {
            ShowError("请填写用户名和密码");
            return;
        }

        LoginButton.IsEnabled = false;
        LoginButton.Content = "登录中...";
        ErrorText.Visibility = Visibility.Collapsed;

        try
        {
            var ok = await _authService.LoginAsync(username, password);
            if (ok)
            {
                DialogResult = true;
                Close();
            }
            else
            {
                ShowError("登录失败，请检查用户名和密码");
            }
        }
        catch (Exception ex)
        {
            ShowError($"连接失败：{ex.Message}");
        }
        finally
        {
            LoginButton.IsEnabled = true;
            LoginButton.Content = "登录";
        }
    }

    private void OnSkip(object sender, RoutedEventArgs e)
    {
        DialogResult = false;
        Close();
    }

    private void ShowError(string msg)
    {
        ErrorText.Text = msg;
        ErrorText.Visibility = Visibility.Visible;
    }
}
