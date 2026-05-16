using CommunityToolkit.Mvvm.ComponentModel;
using Pim.Client.Core.Services;

namespace Pim.Client.App.ViewModels;

public partial class TimelineViewModel : ObservableObject
{
    private readonly ApiClient _api;

    public TimelineViewModel(ApiClient api)
    {
        _api = api;
    }
}
