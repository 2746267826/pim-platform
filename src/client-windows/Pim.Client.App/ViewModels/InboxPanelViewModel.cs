using CommunityToolkit.Mvvm.ComponentModel;
using Pim.Client.Core.Services;

namespace Pim.Client.App.ViewModels;

public partial class InboxPanelViewModel : ObservableObject
{
    private readonly ApiClient _api;

    public InboxPanelViewModel(ApiClient api)
    {
        _api = api;
    }
}
