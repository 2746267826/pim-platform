using System.Windows.Controls;
using Microsoft.Extensions.DependencyInjection;
using Pim.Client.App.ViewModels;

namespace Pim.Client.App.Views;

public partial class InboxPanel : UserControl
{
    public InboxPanel()
    {
        InitializeComponent();
        if (App.Services != null)
            DataContext = App.Services.GetRequiredService<InboxPanelViewModel>();
        Loaded += async (_, _) =>
        {
            if (DataContext is InboxPanelViewModel vm)
                await vm.LoadAsync();
        };
    }
}
