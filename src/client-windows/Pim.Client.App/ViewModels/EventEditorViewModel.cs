using System.Collections.ObjectModel;
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using Pim.Client.App.Services;
using Pim.Client.Core.Models;
using Pim.Client.Core.Services;

namespace Pim.Client.App.ViewModels;

public partial class EventEditorViewModel : ObservableObject
{
    private readonly ApiClient _api;
    private string? _eventId;

    [ObservableProperty] private string _title = string.Empty;
    [ObservableProperty] private bool _isAllDay;
    [ObservableProperty] private DateTime _startDate = DateTime.Now;
    [ObservableProperty] private string _startTime = "09:00";
    [ObservableProperty] private DateTime _endDate = DateTime.Now;
    [ObservableProperty] private string _endTime = "10:00";
    [ObservableProperty] private string? _location;
    [ObservableProperty] private string? _description;
    [ObservableProperty] private string _colorHex = "#6B5EE4";
    [ObservableProperty] private string _status = "CONFIRMED";
    [ObservableProperty] private string? _rrule;
    [ObservableProperty] private bool _isBlock;
    [ObservableProperty] private string? _selectedCalendarId;
    [ObservableProperty] private string _dialogTitle = "新建日程";
    [ObservableProperty] private bool _isEditing;
    [ObservableProperty] private bool _isSaving;
    [ObservableProperty] private string _errorMessage = string.Empty;

    public ObservableCollection<CalendarResponse> Calendars { get; } = new();

    public List<string> ColorPalette { get; } =
    [
        "#6B5EE4", "#0EA8A0", "#E91E63", "#FF9800",
        "#2196F3", "#4CAF50", "#E53935"
    ];

    public List<OptionItem> StatusOptions { get; } =
    [
        new("已确认", "CONFIRMED"),
        new("暂定", "TENTATIVE"),
        new("已取消", "CANCELLED"),
    ];

    public List<OptionItem> RepeatOptions { get; } =
    [
        new("不重复", null),
        new("每天", "FREQ=DAILY"),
        new("每周", "FREQ=WEEKLY"),
        new("每月", "FREQ=MONTHLY"),
        new("每年", "FREQ=YEARLY"),
    ];

    public event Action<EventResponse?>? Saved;

    public CalendarResponse? SelectedCalendar
    {
        get => Calendars.FirstOrDefault(c => c.Id == SelectedCalendarId);
        set
        {
            if (value is not null)
            {
                SelectedCalendarId = value.Id;
                ColorHex = value.Color;
            }
            OnPropertyChanged();
        }
    }

    public OptionItem? SelectedStatusOption
    {
        get => StatusOptions.FirstOrDefault(o => o.Value == Status) ?? StatusOptions[0];
        set
        {
            if (value is not null)
                Status = value.Value ?? string.Empty;
            OnPropertyChanged();
        }
    }

    public OptionItem? SelectedRepeatOption
    {
        get => RepeatOptions.FirstOrDefault(o => o.Value == Rrule);
        set
        {
            if (value is not null)
                Rrule = value.Value;
            OnPropertyChanged();
        }
    }

    partial void OnStatusChanged(string value) => OnPropertyChanged(nameof(SelectedStatusOption));
    partial void OnRruleChanged(string? value) => OnPropertyChanged(nameof(SelectedRepeatOption));
    partial void OnSelectedCalendarIdChanged(string? value) => OnPropertyChanged(nameof(SelectedCalendar));

    partial void OnIsAllDayChanged(bool value)
    {
        if (value)
        {
            StartTime = "00:00";
            EndTime = "00:00";
        }
    }

    public EventEditorViewModel(ApiClient api)
    {
        _api = api;
    }

