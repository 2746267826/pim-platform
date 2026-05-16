using System.Collections.ObjectModel;
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using Pim.Client.Core.Models;
using Pim.Client.Core.Services;

namespace Pim.Client.App.ViewModels;

public partial class TimelineViewModel : ObservableObject
{
    private readonly ApiClient _apiClient;

    [ObservableProperty] private DateTime _selectedDate = DateTime.Today;
    [ObservableProperty] private ObservableCollection<TimelineItem> _displayItems = new();
    [ObservableProperty] private bool _isLoading;

    public TimelineViewModel(ApiClient apiClient)
    {
        _apiClient = apiClient;
    }

    public async Task LoadAsync(DateTime date)
    {
        SelectedDate = date;
        IsLoading = true;
        try
        {
            var from = date.Date;
            var to = from.AddDays(1);
            var eventsResult = await _apiClient.GetAsync<ApiResponse<List<EventResponse>>>(
                $"/calendar/events?start={from:O}&end={to:O}");
            var tasksResult = await _apiClient.GetAsync<ApiResponse<List<TaskResponse>>>(
                "/calendar/tasks");

            var items = new List<TimelineItem>();
            var events = eventsResult?.Data;
            if (events != null)
                items.AddRange(events.Select(e => TimelineItem.FromEvent(e)));
            var tasks = tasksResult?.Data;
            if (tasks != null)
                items.AddRange(tasks
                    .Where(t => t.DtStart?.Date == date.Date)
                    .Select(t => TimelineItem.FromTask(t)));
            DisplayItems = new ObservableCollection<TimelineItem>(
                items.OrderBy(i => i.TopOffset));
        }
        finally { IsLoading = false; }
    }

    [RelayCommand]
    private async Task SelectDate(DateTime date)
    {
        await LoadAsync(date);
    }
}

public class TimelineItem
{
    public string Id { get; set; } = "";
    public string Title { get; set; } = "";
    public double TopOffset { get; set; }
    public double BlockHeight { get; set; }
    public string Type { get; set; } = "event";
    public string ColorHex { get; set; } = "#6B5EE4";
    public string Subtitle { get; set; } = "";
    public double PlanWidth => 200;

    public static TimelineItem FromEvent(EventResponse e)
    {
        var start = e.DtStart;
        var end = e.DtEnd;
        if (end <= start) end = start.AddHours(1);
        return new TimelineItem
        {
            Id = e.Id,
            Title = e.Title,
            TopOffset = (start.Hour + start.Minute / 60.0) * 80,
            BlockHeight = Math.Max((end - start).TotalMinutes / 60.0 * 80, 20),
            Type = "event",
            ColorHex = "#6B5EE4",
            Subtitle = $"{start:HH:mm}-{end:HH:mm}"
        };
    }

    public static TimelineItem FromTask(TaskResponse t)
    {
        var start = t.DtStart ?? DateTimeOffset.Now;
        var minutes = ParseDuration(t.EstimatedDuration) ?? 60;
        return new TimelineItem
        {
            Id = t.Id,
            Title = t.Title,
            TopOffset = (start.Hour + start.Minute / 60.0) * 80,
            BlockHeight = Math.Max(minutes / 60.0 * 80, 20),
            Type = "task",
            ColorHex = t.Priority switch { 1 => "#E53935", 3 => "#43A047", _ => "#FFA726" },
            Subtitle = $"{start:HH:mm} · {minutes}分钟"
        };
    }

    private static int? ParseDuration(string? isoDuration)
    {
        if (string.IsNullOrEmpty(isoDuration)) return null;
        try
        {
            var span = System.Xml.XmlConvert.ToTimeSpan(isoDuration);
            return (int)span.TotalMinutes;
        }
        catch { return null; }
    }
}
