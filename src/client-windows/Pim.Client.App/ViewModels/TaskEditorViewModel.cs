using CommunityToolkit.Mvvm.ComponentModel;
using Pim.Client.Core.Services;

namespace Pim.Client.App.ViewModels;

public partial class TaskEditorViewModel : ObservableObject
{
    private readonly ApiClient _api;

    public TaskEditorViewModel(ApiClient api)
    {
        _api = api;
    }
}
