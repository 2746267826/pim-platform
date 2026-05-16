using System.Collections.ObjectModel;
using System.ComponentModel;
using System.Runtime.CompilerServices;
using System.Xml;
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using Pim.Client.Core.Models;
using Pim.Client.Core.Services;

namespace Pim.Client.App.ViewModels;

public partial class MonthViewModel : ObservableObject
{
    private readonly ApiClient _api;
    private List<EventResponse> _events = new();
    private List<TaskResponse> _tasks = new();

    [ObservableProperty]
    private DateTime _displayMonth;

    [ObservableProperty]
    private ObservableCollection<CalendarDay> _days = new();

    [ObservableProperty]
    private DateTime? _selectedDate;

    [ObservableProperty]
    private string _selectedDayLabel = string.Empty;

    [ObservableProperty]
    private ObservableCollection<PreviewItem> _selectedEvents = new();

    [ObservableProperty]
    private ObservableCollection<PreviewItem> _selectedTasks = new();

    [ObservableProperty]
    private bool _hasEvents;

    [ObservableProperty]
    private bool _hasTasks;

    public MonthViewModel(ApiClient api)
    {
        _api = api;
    }

    public string MonthLabel => $"{DisplayMonth.Year}年{DisplayMonth.Month}月";

    partial void OnDisplayMonthChanged(DateTime value)
    {
        OnPropertyChanged(nameof(MonthLabel));
    }

    public async Task LoadMonthAsync(DateTime monthStart)
    {
        DisplayMonth = new DateTime(monthStart.Year, monthStart.Month, 1);

        var start = new DateTimeOffset(DisplayMonth, TimeSpan.Zero);
        var end = start.AddMonths(1);

        try
        {
            var eventResult = await _api.GetAsync<ApiResponse<List<EventResponse>>>(
                $"/calendar/events?start={start:yyyy-MM-dd}&end={end:yyyy-MM-dd}");

            var taskResult = await _api.GetAsync<ApiResponse<List<TaskResponse>>>(
                "/calendar/tasks?inbox=false");

            _events = eventResult?.Data ?? new List<EventResponse>();
            _tasks = taskResult?.Data ?? new List<TaskResponse>();
        }
        catch
        {
            _events = new List<EventResponse>();
            _tasks = new List<TaskResponse>();
        }

        BuildDays();
    }

    private void BuildDays()
    {
        var today = DateTime.Today;
        var firstOfMonth = DisplayMonth;

        // Find the Sunday on or before the 1st of the month
        int diff = (7 + (firstOfMonth.DayOfWeek - DayOfWeek.Sunday)) % 7;
        var gridStart = firstOfMonth.AddDays(-diff);

        var dayList = new List<CalendarDay>();

        for (int i = 0; i < 42; i++)
        {
            var date = gridStart.AddDays(i);
            var isCurrentMonth = date.Month == firstOfMonth.Month;
            var isToday = date == today;
            var isSunday = date.DayOfWeek == DayOfWeek.Sunday;

            // Collect dots (max 4): events first, then tasks
            var dots = new List<string>();

            var dayEvents = _events.Where(e =>
                e.DtStart.Date <= date && e.DtEnd.Date >= date);
            foreach (var evt in dayEvents)
            {
                if (dots.Count >= 4) break;
                dots.Add("#1565c0");
            }

            var dayTasks = _tasks.Where(t =>
                t.DtStart.HasValue && t.DtStart.Value.Date == date);
            foreach (var task in dayTasks)
            {
                if (dots.Count >= 4) break;
                var color = task.Priority switch
                {
                    1 => "#E53935",
                    3 => "#43A047",
                    _ => "#FFA726"
                };
                dots.Add(color);
            }

            dayList.Add(new CalendarDay
            {
                Date = date,
                Day = date.Day,
                IsCurrentMonth = isCurrentMonth,
                IsToday = isToday,
                IsSunday = isSunday,
                Dots = dots
            });
        }

        Days = new ObservableCollection<CalendarDay>(dayList);
    }

    [RelayCommand]
    private async Task PreviousMonth()
    {
        SelectedDate = null;
        SelectedDayLabel = string.Empty;
        SelectedEvents = new ObservableCollection<PreviewItem>();
        SelectedTasks = new ObservableCollection<PreviewItem>();
        HasEvents = false;
        HasTasks = false;
        await LoadMonthAsync(DisplayMonth.AddMonths(-1));
    }

