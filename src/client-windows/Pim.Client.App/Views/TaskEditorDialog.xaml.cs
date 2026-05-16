using System.Windows.Controls;
using Microsoft.Extensions.DependencyInjection;
using Pim.Client.App.ViewModels;

namespace Pim.Client.App.Views;

public partial class TaskEditorDialog : UserControl
{
    public TaskEditorDialog()
    {
        InitializeComponent();
        if (App.Services != null)
            DataContext = App.Services.GetRequiredService<TaskEditorViewModel>();
    }
}
