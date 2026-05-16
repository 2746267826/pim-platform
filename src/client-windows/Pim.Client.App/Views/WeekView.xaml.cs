using System.Windows;
using System.Windows.Controls;
using Microsoft.Extensions.DependencyInjection;
using Pim.Client.App.Services;
using Pim.Client.App.ViewModels;

namespace Pim.Client.App.Views;

public partial class WeekView : UserControl
{
    private WeekViewModel? _vm;

    public WeekView()
    {
        InitializeComponent();

        if (App.Services is not null)
        {
            _vm = App.Services.GetRequiredService<WeekViewModel>();
            DataContext = _vm;
        }

        Loaded += OnLoaded;
    }

    private async void OnLoaded(object sender, RoutedEventArgs e)
    {
        if (_vm is not null)
        {
            try
            {
                Logger.Info("WeekView OnLoaded: loading week data");
                await _vm.LoadWeekAsync();
                Logger.Info("WeekView OnLoaded: complete");
            }
            catch (Exception ex)
            {
                Logger.Error("WeekView OnLoaded failed", ex);
            }
        }
    }
}
