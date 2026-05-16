using System.Windows.Controls;
using System.Windows.Threading;
using Microsoft.Extensions.DependencyInjection;
using Pim.Client.App.ViewModels;

namespace Pim.Client.App.Views;

public partial class TimelineView : UserControl
{
    private readonly DispatcherTimer _timer;

    public TimelineView()
    {
        InitializeComponent();

        // Resolve ViewModel from DI
        if (App.Services != null)
        {
            DataContext = App.Services.GetRequiredService<TimelineViewModel>();
        }

        _timer = new DispatcherTimer(TimeSpan.FromSeconds(60), DispatcherPriority.Normal,
            (_, _) => UpdateTimeLine(), Dispatcher);
        Loaded += async (_, _) =>
        {
            if (DataContext is TimelineViewModel vm)
                await vm.LoadAsync(DateTime.Today);
            UpdateTimeLine();
            PopulateTimeLabels();
        };
    }

    private void PopulateTimeLabels()
    {
        var labels = new List<object>();
        for (int h = 0; h < 24; h++)
        {
            labels.Add(new { Label = $"{h:D2}:00", Top = (double)(h * 80) });
        }
        TimeLabels.ItemsSource = labels;
    }

    private void UpdateTimeLine()
    {
        if (DataContext is TimelineViewModel vm)
        {
            var now = DateTime.Now;
            var top = (now.Hour + now.Minute / 60.0) * 80;
            // TimeLine position would be set here if we had the TimeLine element
            TimeScroller.ScrollToVerticalOffset(Math.Max(0, top - 200));
        }
    }
}
