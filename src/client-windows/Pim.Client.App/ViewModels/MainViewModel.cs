using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using Pim.Client.App.Services;
using Pim.Client.Core.Services;

namespace Pim.Client.App.ViewModels;

public partial class MainViewModel : ObservableObject
{
    private readonly INavigationService _navigation;
    private readonly AuthService _authService;

    [ObservableProperty] private string _statusText = "已连接";
    [ObservableProperty] private string _currentView = "calendar";
    [ObservableProperty] private string _userDisplayName = string.Empty;

    public MainViewModel(INavigationService navigation, AuthService authService)
    {
        _navigation = navigation;
        _authService = authService;
        _navigation.Navigated += OnNavigated;
        UserDisplayName = authService.CurrentDisplayName ?? authService.CurrentUsername ?? "";
    }

    private void OnNavigated(string viewName)
    {
        CurrentView = viewName;
    }

    [RelayCommand]
    private void Navigate(string viewName)
    {
        _navigation.NavigateTo(viewName);
    }

    [RelayCommand]
    private void Logout()
    {
        _authService.Logout();
        LoggedOut?.Invoke();
    }

    public event Action? LoggedOut;
}