    public async Task LoadCalendarsAsync()
    {
        try
        {
            var result = await _api.GetAsync<ApiResponse<List<CalendarResponse>>>("/calendar/calendars");
            Calendars.Clear();
            if (result?.Data is not null)
            {
                foreach (var cal in result.Data)
                    Calendars.Add(cal);

                var defaultCal = result.Data.FirstOrDefault(c => c.IsDefault) ?? result.Data.FirstOrDefault();
                if (defaultCal is not null)
                {
                    SelectedCalendarId = defaultCal.Id;
                    ColorHex = defaultCal.Color;
                }
            }
        }
        catch (Exception ex)
        {
            ErrorMessage = $"加载日历失败: {ex.Message}";
            Logger.Error("EventEditorViewModel LoadCalendars failed", ex);
        }
    }

    public void LoadEvent(EventResponse evt)
    {
        _eventId = evt.Id;
        IsEditing = true;
        DialogTitle = "编辑日程";
        Title = evt.Title;
        Description = evt.Description;
        Location = evt.Location;
        StartDate = evt.DtStart.Date;
        StartTime = evt.DtStart.ToString("HH:mm");
        EndDate = evt.DtEnd.Date;
        EndTime = evt.DtEnd.ToString("HH:mm");
        SelectedCalendarId = evt.CalendarId;
        Status = string.IsNullOrEmpty(evt.Status) ? "CONFIRMED" : evt.Status;
        Rrule = evt.RRule;
        IsAllDay = evt.DtStart.TimeOfDay == TimeSpan.Zero && evt.DtEnd.TimeOfDay == TimeSpan.Zero;

        var cal = Calendars.FirstOrDefault(c => c.Id == evt.CalendarId);
        if (cal is not null)
            ColorHex = cal.Color;
    }

    [RelayCommand]
    private void SelectColor(string? color)
    {
        if (!string.IsNullOrEmpty(color))
            ColorHex = color;
    }

    [RelayCommand]
    private async Task SaveAsync()
    {
        ErrorMessage = string.Empty;

        if (string.IsNullOrWhiteSpace(Title))
        {
            ErrorMessage = "请输入标题";
            return;
        }

        if (string.IsNullOrWhiteSpace(SelectedCalendarId))
        {
            ErrorMessage = "请选择日历";
            return;
        }

        if (!TimeSpan.TryParse(StartTime, out var startTs))
        {
            ErrorMessage = "开始时间格式无效，请使用 HH:mm 格式";
            return;
        }

        if (!TimeSpan.TryParse(EndTime, out var endTs))
        {
            ErrorMessage = "结束时间格式无效，请使用 HH:mm 格式";
            return;
        }

        var start = new DateTimeOffset(StartDate.Date + startTs);
        var end = new DateTimeOffset(EndDate.Date + endTs);

        if (end <= start)
        {
            ErrorMessage = "结束时间必须晚于开始时间";
            return;
        }

        IsSaving = true;
        try
        {
            var body = new
            {
                calendarId = SelectedCalendarId,
                title = Title.Trim(),
                description = string.IsNullOrWhiteSpace(Description) ? null : Description,
                location = string.IsNullOrWhiteSpace(Location) ? null : Location,
                dtStart = start,
                dtEnd = end,
                rrule = Rrule,
                status = Status,
                isAllDay = IsAllDay,
                isBlock = IsBlock
            };

            EventResponse? savedEvent = null;

            if (IsEditing && _eventId is not null)
            {
                var response = await _api.PutAsync<ApiResponse<EventResponse>>($"/calendar/events/{_eventId}", body);
                savedEvent = response?.Data;
            }
            else
            {
                var response = await _api.PostAsync<ApiResponse<EventResponse>>("/calendar/events", body);
                savedEvent = response?.Data;
            }

            Saved?.Invoke(savedEvent);
            ErrorMessage = string.Empty;
        }
        catch (Exception ex)
        {
            ErrorMessage = $"保存失败: {ex.Message}";
            Logger.Error("EventEditorViewModel Save failed", ex);
        }
        finally
        {
            IsSaving = false;
        }
    }
}

public record OptionItem(string Display, string? Value);
