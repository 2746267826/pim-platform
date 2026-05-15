using System.Windows;
using System.Windows.Controls;
using System.Windows.Input;
using Pim.Client.App.ViewModels;

namespace Pim.Client.App.Views;

public partial class SearchView : UserControl
{
    public SearchView()
    {
        InitializeComponent();
    }

    private void SearchBox_KeyDown(object sender, KeyEventArgs e)
    {
        if (e.Key == Key.Enter && DataContext is SearchViewModel vm)
            vm.SearchCommand.Execute(null);
    }
}
