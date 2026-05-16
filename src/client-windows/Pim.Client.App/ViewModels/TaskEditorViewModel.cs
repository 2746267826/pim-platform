using System.Collections.ObjectModel;
using System.Xml;
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using Pim.Client.App.Services;
using Pim.Client.Core.Models;
using Pim.Client.Core.Services;

namespace Pim.Client.App.ViewModels;

public partial class TaskEditorViewModel : ObservableObject
{
    private readonly ApiClient _api;
    private string? _taskId;

    [ObservableProperty] private string _title = string.Empty;
    [ObservableProperty] private string? _description;
    [ObservableProperty] private string? _location;
    [ObservableProperty] private int _durationMinutes = 60;
    [ObservableProperty] private DateTime? _dueDate;
    [ObservableProperty] private string? _dueTime;
    [ObservableProperty] private int _priority = 2;
    [ObservableProperty] private string? _rrule;
    [ObservableProperty] private int _reminderMinutes = 15;
    [ObservableProperty] private bool _isAutoScheduled = true;
    [ObservableProperty] private bool _isSplittable;
    [ObservableProperty] private bool _isLocked;
    [ObservableProperty] private string? _selectedTaskListId;
    [ObservableProperty] private string _dialogTitle = "新建任务";
    [ObservableProperty] private bool _isEditing;
    [ObservableProperty] private bool _isSaving;
    [ObservableProperty] private string _errorMessage = string.Empty;

    public ObservableCollection<TaskListDisplay> TaskLists { get; } = new();

    public List<int> DurationOptions { get; } = [15, 30, 60, 90, 120, 180, 240, 360, 480];

    public List<PriorityOption> PriorityOptions { get; } =
    [
        new PriorityOption("高", 1, "#E53935"),
        new PriorityOption("中", 2, "#FFA726"),
        new PriorityOption("低", 3, "#43A047"),
    ];

    public List<OptionItem> RepeatOptions { get; } =
    [
        new OptionItem("不重复", null),
        new OptionItem("每天", "FREQ=DAILY"),
        new OptionItem("每周", "FREQ=WEEKLY"),
        new OptionItem("每月", "FREQ=MONTHLY"),
        new OptionItem("每年", "FREQ=YEARLY"),
    ];

    public List<ReminderOption> ReminderOptions { get; } =
    [
        new ReminderOption("准时", 0),
        new ReminderOption("5分钟前", 5),
        new ReminderOption("15分钟前", 15),
        new ReminderOption("30分钟前", 30),
        new ReminderOption("1小时前", 60),
        new ReminderOption("1天前", 1440),
    ];

    public event Action<TaskResponse?>? Saved;

    public TaskListDisplay? SelectedTaskList
    {
        get => TaskLists.FirstOrDefault(t => t.Id == SelectedTaskListId);
        set
        {
            if (value is not null)
                SelectedTaskListId = value.Id;
            OnPropertyChanged();
        }
    }

