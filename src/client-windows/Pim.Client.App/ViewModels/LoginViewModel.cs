using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using Pim.Client.Core.Services;

namespace Pim.Client.App.ViewModels;

public partial class LoginViewModel : ObservableObject
{
    private readonly AuthService _authService;

    [ObservableProperty] private string _username = string.Empty;
    [ObservableProperty] private string _password = string.Empty;
    [ObservableProperty] private string _email = string.Empty;
    [ObservableProperty] private string _displayName = string.Empty;
    [ObservableProperty] private string _errorMessage = string.Empty;
    [ObservableProperty] private bool _isLoading;
    [ObservableProperty] private bool _isRegisterMode;

    public event Action? LoginSucceeded;

    public LoginViewModel(AuthService authService)
    {
        _authService = authService;
    }

    [RelayCommand(CanExecute = nameof(CanLogin))]
    private async Task LoginAsync()
    {
        ErrorMessage = string.Empty;
        IsLoading = true;
        try
        {
            var success = await _authService.LoginAsync(Username, Password);
            if (success)
                LoginSucceeded?.Invoke();
            else
                ErrorMessage = "用户名或密码错误";
        }
        catch (Exception ex)
        {
            ErrorMessage = $"连接失败: {ex.Message}";
        }
        finally
        {
            IsLoading = false;
        }
    }

    private bool CanLogin() =>
        !string.IsNullOrWhiteSpace(Username) && !string.IsNullOrWhiteSpace(Password) && !IsLoading;

    [RelayCommand(CanExecute = nameof(CanRegister))]
    private async Task RegisterAsync()
    {
        ErrorMessage = string.Empty;
        IsLoading = true;
        try
        {
            var success = await _authService.RegisterAsync(Username, Email, Password,
                string.IsNullOrWhiteSpace(DisplayName) ? null : DisplayName);
            if (success)
            {
                var loginOk = await _authService.LoginAsync(Username, Password);
                if (loginOk)
                    LoginSucceeded?.Invoke();
                else
                    ErrorMessage = "注册成功但登录失败，请手动登录";
            }
            else
                ErrorMessage = "注册失败，用户名或邮箱可能已被使用";
        }
        catch (Exception ex)
        {
            ErrorMessage = $"连接失败: {ex.Message}";
        }
        finally
        {
            IsLoading = false;
        }
    }

    private bool CanRegister() =>
        !string.IsNullOrWhiteSpace(Username) &&
        !string.IsNullOrWhiteSpace(Email) &&
        !string.IsNullOrWhiteSpace(Password) &&
        Password.Length >= 6 &&
        !IsLoading;

    [RelayCommand]
    private void ToggleMode()
    {
        IsRegisterMode = !IsRegisterMode;
        ErrorMessage = string.Empty;
    }
}
