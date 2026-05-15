namespace Pim.Client.App.Services;

public interface INavigationService
{
    event Action<string>? Navigated;
    string CurrentView { get; }
    void NavigateTo(string viewName);
}
