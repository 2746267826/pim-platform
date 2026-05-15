using System.Windows;
using System.Windows.Controls;
using Pim.Client.App.ViewModels;

namespace Pim.Client.App.Views;

public partial class TaskListView : UserControl
{
    private TaskListViewModel? _vm;

    public TaskListView()
    {
        InitializeComponent();
        DataContextChanged += (_, e) =>
        {
            _vm = e.NewValue as TaskListViewModel;
            if (_vm is not null)
                _ = _vm.LoadTasksAsync();
        };
    }
}
