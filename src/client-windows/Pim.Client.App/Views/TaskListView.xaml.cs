using System.Windows.Controls;
using Microsoft.Extensions.DependencyInjection;
using Pim.Client.App.ViewModels;

namespace Pim.Client.App.Views;

public partial class TaskListView : UserControl
{
    public TaskListView()
    {
        InitializeComponent();
        if (App.Services != null)
            DataContext = App.Services.GetRequiredService<TaskListViewModel>();
        Loaded += async (_, _) =>
        {
            if (DataContext is TaskListViewModel vm)
                await vm.LoadTasksAsync();
        };
    }
}
