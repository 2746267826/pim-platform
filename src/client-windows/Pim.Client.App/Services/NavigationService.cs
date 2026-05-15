namespace Pim.Client.App.Services;

public class NavigationService : INavigationService
{
    public event Action<string>? Navigated;
    public string CurrentView { get; private set; } = "calendar";

    public void NavigateTo(string viewName)
    {
        if (CurrentView == viewName) return;
        CurrentView = viewName;
        Navigated?.Invoke(viewName);
    }
}
