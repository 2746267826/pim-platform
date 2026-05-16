using System.Collections.ObjectModel;
using System.Windows.Media;
using System.Xml;
using CommunityToolkit.Mvvm.ComponentModel;
using Pim.Client.Core.Models;
using Pim.Client.Core.Services;

namespace Pim.Client.App.ViewModels;

public partial class InboxPanelViewModel : ObservableObject
{
    private readonly ApiClient _api;

    [ObservableProperty]
    private ObservableCollection<InboxTaskItem> _items = new();

    [ObservableProperty]
    private bool _isEmpty = true;

    public string EmptyText => "所有任务均已排入日程";

    public InboxPanelViewModel(ApiClient api)
    {
        _api = api;
    }

    public async Task LoadAsync()
    {
        try
        {
            var result = await _api.GetAsync<ApiResponse<List<TaskResponse>>>("/calendar/tasks");
            var tasks = result?.Data ?? new List<TaskResponse>();
            var unscheduled = tasks
                .Where(t => t.DtStart is null)
                .Select(t => new InboxTaskItem
                {
                    Id = t.Id,
                    Title = t.Title,
                    Priority = t.Priority,
                    DurationMinutes = TryParseDuration(t.EstimatedDuration),
                    Due = t.Due
                })
                .ToList();

            Items = new ObservableCollection<InboxTaskItem>(unscheduled);
            IsEmpty = Items.Count == 0;
        }
        catch
        {
            Items = new ObservableCollection<InboxTaskItem>();
            IsEmpty = true;
        }
    }

    private static int? TryParseDuration(string? isoDuration)
    {
        if (string.IsNullOrWhiteSpace(isoDuration)) return null;
        try
        {
            var ts = XmlConvert.ToTimeSpan(isoDuration);
            return (int)ts.TotalMinutes;
        }
        catch
        {
            return null;
        }
    }
}

public class InboxTaskItem
{
    public string Id { get; set; } = string.Empty;
    public string Title { get; set; } = string.Empty;
    public int Priority { get; set; }
    public int? DurationMinutes { get; set; }
    public DateTimeOffset? Due { get; set; }

    public string PriorityColor => Priority switch
    {
        1 => "#E53935",
        3 => "#43A047",
        _ => "#FFA726"
    };

    public SolidColorBrush PriorityBrush =>
        new((Color)ColorConverter.ConvertFromString(PriorityColor));

    public bool IsOverdue =>
        Due.HasValue && Due.Value < DateTimeOffset.Now;
}
