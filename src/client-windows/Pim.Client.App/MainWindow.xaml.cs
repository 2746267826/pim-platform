using System.Windows;
using Microsoft.Extensions.DependencyInjection;
using Pim.Client.App.ViewModels;
using Pim.Client.App.Views;

namespace Pim.Client.App;

public partial class MainWindow : Window
{
    private readonly MainViewModel _vm;
    private readonly IServiceProvider _services;

    public MainWindow(MainViewModel vm, IServiceProvider services)
    {
        InitializeComponent();
        _vm = vm;
        _services = services;
        DataContext = vm;

        CalendarViewPanel.DataContext = _services.GetRequiredService<CalendarViewModel>();
        TaskListViewPanel.DataContext = _services.GetRequiredService<TaskListViewModel>();
        SearchViewPanel.DataContext = _services.GetRequiredService<SearchViewModel>();

        vm.LoggedOut += OnLoggedOut;
        vm.NavigateCommand.Execute("calendar");
    }

    private void OnLoggedOut()
    {
        _vm.LoggedOut -= OnLoggedOut;
        var loginWindow = new LoginWindow(
            _services.GetRequiredService<LoginViewModel>());
        loginWindow.Show();
        Close();
    }
}
