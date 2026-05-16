using System.Collections.ObjectModel;
using System.Xml;
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using Pim.Client.Core.Models;
using Pim.Client.Core.Services;

namespace Pim.Client.App.ViewModels;

public partial class WeekViewModel : ObservableObject
{
    private readonly ApiClient _api;

    [ObservableProperty]
    private DateTime _weekStart;

    [ObservableProperty]
    private ObservableCollection<WeekDayColumn> _dayColumns = new();

    [ObservableProperty]
    private ObservableCollection<HourEntry> _hourEntries = new();

    [ObservableProperty]
    private bool _isLoading;

    [ObservableProperty]
    private string _errorMessage = string.Empty;

    public WeekViewModel(ApiClient api)
    {
        _api = api;
        WeekStart = StartOfWeek(DateTime.Now);
        InitHourEntries();
    }

    private void InitHourEntries()
    {
        var entries = new ObservableCollection<HourEntry>();
        for (int h = 6; h <= 23; h++)
        {
            entries.Add(new HourEntry { Label = $"{h:D2}:00", Top = (h - 6) * 60.0 });
        }

        HourEntries = entries;
    }

    private static DateTime StartOfWeek(DateTime date)
    {
        var diff = (7 + (date.DayOfWeek - DayOfWeek.Monday)) % 7;
        return date.Date.AddDays(-diff);
    }

    public string WeekLabel
    {
        get
        {
            var end = WeekStart.AddDays(6);
            return $"{WeekStart:MM/dd} - {end:MM/dd}";
        }
    }

    partial void OnWeekStartChanged(DateTime value)
    {
        OnPropertyChanged(nameof(WeekLabel));
    }

    public async Task LoadWeekAsync()
    {
        IsLoading = true;
        ErrorMessage = string.Empty;
        try
        {
            var start = new DateTimeOffset(WeekStart, TimeSpan.Zero);
            var end = new DateTimeOffset(WeekStart.AddDays(7), TimeSpan.Zero);

            var eventResult = await _api.GetAsync<ApiResponse<List<EventResponse>>>(
                $"/calendar/events?start={start:yyyy-MM-dd}&end={end:yyyy-MM-dd}");

            var taskResult = await _api.GetAsync<ApiResponse<List<TaskResponse>>>(
                "/calendar/tasks?inbox=false");

            var events = eventResult?.Data ?? new List<EventResponse>();
            var tasks = taskResult?.Data ?? new List<TaskResponse>();

            BuildDayColumns(events, tasks);
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

    private void BuildDayColumns(List<EventResponse> events, List<TaskResponse> tasks)
    {
        var columns = new ObservableCollection<WeekDayColumn>();
        var today = DateTime.Today;

        for (int i = 0; i < 7; i++)
        {
            var date = WeekStart.AddDays(i);
            var items = new List<WeekItemDisplay>();

            // Events that overlap this day
            foreach (var evt in events)
            {
                if (evt.DtStart.Date > date || evt.DtEnd.Date < date)
                    continue;

                int dayStartMinutes = (int)(date - date.Date).TotalMinutes;
                int dayEndMinutes = dayStartMinutes + 24 * 60;

                var rangeStart = evt.DtStart.DateTime;
                var rangeEnd = evt.DtEnd.DateTime;

                // Clamp to day boundaries
                var visibleStart = rangeStart < date ? date : rangeStart;
                var visibleEnd = rangeEnd > date.AddDays(1) ? date.AddDays(1) : rangeEnd;

                int startMinutes = (int)(visibleStart - date).TotalMinutes;
                int endMinutes = (int)(visibleEnd - date).TotalMinutes;

                // Clamp to visible grid range (6:00 - 24:00 = 360min - 1440min)
                int gridStart = Math.Max(startMinutes, 6 * 60);
                int gridEnd = Math.Min(endMinutes, 24 * 60);
                int duration = gridEnd - gridStart;

                if (duration <= 0) continue;

                items.Add(new WeekItemDisplay
                {
                    Id = evt.Id,
                    Title = evt.Title,
                    TopOffset = gridStart - 6 * 60,
                    DurationMinutes = duration,
                    Type = "event",
                    ColorHex = "#1565c0"
                });
            }

            // Tasks scheduled on this day
            foreach (var task in tasks.Where(t => t.DtStart.HasValue && t.DtStart.Value.Date == date))
            {
                var startTime = task.DtStart!.Value;
                int minutesFromMidnight = startTime.Hour * 60 + startTime.Minute;

                if (minutesFromMidnight < 6 * 60) continue;
                if (minutesFromMidnight >= 24 * 60) continue;

                int durationMin = 60;
                if (!string.IsNullOrEmpty(task.EstimatedDuration))
                {
                    try
                    {
                        var ts = XmlConvert.ToTimeSpan(task.EstimatedDuration);
                        durationMin = Math.Max((int)ts.TotalMinutes, 1);
                    }
                    catch { }
                }

                var color = task.Priority switch
                {
                    1 => "#E53935",
                    3 => "#43A047",
                    _ => "#FFA726"
                };

                items.Add(new WeekItemDisplay
                {
                    Id = task.Id,
                    Title = task.Title,
                    TopOffset = minutesFromMidnight - 6 * 60,
                    DurationMinutes = durationMin,
                    Type = "task",
                    ColorHex = color
                });
            }

            columns.Add(new WeekDayColumn
            {
                Date = date,
                DayLabel = _dayLabels[i],
                DateLabel = date.Day.ToString(),
                IsToday = date == today,
                Items = new ObservableCollection<WeekItemDisplay>(
                    items.OrderBy(d => d.TopOffset))
            });
        }

        DayColumns = columns;
    }

    private static readonly string[] _dayLabels = { "一", "二", "三", "四", "五", "六", "日" };

    [RelayCommand]
    private void PreviousWeek()
    {
        WeekStart = WeekStart.AddDays(-7);
        _ = LoadWeekAsync();
    }

    [RelayCommand]
    private void NextWeek()
    {
        WeekStart = WeekStart.AddDays(7);
        _ = LoadWeekAsync();
    }

    [RelayCommand]
    private void GoToToday()
    {
        WeekStart = StartOfWeek(DateTime.Now);
        _ = LoadWeekAsync();
    }
}

public class HourEntry
{
    public string Label { get; set; } = string.Empty;
    public double Top { get; set; }
}

public class WeekDayColumn
{
    public DateTime Date { get; set; }
    public string DayLabel { get; set; } = string.Empty;
    public string DateLabel { get; set; } = string.Empty;
    public bool IsToday { get; set; }
    public ObservableCollection<WeekItemDisplay> Items { get; set; } = new();
}

public class WeekItemDisplay
{
    public string Id { get; set; } = string.Empty;
    public string Title { get; set; } = string.Empty;

    /// <summary>Offset in pixels from the top of the time grid (6:00 = 0px, 60px/hour).</summary>
    public double TopOffset { get; set; }

    /// <summary>Duration in minutes (also equals pixel height at 60px/hour scale).</summary>
    public int DurationMinutes { get; set; }

    public string Type { get; set; } = string.Empty;
    public string ColorHex { get; set; } = "#1565c0";

    public double BlockHeight => Math.Max(20, DurationMinutes);
}