    public PriorityOption? SelectedPriorityOption
    {
        get => PriorityOptions.FirstOrDefault(o => o.Value == Priority);
        set
        {
            if (value is not null)
                Priority = value.Value;
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

    public ReminderOption? SelectedReminderOption
    {
        get => ReminderOptions.FirstOrDefault(o => o.Value == ReminderMinutes);
        set
        {
            if (value is not null)
                ReminderMinutes = value.Value;
            OnPropertyChanged();
        }
    }

    partial void OnPriorityChanged(int value) => OnPropertyChanged(nameof(SelectedPriorityOption));
    partial void OnRruleChanged(string? value) => OnPropertyChanged(nameof(SelectedRepeatOption));
    partial void OnReminderMinutesChanged(int value) => OnPropertyChanged(nameof(SelectedReminderOption));
    partial void OnSelectedTaskListIdChanged(string? value) => OnPropertyChanged(nameof(SelectedTaskList));

    public TaskEditorViewModel(ApiClient api)
    {
        _api = api;
    }

    public async Task LoadTaskListsAsync()
    {
        try
        {
            var result = await _api.GetAsync<ApiResponse<List<TaskListDisplay>>>("/calendar/task-lists");
            TaskLists.Clear();
            if (result?.Data is not null)
            {
                foreach (var tl in result.Data)
                    TaskLists.Add(tl);
            }
        }
        catch (Exception ex)
        {
            ErrorMessage = $"加载任务列表失败: {ex.Message}";
            Logger.Error("TaskEditorViewModel LoadTaskListsAsync failed", ex);
        }
    }

    public void LoadTask(TaskResponse task)
    {
        _taskId = task.Id;
        IsEditing = true;
        DialogTitle = "编辑任务";
        Title = task.Title;
        Description = task.Description;
        Priority = task.Priority is >= 1 and <= 3 ? task.Priority : 2;
        SelectedTaskListId = task.CalendarId;
        DurationMinutes = Iso8601ToMinutes(task.EstimatedDuration);
        DueDate = task.Due?.Date;
        DueTime = task.Due?.ToString("HH:mm");
    }

    [RelayCommand]
    private void SelectDuration(int minutes)
    {
        DurationMinutes = minutes;
    }

    [RelayCommand]
    private void SelectPriority(int value)
    {
        Priority = value;
    }

    [RelayCommand]
    private void SelectRepeat(string? rrule)
    {
        Rrule = rrule;
    }

    [RelayCommand]
    private void SelectReminder(int minutes)
    {
        ReminderMinutes = minutes;
    }

    [RelayCommand]
    private void ClearDueDate()
    {
        DueDate = null;
        DueTime = null;
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

        if (string.IsNullOrWhiteSpace(SelectedTaskListId))
        {
            ErrorMessage = "请选择任务列表";
            return;
        }

        IsSaving = true;
        try
        {
            DateTimeOffset? due = null;
            if (DueDate.HasValue)
            {
                if (!string.IsNullOrWhiteSpace(DueTime) && TimeSpan.TryParse(DueTime, out var dueTs))
                    due = new DateTimeOffset(DueDate.Value.Date + dueTs);
                else
                    due = new DateTimeOffset(DueDate.Value.Date);
            }

            var estimatedDuration = MinutesToIso8601(DurationMinutes);

            var body = new
            {
                calendarId = SelectedTaskListId,
                title = Title.Trim(),
                description = string.IsNullOrWhiteSpace(Description) ? null : Description,
                location = string.IsNullOrWhiteSpace(Location) ? null : Location,
                priority = Priority,
                estimatedDuration,
                due,
                rrule = Rrule,
                reminderMinutes = ReminderMinutes,
                isAutoScheduled = IsAutoScheduled,
                isSplittable = IsSplittable,
                isLocked = IsLocked
            };

            TaskResponse? savedTask = null;

            if (IsEditing && _taskId is not null)
            {
                var response = await _api.PutAsync<ApiResponse<TaskResponse>>($"/calendar/tasks/{_taskId}", body);
                savedTask = response?.Data;
            }
            else
            {
                var response = await _api.PostAsync<ApiResponse<TaskResponse>>("/calendar/tasks", body);
                savedTask = response?.Data;
            }

            Saved?.Invoke(savedTask);
            ErrorMessage = string.Empty;
        }
        catch (Exception ex)
        {
            ErrorMessage = $"保存失败: {ex.Message}";
            Logger.Error("TaskEditorViewModel Save failed", ex);
        }
        finally
        {
            IsSaving = false;
        }
    }

    private static string MinutesToIso8601(int minutes)
    {
        if (minutes <= 0) return "PT0M";
        if (minutes < 60) return $"PT{minutes}M";
        var h = minutes / 60;
        var m = minutes % 60;
        return m == 0 ? $"PT{h}H" : $"PT{h}H{m}M";
    }

    private static int Iso8601ToMinutes(string? duration)
    {
        if (string.IsNullOrEmpty(duration)) return 60;
        try
        {
            var ts = XmlConvert.ToTimeSpan(duration);
            return (int)ts.TotalMinutes;
        }
        catch
        {
            return 60;
        }
    }
}

public class PriorityOption
{
    public string Display { get; }
    public int Value { get; }
    public string Color { get; }

    public PriorityOption(string display, int value, string color)
    {
        Display = display;
        Value = value;
        Color = color;
    }
}

public class ReminderOption
{
    public string Display { get; }
    public int Value { get; }

    public ReminderOption(string display, int value)
    {
        Display = display;
        Value = value;
    }
}
