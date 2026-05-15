using System.Windows;
using System.Windows.Controls;
using Pim.Client.App.ViewModels;

namespace Pim.Client.App.Views;

public partial class LoginWindow : Window
{
    private readonly LoginViewModel _vm;

    public LoginWindow(LoginViewModel vm)
    {
        InitializeComponent();
        _vm = vm;
        DataContext = vm;
        vm.LoginSucceeded += OnLoginSucceeded;
        vm.PropertyChanged += (_, e) =>
        {
            if (e.PropertyName == nameof(LoginViewModel.IsRegisterMode))
            {
                PasswordBox.Password = string.Empty;
                vm.Password = string.Empty;
            }
        };
    }

    private void OnLoginSucceeded()
    {
        _vm.LoginSucceeded -= OnLoginSucceeded;
        Close();
    }

    private void PasswordBox_PasswordChanged(object sender, RoutedEventArgs e)
    {
        _vm.Password = PasswordBox.Password;
    }
}
