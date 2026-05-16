using System.Collections.ObjectModel;
using System.Windows.Media;
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using Pim.Client.Core.Models;
using Pim.Client.Core.Services;

namespace Pim.Client.App.ViewModels;

public partial class TaskListViewModel : ObservableObject
{
    private readonly ApiClient _api;
    private List<TaskResponse> _allTasks = new();

    [ObservableProperty]
    private ObservableCollection<TaskDisplayItem> _tasks = new();

    [ObservableProperty]
    private string _filter = "all";

    [ObservableProperty]
    private string _searchText = string.Empty;

    [ObservableProperty]
    private string _summaryText = string.Empty;

    [ObservableProperty]
    private bool _isLoading;

    public TaskListViewModel(ApiClient api)
    {
        _api = api;
    }

    partial void OnFilterChanged(string value)
    {
        ApplyFilter();
    }

    partial void OnSearchTextChanged(string value)
    {
        ApplyFilter();
    }

    [RelayCommand]
    public async Task LoadTasksAsync()
    {
        IsLoading = true;
        try
        {
            var result = await _api.GetAsync<ApiResponse<List<TaskResponse>>>("/calendar/tasks");
            _allTasks = result?.Data ?? new List<TaskResponse>();
            ApplyFilter();
        }
        catch (Exception ex)
        {
            SummaryText = $"加载失败: {ex.Message}";
        }
        finally
        {
            IsLoading = false;
        }
    }

    [RelayCommand]
    private void SetFilter(string filter)
    {
        Filter = filter;
    }

    private void ApplyFilter()
    {
        var filtered = _allTasks.AsEnumerable();

        // Category filter
        filtered = Filter switch
        {
            "inbox" => filtered.Where(t => t.IsInbox),
            "high" => filtered.Where(t => t.Priority == 1),
            "today" => filtered.Where(t =>
                (t.DtStart?.Date == DateTime.Today) ||
                (t.Due?.Date == DateTime.Today)),
            _ => filtered // "all"
        };

        // Search filter
        if (!string.IsNullOrWhiteSpace(SearchText))
        {
            var search = SearchText.Trim().ToLowerInvariant();
            filtered = filtered.Where(t =>
                t.Title.Contains(search, StringComparison.OrdinalIgnoreCase) ||
                (t.Description?.Contains(search, StringComparison.OrdinalIgnoreCase) ?? false));
        }

        var list = filtered.ToList();

        Tasks = new ObservableCollection<TaskDisplayItem>(
            list.Select(t => new TaskDisplayItem
            {
                Id = t.Id,
                Title = t.Title,
                Description = t.Description,
                DurationMinutes = ParseDuration(t.EstimatedDuration),
                DtStart = t.DtStart,
                Due = t.Due,
                Priority = t.Priority,
                IsInbox = t.IsInbox
            }));

        var totalCount = _allTasks.Count;
        var inboxCount = _allTasks.Count(t => t.IsInbox);
        SummaryText = $"共 {totalCount} 个任务 · {inboxCount} 个未排程";
    }

    private static int? ParseDuration(string? isoDuration)
    {
        if (string.IsNullOrEmpty(isoDuration)) return null;
        try
        {
            var span = System.Xml.XmlConvert.ToTimeSpan(isoDuration);
            return (int)span.TotalMinutes;
        }
        catch
        {
            return null;
        }
    }
}

public class TaskDisplayItem
{
    public string Id { get; set; } = string.Empty;
    public string Title { get; set; } = string.Empty;
    public string? Description { get; set; }
    public int? DurationMinutes { get; set; }
    public DateTimeOffset? DtStart { get; set; }
    public DateTimeOffset? Due { get; set; }
    public int Priority { get; set; }
    public bool IsInbox { get; set; }

    public string PriorityColor => Priority switch
    {
        1 => "#E53935",
        3 => "#43A047",
        _ => "#FFA726"
    };

    public string StatusLabel => IsInbox ? "收件箱" : "已排程";

    public SolidColorBrush PriorityBrush =>
        new((Color)ColorConverter.ConvertFromString(PriorityColor));

    public SolidColorBrush StatusBrush =>
        new((Color)ColorConverter.ConvertFromString(IsInbox ? "#999999" : "#43A047"));
}
