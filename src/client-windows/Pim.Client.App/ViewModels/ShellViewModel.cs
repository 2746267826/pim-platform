using System.Collections.ObjectModel;
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using Pim.Client.Core.Models;
using Pim.Client.Core.Services;

namespace Pim.Client.App.ViewModels;

public partial class ShellViewModel : ObservableObject
{
    private readonly ApiClient _apiClient;

    [ObservableProperty] private string _currentView = "timeline";
    [ObservableProperty] private DateTime _selectedDate = DateTime.Today;
    [ObservableProperty] private string _userDisplayName = string.Empty;
    [ObservableProperty] private ObservableCollection<CalendarResponse> _calendars = new();
    [ObservableProperty] private ObservableCollection<TaskListDisplay> _taskLists = new();

    public ShellViewModel(ApiClient apiClient)
    {
        _apiClient = apiClient;
    }

    public void SetUserInfo(string displayName)
    {
        UserDisplayName = displayName;
    }

    [RelayCommand]
    private void Navigate(string viewName)
    {
        CurrentView = viewName;
    }

    [RelayCommand]
    private void GoToToday()
    {
        SelectedDate = DateTime.Today;
    }

    [RelayCommand]
    private void GoToPrevDay()
    {
        SelectedDate = SelectedDate.AddDays(-1);
    }

    [RelayCommand]
    private void GoToNextDay()
    {
        SelectedDate = SelectedDate.AddDays(1);
    }

    [RelayCommand]
    private void GoToPrevWeek()
    {
        SelectedDate = SelectedDate.AddDays(-7);
    }

    [RelayCommand]
    private void GoToNextWeek()
    {
        SelectedDate = SelectedDate.AddDays(7);
    }

    [RelayCommand]
    private void GoToPrevMonth()
    {
        SelectedDate = SelectedDate.AddMonths(-1);
    }

    [RelayCommand]
    private void GoToNextMonth()
    {
        SelectedDate = SelectedDate.AddMonths(1);
    }

    public async Task LoadCalendarsAsync()
    {
        try
        {
            var result = await _apiClient.GetAsync<ApiResponse<List<CalendarResponse>>>("/calendar/calendars");
            Calendars = new ObservableCollection<CalendarResponse>(result?.Data ?? new List<CalendarResponse>());
        }
        catch
        {
            // Silently handle failures; calendars will remain empty until a retry
        }
    }

    public async Task LoadTaskListsAsync()
    {
        try
        {
            var result = await _apiClient.GetAsync<ApiResponse<List<TaskListDisplay>>>("/calendar/task-lists");
            TaskLists = new ObservableCollection<TaskListDisplay>(result?.Data ?? new List<TaskListDisplay>());
        }
        catch
        {
            // Silently handle failures; task lists will remain empty until a retry
        }
    }
}

/// <summary>
/// Simple display model for task lists, since TaskListResponse does not exist as a server model.
/// </summary>
public class TaskListDisplay
{
    public string Id { get; set; } = "";
    public string Name { get; set; } = "";
}
