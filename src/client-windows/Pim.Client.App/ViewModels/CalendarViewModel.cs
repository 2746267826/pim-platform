using System.Collections.ObjectModel;
using System.IO;
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using Pim.Client.Core.Models;
using Pim.Client.Core.Services;

namespace Pim.Client.App.ViewModels;

public partial class CalendarViewModel : ObservableObject
{
    private readonly ApiClient _api;

    [ObservableProperty] private DateTime _currentMonth = DateTime.Now;
    [ObservableProperty] private ObservableCollection<EventResponse> _events = new();
    [ObservableProperty] private EventResponse? _selectedEvent;
    [ObservableProperty] private bool _isLoading;
    [ObservableProperty] private bool _isEditorOpen;
    [ObservableProperty] private string _editorTitle = string.Empty;
    [ObservableProperty] private string? _editorDescription;
    [ObservableProperty] private string? _editorLocation;
    [ObservableProperty] private DateTime _editorStart = DateTime.Now;
    [ObservableProperty] private DateTime _editorEnd = DateTime.Now.AddHours(1);
    [ObservableProperty] private string _errorMessage = string.Empty;

    public CalendarViewModel(ApiClient api)
    {
        _api = api;
    }

    public int Year => CurrentMonth.Year;
    public int Month => CurrentMonth.Month;
    public int DaysInMonth => DateTime.DaysInMonth(Year, Month);
    public DayOfWeek FirstDayOfWeek => new DateTime(Year, Month, 1).DayOfWeek;
    public int LeadingBlanks => (int)FirstDayOfWeek;

    public async Task LoadEventsAsync()
    {
        IsLoading = true;
        ErrorMessage = string.Empty;
        try
        {
            var start = new DateTimeOffset(Year, Month, 1, 0, 0, 0, TimeSpan.Zero);
            var end = start.AddMonths(1);
            var result = await _api.GetAsync<ApiResponse<List<EventResponse>>>(
                $"/calendar/events?start={start:yyyy-MM-dd}&end={end:yyyy-MM-dd}");
            Events = new ObservableCollection<EventResponse>(result?.Data ?? new List<EventResponse>());
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
    private void PreviousMonth()
    {
        CurrentMonth = CurrentMonth.AddMonths(-1);
        OnPropertyChanged(nameof(Year));
        OnPropertyChanged(nameof(Month));
        OnPropertyChanged(nameof(DaysInMonth));
        OnPropertyChanged(nameof(FirstDayOfWeek));
        OnPropertyChanged(nameof(LeadingBlanks));
    }

    [RelayCommand]
    private void NextMonth()
    {
        CurrentMonth = CurrentMonth.AddMonths(1);
        OnPropertyChanged(nameof(Year));
        OnPropertyChanged(nameof(Month));
        OnPropertyChanged(nameof(DaysInMonth));
        OnPropertyChanged(nameof(FirstDayOfWeek));
        OnPropertyChanged(nameof(LeadingBlanks));
    }

    [RelayCommand]
    private void OpenCreateEditor(DateTime? date = null)
    {
        SelectedEvent = null;
        EditorTitle = string.Empty;
        EditorDescription = null;
        EditorLocation = null;
        EditorStart = date ?? DateTime.Now;
        EditorEnd = EditorStart.AddHours(1);
        IsEditorOpen = true;
    }

    [RelayCommand]
    private void OpenEditEditor(EventResponse evt)
    {
        SelectedEvent = evt;
        EditorTitle = evt.Title;
        EditorDescription = evt.Description;
        EditorLocation = evt.Location;
        EditorStart = evt.DtStart.DateTime;
        EditorEnd = evt.DtEnd.DateTime;
        IsEditorOpen = true;
    }

    [RelayCommand]
    private void CloseEditor()
    {
        IsEditorOpen = false;
    }

    [RelayCommand]
    private async Task SaveEventAsync()
    {
        if (string.IsNullOrWhiteSpace(EditorTitle)) return;
        IsLoading = true;
        try
        {
            var body = new
            {
                calendarId = "", // will use default
                title = EditorTitle,
                description = EditorDescription,
                location = EditorLocation,
                dtStart = new DateTimeOffset(EditorStart),
                dtEnd = new DateTimeOffset(EditorEnd)
            };

            if (SelectedEvent is not null)
            {
                await _api.PutAsync<object>($"/calendar/events/{SelectedEvent.Id}", body);
            }
            else
            {
                // For now, get first calendar
                var cals = await _api.GetAsync<ApiResponse<List<CalendarResponse>>>("/calendar/calendars");
                var calId = cals?.Data?.FirstOrDefault()?.Id;
                if (calId is null)
                {
                    ErrorMessage = "请先创建日历";
                    return;
                }
                await _api.PostAsync<object>("/calendar/events",
                    new { calendarId = calId, title = EditorTitle, description = EditorDescription, location = EditorLocation, dtStart = new DateTimeOffset(EditorStart), dtEnd = new DateTimeOffset(EditorEnd) });
            }

            IsEditorOpen = false;
            await LoadEventsAsync();
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
    private async Task DeleteEventAsync(EventResponse evt)
    {
        try
        {
            await _api.DeleteAsync($"/calendar/events/{evt.Id}");
            await LoadEventsAsync();
        }
        catch (Exception ex)
        {
            ErrorMessage = $"删除失败: {ex.Message}";
        }
    }

    public async Task SyncOutlookAsync()
    {
        IsLoading = true;
        ErrorMessage = string.Empty;
        try
        {
            await _api.PostAsync<object>("/calendar/outlook/sync", new { });
            await LoadEventsAsync();
        }
        catch (Exception ex)
        {
            ErrorMessage = $"同步失败: {ex.Message}";
        }
        finally
        {
            IsLoading = false;
        }
    }

    public async Task ImportIcsAsync(string filePath)
    {
        IsLoading = true;
        ErrorMessage = string.Empty;
        try
        {
            var icsContent = await File.ReadAllTextAsync(filePath);
            await _api.PostStringAsync<object>("/calendar/import-ics", icsContent);
            await LoadEventsAsync();
        }
        catch (Exception ex)
        {
            ErrorMessage = $"导入失败: {ex.Message}";
        }
        finally
        {
            IsLoading = false;
        }
    }

    public async Task ExportIcsAsync(string filePath)
    {
        IsLoading = true;
        ErrorMessage = string.Empty;
        try
        {
            var start = new DateTimeOffset(Year, Month, 1, 0, 0, 0, TimeSpan.Zero);
            var end = start.AddMonths(1);
            var result = await _api.GetAsync<ApiResponse<string>>(
                $"/calendar/export-ics?start={start:yyyy-MM-dd}&end={end:yyyy-MM-dd}");
            if (result?.Data is not null)
                await File.WriteAllTextAsync(filePath, result.Data);
        }
        catch (Exception ex)
        {
            ErrorMessage = $"导出失败: {ex.Message}";
        }
        finally
        {
            IsLoading = false;
        }
    }

    public List<EventResponse> GetEventsForDay(int day)
    {
        var date = new DateTime(Year, Month, day);
        return Events.Where(e =>
            e.DtStart.Date <= date && e.DtEnd.Date >= date).ToList();
    }
}
