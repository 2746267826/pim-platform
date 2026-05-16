using System.Windows.Controls;
using Microsoft.Extensions.DependencyInjection;
using Pim.Client.App.ViewModels;

namespace Pim.Client.App.Views;

public partial class MonthView : UserControl
{
    public MonthView()
    {
        InitializeComponent();
        if (App.Services != null)
            DataContext = App.Services.GetRequiredService<MonthViewModel>();
        Loaded += async (_, _) =>
        {
            if (DataContext is MonthViewModel vm)
                await vm.LoadMonthAsync(new DateTime(DateTime.Today.Year, DateTime.Today.Month, 1));
        };
    }
}
