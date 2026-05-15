using System.Collections.ObjectModel;
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using Pim.Client.Core.Models;
using Pim.Client.Core.Services;

namespace Pim.Client.App.ViewModels;

public partial class SearchViewModel : ObservableObject
{
    private readonly ApiClient _api;

    [ObservableProperty] private string _query = string.Empty;
    [ObservableProperty] private string? _typeFilter;
    [ObservableProperty] private ObservableCollection<SearchResult> _results = new();
    [ObservableProperty] private bool _isLoading;
    [ObservableProperty] private string _errorMessage = string.Empty;

    public SearchViewModel(ApiClient api)
    {
        _api = api;
    }

    [RelayCommand]
    private async Task SearchAsync()
    {
        if (string.IsNullOrWhiteSpace(Query)) return;
        IsLoading = true;
        ErrorMessage = string.Empty;
        try
        {
            var typeParam = string.IsNullOrEmpty(TypeFilter) ? "" : $"&type={TypeFilter}";
            var result = await _api.GetAsync<ApiResponse<PagedResult<SearchResult>>>(
                $"/search?q={Uri.EscapeDataString(Query)}{typeParam}&limit=20");
            Results = new ObservableCollection<SearchResult>(result?.Data?.Items ?? new List<SearchResult>());
        }
        catch (Exception ex)
        {
            ErrorMessage = $"搜索失败: {ex.Message}";
        }
        finally
        {
            IsLoading = false;
        }
    }

    [RelayCommand]
    private void FilterByType(string type)
    {
        TypeFilter = TypeFilter == type ? null : type;
    }
}
