using System.Collections.ObjectModel;
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using Pim.Client.Core.Models;
using Pim.Client.Core.Services;

namespace Pim.Client.App.ViewModels;

public partial class TaskListViewModel : ObservableObject
{
    private readonly ApiClient _api;

    [ObservableProperty] private ObservableCollection<TaskResponse> _tasks = new();
    [ObservableProperty] private bool _isLoading;
    [ObservableProperty] private bool _isEditorOpen;
    [ObservableProperty] private string _editorTitle = string.Empty;
    [ObservableProperty] private string? _editorDescription;
    [ObservableProperty] private int _editorPriority;
    [ObservableProperty] private bool _showInboxOnly;
    [ObservableProperty] private string _errorMessage = string.Empty;

    public TaskListViewModel(ApiClient api)
    {
        _api = api;
    }

    [RelayCommand]
    public async Task LoadTasksAsync()
    {
        IsLoading = true;
        ErrorMessage = string.Empty;
        try
        {
            var inboxParam = ShowInboxOnly ? "?inbox=true" : "";
            var result = await _api.GetAsync<ApiResponse<List<TaskResponse>>>($"/calendar/tasks{inboxParam}");
            Tasks = new ObservableCollection<TaskResponse>(result?.Data ?? new List<TaskResponse>());
        }
        catch (Exception ex)
        {
            ErrorMessage = $"加载失败: {ex.Message}";
        }
        finally
        {
            IsLoading = false;
        }
    }

    [RelayCommand]
    private void OpenCreateEditor()
    {
        EditorTitle = string.Empty;
        EditorDescription = null;
        EditorPriority = 0;
        IsEditorOpen = true;
    }

    [RelayCommand]
    private void CloseEditor()
    {
        IsEditorOpen = false;
    }

    [RelayCommand]
    private async Task SaveTaskAsync()
    {
        if (string.IsNullOrWhiteSpace(EditorTitle)) return;
        IsLoading = true;
        try
        {
            await _api.PostAsync<object>("/calendar/tasks", new
            {
                title = EditorTitle,
                description = EditorDescription,
                priority = EditorPriority
            });
            IsEditorOpen = false;
            await LoadTasksAsync();
        }
        catch (Exception ex)
        {
            ErrorMessage = $"保存失败: {ex.Message}";
        }
        finally
        {
            IsLoading = false;
        }
    }

    [RelayCommand]
    private async Task ToggleInboxAsync()
    {
        ShowInboxOnly = !ShowInboxOnly;
        await LoadTasksAsync();
    }
}
