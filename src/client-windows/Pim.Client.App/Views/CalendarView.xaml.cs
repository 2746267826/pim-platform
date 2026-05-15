using Microsoft.Win32;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Media;
using Pim.Client.App.ViewModels;

namespace Pim.Client.App.Views;

public partial class CalendarView : UserControl
{
    private CalendarViewModel? _vm;

    public CalendarView()
    {
        InitializeComponent();
        DataContextChanged += OnDataContextChanged;
        Loaded += OnLoaded;
    }

    private void OnDataContextChanged(object sender, DependencyPropertyChangedEventArgs e)
    {
        _vm = e.NewValue as CalendarViewModel;
        if (_vm is not null)
        {
            _vm.PropertyChanged += (_, args) =>
            {
                if (args.PropertyName is nameof(CalendarViewModel.Year)
                    or nameof(CalendarViewModel.Month))
                {
                    BuildCalendarCells();
                }
            };
        }
    }

    private async void OnLoaded(object sender, RoutedEventArgs e)
    {
        if (_vm is not null)
        {
            await _vm.LoadEventsAsync();
            BuildCalendarCells();
        }
    }

    private void BuildCalendarCells()
    {
        if (_vm is null) return;
        CalendarGrid.Items.Clear();

        var year = _vm.Year;
        var month = _vm.Month;
        var daysInMonth = _vm.DaysInMonth;
        var firstDow = _vm.FirstDayOfWeek;
        var leadingBlanks = firstDow == DayOfWeek.Sunday ? 6 : (int)firstDow - 1;

        // Leading blanks
        for (int i = 0; i < leadingBlanks; i++)
        {
            CalendarGrid.Items.Add(new Border());
        }

        // Day cells
        for (int day = 1; day <= daysInMonth; day++)
        {
            var border = new Border
            {
                BorderBrush = new SolidColorBrush(Color.FromRgb(0xE0, 0xE0, 0xE0)),
                BorderThickness = new Thickness(0.5),
                Padding = new Thickness(4),
                MinHeight = 40,
                Cursor = System.Windows.Input.Cursors.Hand
            };

            var stack = new StackPanel();
            var dayText = new TextBlock
            {
                Text = day.ToString(),
                FontSize = 12,
                Foreground = new SolidColorBrush(Color.FromRgb(0x66, 0x66, 0x66))
            };
            stack.Children.Add(dayText);

            var dayEvents = _vm.GetEventsForDay(day);
            foreach (var evt in dayEvents.Take(2))
            {
                var dot = new Border
                {
                    Width = 6, Height = 6, CornerRadius = new CornerRadius(3),
                    Background = new SolidColorBrush(Color.FromRgb(0x00, 0x78, 0xD4)),
                    Margin = new Thickness(0, 1, 0, 0)
                };
                stack.Children.Add(dot);
            }

            if (dayEvents.Count > 2)
            {
                stack.Children.Add(new TextBlock
                {
                    Text = $"+{dayEvents.Count - 2}",
                    FontSize = 10,
                    Foreground = new SolidColorBrush(Color.FromRgb(0x00, 0x78, 0xD4))
                });
            }

            border.Child = stack;

            var d = day;
            border.MouseLeftButtonDown += (_, _) =>
                _vm?.OpenCreateEditorCommand.Execute(
                    new DateTime(year, month, d));

            CalendarGrid.Items.Add(border);
        }
    }

    private void SyncOutlookButton_Click(object sender, RoutedEventArgs e)
    {
        _ = _vm?.SyncOutlookAsync();
    }

    private void ImportIcsButton_Click(object sender, RoutedEventArgs e)
    {
        var dialog = new OpenFileDialog
        {
            Filter = "ICS files (*.ics)|*.ics|All files (*.*)|*.*",
            Title = "导入 ICS 文件"
        };
        if (dialog.ShowDialog() == true && _vm is not null)
            _ = _vm.ImportIcsAsync(dialog.FileName);
    }

    private void ExportIcsButton_Click(object sender, RoutedEventArgs e)
    {
        var dialog = new SaveFileDialog
        {
            Filter = "ICS files (*.ics)|*.ics",
            Title = "导出 ICS 文件",
            FileName = $"calendar-{_vm?.Year}-{_vm?.Month:D2}.ics"
        };
        if (dialog.ShowDialog() == true && _vm is not null)
            _ = _vm.ExportIcsAsync(dialog.FileName);
    }
}
