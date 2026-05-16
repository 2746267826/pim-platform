using System.Windows.Controls;
using Microsoft.Extensions.DependencyInjection;
using Pim.Client.App.ViewModels;

namespace Pim.Client.App.Views;

public partial class EventEditorDialog : UserControl
{
    public EventEditorDialog()
    {
        InitializeComponent();
        if (App.Services != null)
            DataContext = App.Services.GetRequiredService<EventEditorViewModel>();
    }
}
