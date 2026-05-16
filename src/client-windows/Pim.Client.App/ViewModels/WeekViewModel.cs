using CommunityToolkit.Mvvm.ComponentModel;
using Pim.Client.Core.Services;

namespace Pim.Client.App.ViewModels;

public partial class WeekViewModel : ObservableObject
{
    private readonly ApiClient _api;

    public WeekViewModel(ApiClient api)
    {
        _api = api;
    }
}