    [RelayCommand]
    private async Task NextMonth()
    {
        SelectedDate = null;
        SelectedDayLabel = string.Empty;
        SelectedEvents = new ObservableCollection<PreviewItem>();
        SelectedTasks = new ObservableCollection<PreviewItem>();
        HasEvents = false;
        HasTasks = false;
        await LoadMonthAsync(DisplayMonth.AddMonths(1));
    }

    [RelayCommand]
    private void SelectDay(CalendarDay? day)
    {
        if (day is null) return;

        // Deselect previous
        foreach (var d in Days)
        {
            d.IsSelected = false;
        }

        day.IsSelected = true;
        SelectedDate = day.Date;
        SelectedDayLabel = $"{day.Date.Year}年{day.Date.Month}月{day.Date.Day}日";

        var eventItems = new List<PreviewItem>();
        var taskItems = new List<PreviewItem>();

        // Events for this day
        foreach (var evt in _events.Where(e =>
            e.DtStart.Date <= day.Date && e.DtEnd.Date >= day.Date))
        {
            var startStr = evt.DtStart.ToLocalTime().ToString("HH:mm");
            var endStr = evt.DtEnd.ToLocalTime().ToString("HH:mm");
            eventItems.Add(new PreviewItem
            {
                Id = evt.Id,
                Title = evt.Title,
                Subtitle = $"{startStr} - {endStr}",
                ColorHex = "#1565c0",
                IsEvent = true
            });
        }

        // Tasks for this day
        foreach (var task in _tasks.Where(t =>
            t.DtStart.HasValue && t.DtStart.Value.Date == day.Date))
        {
            var color = task.Priority switch
            {
                1 => "#E53935",
                3 => "#43A047",
                _ => "#FFA726"
            };

            string subtitle;
            if (!string.IsNullOrEmpty(task.EstimatedDuration))
            {
                try
                {
                    var ts = XmlConvert.ToTimeSpan(task.EstimatedDuration);
                    subtitle = ts.TotalHours >= 1
                        ? $"预估 {ts.Hours}小时{ts.Minutes}分钟"
                        : $"预估 {ts.TotalMinutes:F0}分钟";
                }
                catch
                {
                    subtitle = string.Empty;
                }
            }
            else
            {
                subtitle = string.Empty;
            }

            taskItems.Add(new PreviewItem
            {
                Id = task.Id,
                Title = task.Title,
                Subtitle = subtitle,
                ColorHex = color,
                IsEvent = false
            });
        }

        SelectedEvents = new ObservableCollection<PreviewItem>(eventItems);
        SelectedTasks = new ObservableCollection<PreviewItem>(taskItems);
        HasEvents = eventItems.Count > 0;
        HasTasks = taskItems.Count > 0;
    }
}

public class CalendarDay : INotifyPropertyChanged
{
    private bool _isSelected;

    public DateTime Date { get; set; }
    public int Day { get; set; }
    public bool IsCurrentMonth { get; set; }
    public bool IsToday { get; set; }
    public bool IsSunday { get; set; }

    public bool IsSelected
    {
        get => _isSelected;
        set
        {
            if (_isSelected != value)
            {
                _isSelected = value;
                OnPropertyChanged();
            }
        }
    }

    public List<string> Dots { get; set; } = new();

    public string DayColorHex
    {
        get
        {
            if (!IsCurrentMonth) return "#CCCCCC";
            if (IsToday) return "#FFFFFF";
            if (IsSunday) return "#E53935";
            return "#666666";
        }
    }

    public event PropertyChangedEventHandler? PropertyChanged;

    protected virtual void OnPropertyChanged([CallerMemberName] string? propertyName = null)
    {
        PropertyChanged?.Invoke(this, new PropertyChangedEventArgs(propertyName));
    }
}

public class PreviewItem
{
    public string Id { get; set; } = string.Empty;
    public string Title { get; set; } = string.Empty;
    public string Subtitle { get; set; } = string.Empty;
    public string ColorHex { get; set; } = "#1565c0";
    public bool IsEvent { get; set; }
}
