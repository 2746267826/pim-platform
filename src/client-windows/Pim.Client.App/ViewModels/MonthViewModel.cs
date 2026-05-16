using CommunityToolkit.Mvvm.ComponentModel;
using Pim.Client.Core.Services;

namespace Pim.Client.App.ViewModels;

public partial class MonthViewModel : ObservableObject
{
    private readonly ApiClient _api;

    public MonthViewModel(ApiClient api)
    {
        _api = api;
    }
}
