# WPF 日历窗口 UI 重新设计 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 基于 Flutter FlowPlanV2 UI 参考，使用 MaterialDesignThemes 重新设计 WPF 客户端日历窗口

**Architecture:** MaterialDesign 主题 + MVVM (CommunityToolkit.Mvvm) + Shell 布局（左侧导航 200px + 中间 ContentControl 动态切换 4 视图 + 右侧收件箱面板 280px）

**Tech Stack:** WPF .NET 8 + MaterialDesignThemes 5.x + CommunityToolkit.Mvvm 8.4.0 + System.Text.Json

---

### Task 1: 添加 MaterialDesignThemes NuGet 包

**Files:**
- Modify: `src/client-windows/Pim.Client.App/Pim.Client.App.csproj`

- [ ] **Step 1: 添加 NuGet 包引用**

```bash
dotnet add src/client-windows/Pim.Client.App/Pim.Client.App.csproj package MaterialDesignThemes --version 5.2.0
```

- [ ] **Step 2: 验证包安装**

```bash
dotnet restore src/client-windows/Pim.Client.App/Pim.Client.App.csproj
```

- [ ] **Step 3: Commit**

```bash
git add src/client-windows/Pim.Client.App/Pim.Client.App.csproj
git commit -m "chore: add MaterialDesignThemes NuGet package

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

### Task 2: 更新 App.xaml 集成 MaterialDesign 主题

**Files:**
- Modify: `src/client-windows/Pim.Client.App/App.xaml`
- Modify: `src/client-windows/Pim.Client.App/Styles/Theme.xaml`

- [ ] **Step 1: 更新 Theme.xaml 颜色体系和 MaterialDesign 兼容样式**

将 `src/client-windows/Pim.Client.App/Styles/Theme.xaml` 替换为：

```xml
<ResourceDictionary xmlns="http://schemas.microsoft.com/winfx/2006/xaml/presentation"
                    xmlns:x="http://schemas.microsoft.com/winfx/2006/xaml">

    <!-- Brand Colors (matching Flutter reference) -->
    <Color x:Key="PrimaryColor">#1565c0</Color>
    <Color x:Key="PrimaryLightColor">#E3F0FD</Color>
    <Color x:Key="BgColor">#F5F5F5</Color>
    <Color x:Key="CardColor">#FFFFFF</Color>
    <Color x:Key="TextPrimaryColor">#1A1A1A</Color>
    <Color x:Key="TextSecondaryColor">#666666</Color>
    <Color x:Key="BorderColor">#E0E0E0</Color>
    <Color x:Key="NavBgColor">#F8F9FA</Color>
    <Color x:Key="DangerColor">#E53935</Color>
    <Color x:Key="WarningColor">#FFA726</Color>
    <Color x:Key="SuccessColor">#43A047</Color>

    <!-- Priority Colors -->
    <Color x:Key="PriorityHighColor">#E53935</Color>
    <Color x:Key="PriorityMediumColor">#FFA726</Color>
    <Color x:Key="PriorityLowColor">#43A047</Color>

    <!-- Calendar Book Default Colors -->
    <Color x:Key="CalendarPurple">#6B5EE4</Color>
    <Color x:Key="CalendarTeal">#0EA8A0</Color>
    <Color x:Key="CalendarPink">#E91E63</Color>
    <Color x:Key="CalendarOrange">#FF9800</Color>
    <Color x:Key="CalendarBlue">#2196F3</Color>
    <Color x:Key="CalendarGreen">#4CAF50</Color>
    <Color x:Key="CalendarRed">#E53935</Color>

    <SolidColorBrush x:Key="PrimaryBrush" Color="{StaticResource PrimaryColor}"/>
    <SolidColorBrush x:Key="PrimaryLightBrush" Color="{StaticResource PrimaryLightColor}"/>
    <SolidColorBrush x:Key="BgBrush" Color="{StaticResource BgColor}"/>
    <SolidColorBrush x:Key="CardBrush" Color="{StaticResource CardColor}"/>
    <SolidColorBrush x:Key="TextPrimaryBrush" Color="{StaticResource TextPrimaryColor}"/>
    <SolidColorBrush x:Key="TextSecondaryBrush" Color="{StaticResource TextSecondaryColor}"/>
    <SolidColorBrush x:Key="BorderBrush" Color="{StaticResource BorderColor}"/>
    <SolidColorBrush x:Key="NavBgBrush" Color="{StaticResource NavBgColor}"/>
    <SolidColorBrush x:Key="DangerBrush" Color="{StaticResource DangerColor}"/>
    <SolidColorBrush x:Key="WarningBrush" Color="{StaticResource WarningColor}"/>
    <SolidColorBrush x:Key="SuccessBrush" Color="{StaticResource SuccessColor}"/>

    <SolidColorBrush x:Key="PriorityHighBrush" Color="{StaticResource PriorityHighColor}"/>
    <SolidColorBrush x:Key="PriorityMediumBrush" Color="{StaticResource PriorityMediumColor}"/>
    <SolidColorBrush x:Key="PriorityLowBrush" Color="{StaticResource PriorityLowColor}"/>

    <!-- Primary Button (Material style) -->
    <Style x:Key="PrimaryButton" TargetType="Button">
        <Setter Property="Background" Value="{StaticResource PrimaryBrush}"/>
        <Setter Property="Foreground" Value="White"/>
        <Setter Property="BorderThickness" Value="0"/>
        <Setter Property="Padding" Value="20,10"/>
        <Setter Property="FontSize" Value="14"/>
        <Setter Property="FontWeight" Value="SemiBold"/>
        <Setter Property="Cursor" Value="Hand"/>
        <Setter Property="Template">
            <Setter.Value>
                <ControlTemplate TargetType="Button">
                    <Border Background="{TemplateBinding Background}" CornerRadius="8"
                            Padding="{TemplateBinding Padding}"
                            RenderOptions.CachingHint="Cache">
                        <ContentPresenter HorizontalAlignment="Center" VerticalAlignment="Center"/>
                    </Border>
                </ControlTemplate>
            </Setter.Value>
        </Setter>
        <Style.Triggers>
            <Trigger Property="IsMouseOver" Value="True">
                <Setter Property="Background" Value="#0D47A1"/>
            </Trigger>
            <Trigger Property="IsEnabled" Value="False">
                <Setter Property="Opacity" Value="0.5"/>
            </Trigger>
        </Style.Triggers>
    </Style>

    <!-- Secondary Button -->
    <Style x:Key="SecondaryButton" TargetType="Button">
        <Setter Property="Background" Value="#E8E8E8"/>
        <Setter Property="Foreground" Value="{StaticResource TextPrimaryBrush}"/>
        <Setter Property="BorderThickness" Value="0"/>
        <Setter Property="Padding" Value="20,10"/>
        <Setter Property="FontSize" Value="14"/>
        <Setter Property="Cursor" Value="Hand"/>
        <Setter Property="Template">
            <Setter.Value>
                <ControlTemplate TargetType="Button">
                    <Border Background="{TemplateBinding Background}" CornerRadius="8"
                            Padding="{TemplateBinding Padding}">
                        <ContentPresenter HorizontalAlignment="Center" VerticalAlignment="Center"/>
                    </Border>
                </ControlTemplate>
            </Setter.Value>
        </Setter>
        <Style.Triggers>
            <Trigger Property="IsMouseOver" Value="True">
                <Setter Property="Background" Value="#D0D0D0"/>
            </Trigger>
        </Style.Triggers>
    </Style>

    <!-- Nav Button Style -->
    <Style x:Key="NavButton" TargetType="Button">
        <Setter Property="Background" Value="Transparent"/>
        <Setter Property="Foreground" Value="#555555"/>
        <Setter Property="BorderThickness" Value="0"/>
        <Setter Property="Padding" Value="20,10"/>
        <Setter Property="FontSize" Value="14"/>
        <Setter Property="HorizontalContentAlignment" Value="Left"/>
        <Setter Property="Cursor" Value="Hand"/>
        <Setter Property="Template">
            <Setter.Value>
                <ControlTemplate TargetType="Button">
                    <Border Background="{TemplateBinding Background}" CornerRadius="8"
                            Padding="{TemplateBinding Padding}">
                        <ContentPresenter HorizontalAlignment="{TemplateBinding HorizontalContentAlignment}"
                                          VerticalAlignment="Center"/>
                    </Border>
                </ControlTemplate>
            </Setter.Value>
        </Setter>
        <Style.Triggers>
            <Trigger Property="IsMouseOver" Value="True">
                <Setter Property="Background" Value="#E8E8E8"/>
            </Trigger>
        </Style.Triggers>
    </Style>

    <Style x:Key="ActiveNavButton" TargetType="Button" BasedOn="{StaticResource NavButton}">
        <Setter Property="Background" Value="{StaticResource PrimaryLightBrush}"/>
        <Setter Property="Foreground" Value="{StaticResource PrimaryBrush}"/>
        <Setter Property="FontWeight" Value="SemiBold"/>
    </Style>

    <!-- Modern TextBox -->
    <Style x:Key="ModernTextBox" TargetType="TextBox">
        <Setter Property="Background" Value="{StaticResource CardBrush}"/>
        <Setter Property="BorderBrush" Value="{StaticResource BorderBrush}"/>
        <Setter Property="BorderThickness" Value="1"/>
        <Setter Property="Padding" Value="12,10"/>
        <Setter Property="FontSize" Value="14"/>
        <Setter Property="Template">
            <Setter.Value>
                <ControlTemplate TargetType="TextBox">
                    <Border Background="{TemplateBinding Background}"
                            BorderBrush="{TemplateBinding BorderBrush}"
                            BorderThickness="{TemplateBinding BorderThickness}"
                            CornerRadius="8">
                        <ScrollViewer x:Name="PART_ContentHost" Margin="{TemplateBinding Padding}"/>
                    </Border>
                </ControlTemplate>
            </Setter.Value>
        </Setter>
    </Style>

    <Style x:Key="ModernPasswordBox" TargetType="PasswordBox">
        <Setter Property="Background" Value="{StaticResource CardBrush}"/>
        <Setter Property="BorderBrush" Value="{StaticResource BorderBrush}"/>
        <Setter Property="BorderThickness" Value="1"/>
        <Setter Property="Padding" Value="12,10"/>
        <Setter Property="FontSize" Value="14"/>
    </Style>

    <!-- Card Style -->
    <Style x:Key="Card" TargetType="Border">
        <Setter Property="Background" Value="{StaticResource CardBrush}"/>
        <Setter Property="BorderBrush" Value="{StaticResource BorderBrush}"/>
        <Setter Property="BorderThickness" Value="1"/>
        <Setter Property="CornerRadius" Value="8"/>
        <Setter Property="Padding" Value="16"/>
    </Style>

    <!-- MaterialDesign Custom Color Palette -->
    <SolidColorBrush x:Key="MaterialDesignPrimary">#1565c0</SolidColorBrush>
    <SolidColorBrush x:Key="MaterialDesignSecondary">#0EA8A0</SolidColorBrush>
    <SolidColorBrush x:Key="MaterialDesignPaper">#FFFFFF</SolidColorBrush>
    <SolidColorBrush x:Key="MaterialDesignBackground">#F5F5F5</SolidColorBrush>
</ResourceDictionary>
```

- [ ] **Step 2: 更新 App.xaml 添加 MaterialDesign 资源字典**

将 `src/client-windows/Pim.Client.App/App.xaml` 替换为：

```xml
<Application x:Class="Pim.Client.App.App"
             xmlns="http://schemas.microsoft.com/winfx/2006/xaml/presentation"
             xmlns:x="http://schemas.microsoft.com/winfx/2006/xaml"
             xmlns:converters="clr-namespace:Pim.Client.App.Converters"
             ShutdownMode="OnExplicitShutdown">
    <Application.Resources>
        <ResourceDictionary>
            <ResourceDictionary.MergedDictionaries>
                <ResourceDictionary Source="pack://application:,,,/MaterialDesignThemes.Wpf;component/Themes/MaterialDesign3.Defaults.xaml"/>
                <ResourceDictionary Source="Styles/Theme.xaml"/>
            </ResourceDictionary.MergedDictionaries>
            <converters:BoolToVisibilityConverter x:Key="BoolToVisibility"/>
            <converters:InverseBoolConverter x:Key="InverseBool"/>
            <converters:StringEqualsConverter x:Key="StringEquals"/>
            <converters:StringNotEmptyConverter x:Key="StringNotEmpty"/>
        </ResourceDictionary>
    </Application.Resources>
</Application>
```

- [ ] **Step 3: 构建验证**

```bash
dotnet build src/client-windows/Pim.Client.App/Pim.Client.App.csproj
```

- [ ] **Step 4: Commit**

```bash
git add src/client-windows/Pim.Client.App/App.xaml src/client-windows/Pim.Client.App/Styles/Theme.xaml
git commit -m "feat: integrate MaterialDesignThemes with custom color palette

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

### Task 3: 添加新转换器

**Files:**
- Modify: `src/client-windows/Pim.Client.App/Converters/Converters.cs`

- [ ] **Step 1: 添加 InverseBoolValueConverter 和 BoolToOpacityConverter**

在 `src/client-windows/Pim.Client.App/Converters/Converters.cs` 末尾追加：

```csharp
public class InverseBoolValueConverter : IValueConverter
{
    public object Convert(object value, Type targetType, object parameter, CultureInfo culture)
    {
        return value is bool b && !b;
    }

    public object ConvertBack(object value, Type targetType, object parameter, CultureInfo culture)
    {
        return value is bool b && !b;
    }
}

public class PriorityToColorConverter : IValueConverter
{
    public object Convert(object value, Type targetType, object parameter, CultureInfo culture)
    {
        return value is int priority switch
        {
            1 => "#E53935",
            3 => "#43A047",
            _ => "#FFA726"
        };
    }

    public object ConvertBack(object value, Type targetType, object parameter, CultureInfo culture) =>
        throw new NotSupportedException();
}

public class PriorityToBrushConverter : IValueConverter
{
    public object Convert(object value, Type targetType, object parameter, CultureInfo culture)
    {
        var color = value is int priority switch
        {
            1 => "#E53935",
            3 => "#43A047",
            _ => "#FFA726"
        };
        return new SolidColorBrush((Color)ColorConverter.ConvertFromString(color));
    }

    public object ConvertBack(object value, Type targetType, object parameter, CultureInfo culture) =>
        throw new NotSupportedException();
}

public class HexToBrushConverter : IValueConverter
{
    public object Convert(object value, Type targetType, object parameter, CultureInfo culture)
    {
        if (value is string hex && !string.IsNullOrEmpty(hex))
        {
            try
            {
                hex = hex.Replace("#", "");
                if (hex.Length == 6) hex = "FF" + hex;
                return new SolidColorBrush(
                    (Color)ColorConverter.ConvertFromString("#" + hex));
            }
            catch { }
        }
        return new SolidColorBrush((Color)ColorConverter.ConvertFromString("#6B5EE4"));
    }

    public object ConvertBack(object value, Type targetType, object parameter, CultureInfo culture) =>
        throw new NotSupportedException();
}

public class DateTimeToTimeLabelConverter : IValueConverter
{
    public object Convert(object value, Type targetType, object parameter, CultureInfo culture)
    {
        return value is DateTime dt
            ? $"{dt.Hour:D2}:{dt.Minute:D2}"
            : "--:--";
    }

    public object ConvertBack(object value, Type targetType, object parameter, CultureInfo culture) =>
        throw new NotSupportedException();
}

public class MinutesToDurationLabelConverter : IValueConverter
{
    public object Convert(object value, Type targetType, object parameter, CultureInfo culture)
    {
        if (value is int minutes)
        {
            if (minutes < 60) return $"{minutes}分钟";
            var h = minutes / 60;
            var m = minutes % 60;
            return m == 0 ? $"{h}小时" : $"{h}小时{m}分钟";
        }
        return "";
    }

    public object ConvertBack(object value, Type targetType, object parameter, CultureInfo culture) =>
        throw new NotSupportedException();
}
```

- [ ] **Step 2: 在 App.xaml 中注册新转换器**

在 `App.xaml` 的 `<Application.Resources>` 字典内添加：

```xml
<converters:InverseBoolValueConverter x:Key="InverseBoolValue"/>
<converters:PriorityToColorConverter x:Key="PriorityToColor"/>
<converters:PriorityToBrushConverter x:Key="PriorityToBrush"/>
<converters:HexToBrushConverter x:Key="HexToBrush"/>
<converters:DateTimeToTimeLabelConverter x:Key="DateTimeToTimeLabel"/>
<converters:MinutesToDurationLabelConverter x:Key="MinutesToDurationLabel"/>
```

- [ ] **Step 3: 构建验证**

```bash
dotnet build src/client-windows/Pim.Client.App/Pim.Client.App.csproj
```

- [ ] **Step 4: Commit**

```bash
git add src/client-windows/Pim.Client.App/Converters/Converters.cs src/client-windows/Pim.Client.App/App.xaml
git commit -m "feat: add UI converters for priority colors, time labels, and hex brushes

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

### Task 4: 创建 ShellViewModel

**Files:**
- Create: `src/client-windows/Pim.Client.App/ViewModels/ShellViewModel.cs`

- [ ] **Step 1: 编写 ShellViewModel**

```csharp
using System.Collections.ObjectModel;
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using Pim.Client.Core.Models;
using Pim.Client.Core.Services;

namespace Pim.Client.App.ViewModels;

public partial class ShellViewModel : ObservableObject
{
    private readonly ApiClient _apiClient;

    [ObservableProperty] private string _currentView = "timeline";
    [ObservableProperty] private DateTime _selectedDate = DateTime.Today;
    [ObservableProperty] private string _userDisplayName = string.Empty;
    [ObservableProperty] private ObservableCollection<CalendarResponse> _calendars = new();
    [ObservableProperty] private ObservableCollection<TaskListResponse> _taskLists = new();

    public ShellViewModel(ApiClient apiClient)
    {
        _apiClient = apiClient;
    }

    public void SetUserInfo(string displayName)
    {
        UserDisplayName = displayName;
    }

    [RelayCommand]
    private void Navigate(string viewName)
    {
        CurrentView = viewName;
    }

    [RelayCommand]
    private void GoToToday()
    {
        SelectedDate = DateTime.Today;
    }

    [RelayCommand]
    private void GoToPrevDay()
    {
        SelectedDate = SelectedDate.AddDays(-1);
    }

    [RelayCommand]
    private void GoToNextDay()
    {
        SelectedDate = SelectedDate.AddDays(1);
    }

    [RelayCommand]
    private void GoToPrevWeek()
    {
        SelectedDate = SelectedDate.AddDays(-7);
    }

    [RelayCommand]
    private void GoToNextWeek()
    {
        SelectedDate = SelectedDate.AddDays(7);
    }

    [RelayCommand]
    private void GoToPrevMonth()
    {
        SelectedDate = SelectedDate.AddMonths(-1);
    }

    [RelayCommand]
    private void GoToNextMonth()
    {
        SelectedDate = SelectedDate.AddMonths(1);
    }

    public async Task LoadCalendarsAsync()
    {
        try
        {
            var list = await _apiClient.GetAsync<List<CalendarResponse>>("calendars");
            Calendars = new ObservableCollection<CalendarResponse>(list ?? new());
        }
        catch { }
    }

    public async Task LoadTaskListsAsync()
    {
        try
        {
            var list = await _apiClient.GetAsync<List<TaskListResponse>>("calendars/task-lists");
            TaskLists = new ObservableCollection<TaskListResponse>(list ?? new());
        }
        catch { }
    }
}
```

- [ ] **Step 2: 验证编译**

```bash
dotnet build src/client-windows/Pim.Client.App/Pim.Client.App.csproj
```

- [ ] **Step 3: Commit**

```bash
git add src/client-windows/Pim.Client.App/ViewModels/ShellViewModel.cs
git commit -m "feat: add ShellViewModel with navigation and date controls

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

### Task 5: 重写 MainWindow Shell 布局

**Files:**
- Modify: `src/client-windows/Pim.Client.App/MainWindow.xaml`

- [ ] **Step 1: 编写 Shell 布局 XAML**

将 `src/client-windows/Pim.Client.App/MainWindow.xaml` 替换为：

```xml
<Window x:Class="Pim.Client.App.MainWindow"
        xmlns="http://schemas.microsoft.com/winfx/2006/xaml/presentation"
        xmlns:x="http://schemas.microsoft.com/winfx/2006/xaml"
        xmlns:views="clr-namespace:Pim.Client.App.Views"
        xmlns:md="http://materialdesigninxaml.net/winfx/xaml/themes"
        Title="PIM" Height="900" Width="1400"
        MinHeight="600" MinWidth="1000"
        WindowStartupLocation="CenterScreen"
        TextElement.Foreground="{StaticResource TextPrimaryBrush}"
        Background="{StaticResource BgBrush}">
    <Grid>
        <Grid.ColumnDefinitions>
            <ColumnDefinition Width="200"/>
            <ColumnDefinition Width="*"/>
            <ColumnDefinition Width="280"/>
        </Grid.ColumnDefinitions>

        <!-- Left Sidebar -->
        <Border Grid.Column="0" Background="{StaticResource NavBgBrush}">
            <Grid>
                <Grid.RowDefinitions>
                    <RowDefinition Height="Auto"/>
                    <RowDefinition Height="*"/>
                    <RowDefinition Height="Auto"/>
                </Grid.RowDefinitions>

                <!-- App Title -->
                <TextBlock Text="PIM" FontSize="18" FontWeight="Bold"
                           Margin="16,14" Foreground="{StaticResource PrimaryBrush}"/>

                <!-- Navigation -->
                <StackPanel Grid.Row="1" Margin="8,4">
                    <Button Content="⏱  时间轴"
                            Command="{Binding NavigateCommand}" CommandParameter="timeline">
                        <Button.Style>
                            <Style TargetType="Button" BasedOn="{StaticResource NavButton}">
                                <Style.Triggers>
                                    <DataTrigger Binding="{Binding CurrentView}" Value="timeline">
                                        <Setter Property="Background" Value="{StaticResource PrimaryLightBrush}"/>
                                        <Setter Property="Foreground" Value="{StaticResource PrimaryBrush}"/>
                                        <Setter Property="FontWeight" Value="SemiBold"/>
                                    </DataTrigger>
                                </Style.Triggers>
                            </Style>
                        </Button.Style>
                    </Button>
                    <Button Content="📅  本周"
                            Command="{Binding NavigateCommand}" CommandParameter="week"
                            Style="{StaticResource NavButton}" Margin="0,2,0,0">
                        <Button.Resources>
                            <Style TargetType="Button" BasedOn="{StaticResource NavButton}">
                                <Style.Triggers>
                                    <DataTrigger Binding="{Binding CurrentView}" Value="week">
                                        <Setter Property="Background" Value="{StaticResource PrimaryLightBrush}"/>
                                        <Setter Property="Foreground" Value="{StaticResource PrimaryBrush}"/>
                                        <Setter Property="FontWeight" Value="SemiBold"/>
                                    </DataTrigger>
                                </Style.Triggers>
                            </Style>
                        </Button.Resources>
                    </Button>
                    <Button Content="📆  月视图"
                            Command="{Binding NavigateCommand}" CommandParameter="month"
                            Style="{StaticResource NavButton}" Margin="0,2,0,0">
                        <Button.Resources>
                            <Style TargetType="Button" BasedOn="{StaticResource NavButton}">
                                <Style.Triggers>
                                    <DataTrigger Binding="{Binding CurrentView}" Value="month">
                                        <Setter Property="Background" Value="{StaticResource PrimaryLightBrush}"/>
                                        <Setter Property="Foreground" Value="{StaticResource PrimaryBrush}"/>
                                        <Setter Property="FontWeight" Value="SemiBold"/>
                                    </DataTrigger>
                                </Style.Triggers>
                            </Style>
                        </Button.Resources>
                    </Button>
                    <Button Content="📋  任务"
                            Command="{Binding NavigateCommand}" CommandParameter="tasks"
                            Style="{StaticResource NavButton}" Margin="0,2,0,0">
                        <Button.Resources>
                            <Style TargetType="Button" BasedOn="{StaticResource NavButton}">
                                <Style.Triggers>
                                    <DataTrigger Binding="{Binding CurrentView}" Value="tasks">
                                        <Setter Property="Background" Value="{StaticResource PrimaryLightBrush}"/>
                                        <Setter Property="Foreground" Value="{StaticResource PrimaryBrush}"/>
                                        <Setter Property="FontWeight" Value="SemiBold"/>
                                    </DataTrigger>
                                </Style.Triggers>
                            </Style>
                        </Button.Resources>
                    </Button>
                </StackPanel>

                <!-- Calendar Books at bottom of nav -->
                <Border Grid.Row="2" BorderBrush="{StaticResource BorderBrush}" BorderThickness="0,1,0,0"
                        Padding="12,10" Margin="4,0">
                    <StackPanel>
                        <TextBlock Text="日历本" FontSize="11" Foreground="#888"
                                   Margin="0,0,0,8"/>
                        <ItemsControl ItemsSource="{Binding Calendars}">
                            <ItemsControl.ItemTemplate>
                                <DataTemplate>
                                    <StackPanel Orientation="Horizontal" Margin="0,3">
                                        <Ellipse Width="10" Height="10"
                                                 Fill="{Binding ColorHex, Converter={StaticResource HexToBrush}}"/>
                                        <TextBlock Text="{Binding Name}" FontSize="12"
                                                   Margin="8,0,0,0" VerticalAlignment="Center"/>
                                    </StackPanel>
                                </DataTemplate>
                            </ItemsControl.ItemTemplate>
                        </ItemsControl>
                    </StackPanel>
                </Border>
            </Grid>
        </Border>

        <!-- Main Content Area -->
        <Grid Grid.Column="1">
            <Grid.RowDefinitions>
                <RowDefinition Height="*"/>
            </Grid.RowDefinitions>

            <views:TimelineView Visibility="{Binding CurrentView, Converter={StaticResource StringEquals}, ConverterParameter=timeline}"/>
            <views:WeekView Visibility="{Binding CurrentView, Converter={StaticResource StringEquals}, ConverterParameter=week}"/>
            <views:MonthView Visibility="{Binding CurrentView, Converter={StaticResource StringEquals}, ConverterParameter=month}"/>
            <views:TaskListView Visibility="{Binding CurrentView, Converter={StaticResource StringEquals}, ConverterParameter=tasks}"/>
        </Grid>

        <!-- Right Inbox Panel -->
        <views:InboxPanel Grid.Column="2"/>
    </Grid>
</Window>
```

- [ ] **Step 2: Commit**

```bash
git add src/client-windows/Pim.Client.App/MainWindow.xaml
git commit -m "feat: rewrite MainWindow with Shell layout (left nav + content + right inbox)

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

### Task 6: 更新 MainWindow.xaml.cs 和移除旧 NavigationService

**Files:**
- Modify: `src/client-windows/Pim.Client.App/MainWindow.xaml.cs`
- Modify: `src/client-windows/Pim.Client.App/Startup.cs`

- [ ] **Step 1: 更新 MainWindow.xaml.cs**

将 `src/client-windows/Pim.Client.App/MainWindow.xaml.cs` 替换为：

```csharp
using System.Windows;
using Microsoft.Extensions.DependencyInjection;
using Pim.Client.App.ViewModels;
using Pim.Client.App.Views;

namespace Pim.Client.App;

public partial class MainWindow : Window
{
    private readonly ShellViewModel _shellVm;
    private readonly IServiceProvider _services;
    private bool _isLoggingOut;

    public event Action? LoggedOutAndReauthenticated;

    public MainWindow(ShellViewModel shellVm, IServiceProvider services)
    {
        Logger.Info("MainWindow constructing");
        InitializeComponent();
        _shellVm = shellVm;
        _services = services;
        DataContext = shellVm;

        var authService = _services.GetRequiredService<Core.Services.AuthService>();
        _shellVm.SetUserInfo(authService.CurrentDisplayName ?? authService.CurrentUsername ?? "");

        Loaded += async (_, _) => await _shellVm.LoadCalendarsAsync();
        Logger.Info("MainWindow constructed");
    }

    protected override void OnClosing(System.ComponentModel.CancelEventArgs e)
    {
        if (!_isLoggingOut)
        {
            Logger.Info("MainWindow closing (user exit), shutting down");
            Application.Current.Shutdown();
        }
        base.OnClosing(e);
    }
}
```

- [ ] **Step 2: 更新 Startup.cs DI 注册**

将 `src/client-windows/Pim.Client.App/Startup.cs` 替换为：

```csharp
using Microsoft.Extensions.DependencyInjection;
using Pim.Client.App.ViewModels;
using Pim.Client.Core.Services;

namespace Pim.Client.App;

public static class Startup
{
    public static IServiceProvider ConfigureServices()
    {
        var services = new ServiceCollection();

        // Core services
        services.AddSingleton<ApiClient>();
        services.AddSingleton<AuthService>();

        // ViewModels
        services.AddSingleton<ShellViewModel>();
        services.AddTransient<LoginViewModel>();
        services.AddTransient<TimelineViewModel>();
        services.AddTransient<WeekViewModel>();
        services.AddTransient<MonthViewModel>();
        services.AddTransient<TaskListViewModel>();
        services.AddTransient<InboxPanelViewModel>();
        services.AddTransient<EventEditorViewModel>();
        services.AddTransient<TaskEditorViewModel>();

        return services.BuildServiceProvider();
    }
}
```

- [ ] **Step 3: 更新 App.xaml.cs（移除旧 MainViewModel 引用）**

修改 `src/client-windows/Pim.Client.App/App.xaml.cs` 中的 `ShowMainWindow` 方法，将 `MainViewModel` 替换为 `ShellViewModel`：

```csharp
private void ShowMainWindow()
{
    try
    {
        var shellVm = Services.GetRequiredService<ShellViewModel>();
        var mainWindow = new MainWindow(shellVm, Services);
        Logger.Info("MainWindow created, showing");
        mainWindow.Show();
    }
    catch (Exception ex)
    {
        Logger.Error("MainWindow creation failed", ex);
        MessageBox.Show($"主窗口加载失败:\n{ex.Message}\n\n这可能是资源文件或样式配置问题。\n\n日志已保存到:\n{Logger.LogFilePath}",
            "PIM 错误", MessageBoxButton.OK, MessageBoxImage.Error);
        Shutdown();
    }
}
```

同时删除 `ShowLoginDialog` 中的 `MainViewModel` 引用，直接检查 auth 状态。

- [ ] **Step 4: 构建验证**

```bash
dotnet build src/client-windows/Pim.Client.App/Pim.Client.App.csproj
```

- [ ] **Step 5: Commit**

```bash
git add src/client-windows/Pim.Client.App/MainWindow.xaml.cs src/client-windows/Pim.Client.App/Startup.cs src/client-windows/Pim.Client.App/App.xaml.cs src/client-windows/Pim.Client.App/Services/INavigationService.cs src/client-windows/Pim.Client.App/Services/NavigationService.cs src/client-windows/Pim.Client.App/ViewModels/MainViewModel.cs
git commit -m "refactor: replace MainViewModel with ShellViewModel, remove NavigationService

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

### Task 7: 创建时间轴视图 (TimelineView + TimelineViewModel)

**Files:**
- Create: `src/client-windows/Pim.Client.App/ViewModels/TimelineViewModel.cs`
- Create: `src/client-windows/Pim.Client.App/Views/TimelineView.xaml`
- Create: `src/client-windows/Pim.Client.App/Views/TimelineView.xaml.cs`

- [ ] **Step 1: 编写 TimelineViewModel**

```csharp
using System.Collections.ObjectModel;
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using Pim.Client.Core.Models;
using Pim.Client.Core.Services;

namespace Pim.Client.App.ViewModels;

public partial class TimelineViewModel : ObservableObject
{
    private readonly ApiClient _apiClient;

    [ObservableProperty] private DateTime _selectedDate = DateTime.Today;
    [ObservableProperty] private ObservableCollection<CalendarItemDisplay> _displayItems = new();
    [ObservableProperty] private bool _isLoading;

    public TimelineViewModel(ApiClient apiClient)
    {
        _apiClient = apiClient;
    }

    public async Task LoadAsync(DateTime date)
    {
        SelectedDate = date;
        IsLoading = true;
        try
        {
            var from = date.Date;
            var to = from.AddDays(1);
            var events = await _apiClient.GetAsync<List<EventResponse>>(
                $"calendars/events?from={from:O}&to={to:O}");
            var tasks = await _apiClient.GetAsync<List<TaskResponse>>("calendars/tasks");

            var items = new List<CalendarItemDisplay>();
            if (events != null)
                items.AddRange(events.Select(e => CalendarItemDisplay.FromEvent(e)));
            if (tasks != null)
                items.AddRange(tasks
                    .Where(t => t.DtStart?.Date == date.Date)
                    .Select(t => CalendarItemDisplay.FromTask(t)));
            DisplayItems = new ObservableCollection<CalendarItemDisplay>(
                items.OrderBy(i => i.Start));
        }
        finally { IsLoading = false; }
    }

    [RelayCommand]
    private async Task SelectDate(DateTime date)
    {
        await LoadAsync(date);
    }

    public double GetTopOffset(DateTimeOffset time, double hourHeight = 80.0)
    {
        return (time.Hour + time.Minute / 60.0) * hourHeight;
    }

    public double GetHeight(DateTimeOffset start, DateTimeOffset end, double hourHeight = 80.0)
    {
        var minutes = (end - start).TotalMinutes;
        return Math.Max(minutes / 60.0 * hourHeight, 20.0);
    }
}

public record CalendarItemDisplay(
    string Id, string Title, DateTimeOffset Start, DateTimeOffset End,
    string Type, string ColorHex, string Subtitle)
{
    public static CalendarItemDisplay FromEvent(EventResponse e) => new(
        e.Id.ToString(), e.Title, e.Start, e.End ?? e.Start.AddHours(1),
        "event", e.ColorHex ?? "#6B5EE4",
        $"{e.Start:HH:mm}-{(e.End ?? e.Start.AddHours(1)):HH:mm}{(string.IsNullOrEmpty(e.Location) ? "" : $" · {e.Location}")}");

    public static CalendarItemDisplay FromTask(TaskResponse t) => new(
        t.Id.ToString(), t.Title, t.DtStart ?? DateTimeOffset.Now,
        (t.DtStart ?? DateTimeOffset.Now).AddMinutes(Math.Max(t.EstimatedDurationMinutes(), 30)),
        "task", t.Priority switch { 1 => "#E53935", 3 => "#43A047", _ => "#FFA726" },
        $"{t.DtStart:HH:mm} · {DurationLabel(t.EstimatedDurationMinutes())}{(string.IsNullOrEmpty(t.Location) ? "" : $" · {t.Location}")}");

    private static string DurationLabel(int? min)
    {
        if (min is not { } m) return "";
        if (m < 60) return $"{m}分钟";
        var h = m / 60;
        var r = m % 60;
        return r == 0 ? $"{h}小时" : $"{h}小时{r}分钟";
    }
}
```

- [ ] **Step 2: 编写 TimelineView.xaml**

```xml
<UserControl x:Class="Pim.Client.App.Views.TimelineView"
             xmlns="http://schemas.microsoft.com/winfx/2006/xaml/presentation"
             xmlns:x="http://schemas.microsoft.com/winfx/2006/xaml"
             xmlns:md="http://materialdesigninxaml.net/winfx/xaml/themes">
    <Grid>
        <Grid.RowDefinitions>
            <RowDefinition Height="Auto"/>
            <RowDefinition Height="Auto"/>
            <RowDefinition Height="*"/>
        </Grid.RowDefinitions>

        <!-- Header -->
        <Border Grid.Row="0" Padding="16,12" Background="{StaticResource CardBrush}"
                BorderBrush="{StaticResource BorderBrush}" BorderThickness="0,0,0,1">
            <Grid>
                <StackPanel Orientation="Horizontal">
                    <TextBlock FontSize="18" FontWeight="Bold" VerticalAlignment="Center">
                        <Run Text="{Binding SelectedDate.Month}"/><Run Text="月"/>
                        <Run Text="{Binding SelectedDate.Day}"/><Run Text="日"/>
                        <Run Text=" "/>
                        <Run Text="星期"/>
                    </TextBlock>
                    <TextBlock x:Name="TodayLabel" FontSize="12" Foreground="{StaticResource PrimaryBrush}"
                               FontWeight="SemiBold" Margin="8,0,0,0" VerticalAlignment="Center"/>
                </StackPanel>
                <StackPanel Orientation="Horizontal" HorizontalAlignment="Right">
                    <Button Content="今日" Style="{StaticResource SecondaryButton}"
                            Padding="8,4" FontSize="12" Margin="0,0,8,0"
                            Command="{Binding GoToTodayCommand}"/>
                    <Button Content="‹" Style="{StaticResource SecondaryButton}"
                            Padding="6,4" FontSize="14" Margin="0,0,2,0"
                            Command="{Binding GoToPrevDayCommand}"/>
                    <Button Content="›" Style="{StaticResource SecondaryButton}"
                            Padding="6,4" FontSize="14"
                            Command="{Binding GoToNextDayCommand}"/>
                </StackPanel>
            </Grid>
        </Border>

        <!-- Date Strip -->
        <Border Grid.Row="1" Padding="8,6" Background="{StaticResource CardBrush}"
                BorderBrush="{StaticResource BorderBrush}" BorderThickness="0,0,0,1">
            <ScrollViewer HorizontalScrollBarVisibility="Auto" VerticalScrollBarVisibility="Disabled">
                <ItemsControl ItemsSource="{Binding DateStripItems}">
                    <ItemsControl.ItemsPanel>
                        <ItemsPanelTemplate>
                            <StackPanel Orientation="Horizontal"/>
                        </ItemsPanelTemplate>
                    </ItemsControl.ItemsPanel>
                    <ItemsControl.ItemTemplate>
                        <DataTemplate>
                            <Button Width="44" Height="56" Margin="2"
                                    Command="{Binding DataContext.SelectDateCommand, RelativeSource={RelativeSource AncestorType=UserControl}}"
                                    CommandParameter="{Binding Date}">
                                <Button.Template>
                                    <ControlTemplate TargetType="Button">
                                        <Border CornerRadius="10" Padding="4"
                                                Background="{TemplateBinding Background}">
                                            <StackPanel HorizontalAlignment="Center" VerticalAlignment="Center">
                                                <TextBlock Text="{Binding Weekday}" FontSize="10"
                                                           HorizontalAlignment="Center"
                                                           Foreground="{Binding WeekdayForeground}"/>
                                                <TextBlock Text="{Binding Day}" FontSize="15" FontWeight="SemiBold"
                                                           HorizontalAlignment="Center" Margin="0,2,0,0"
                                                           Foreground="{Binding DayForeground}"/>
                                            </StackPanel>
                                        </Border>
                                    </ControlTemplate>
                                </Button.Template>
                                <Button.Style>
                                    <Style TargetType="Button">
                                        <Setter Property="Background" Value="Transparent"/>
                                        <Style.Triggers>
                                            <DataTrigger Binding="{Binding IsToday}" Value="True">
                                                <Setter Property="Background" Value="#E3F0FD"/>
                                            </DataTrigger>
                                            <DataTrigger Binding="{Binding IsSelected}" Value="True">
                                                <Setter Property="Background" Value="{StaticResource PrimaryBrush}"/>
                                            </DataTrigger>
                                        </Style.Triggers>
                                    </Style>
                                </Button.Style>
                            </Button>
                        </DataTemplate>
                    </ItemsControl.ItemTemplate>
                </ItemsControl>
            </ScrollViewer>
        </Border>

        <!-- Time Grid -->
        <ScrollViewer Grid.Row="2" VerticalScrollBarVisibility="Auto"
                      x:Name="TimeScroller">
            <Grid Height="1920"> <!-- 24 hours * 80px -->
                <Grid.ColumnDefinitions>
                    <ColumnDefinition Width="52"/>
                    <ColumnDefinition Width="3*"/>
                    <ColumnDefinition Width="2*"/>
                </Grid.ColumnDefinitions>

                <!-- Time Labels -->
                <ItemsControl Grid.Column="0">
                    <ItemsControl.ItemsPanel>
                        <ItemsPanelTemplate><Canvas/></ItemsPanelTemplate>
                    </ItemsControl.ItemsPanel>
                    <ItemsControl.ItemTemplate>
                        <DataTemplate>
                            <TextBlock Text="{Binding Label}" FontSize="10" Foreground="#999"
                                       Canvas.Top="{Binding Top}"/>
                        </DataTemplate>
                    </ItemsControl.ItemTemplate>
                </ItemsControl>

                <Border Grid.Column="0" BorderBrush="#EEE" BorderThickness="0,0,1,0"/>

                <!-- Plan Column -->
                <Grid Grid.Column="1">
                    <Border BorderBrush="#F0F0F0" BorderThickness="0,0,1,0"/>
                    <TextBlock Text="计划" FontSize="11" Foreground="{StaticResource PrimaryBrush}"
                               FontWeight="SemiBold" Margin="8,6,0,0"/>
                    <ItemsControl ItemsSource="{Binding DisplayItems}" x:Name="PlanItems">
                        <ItemsControl.ItemsPanel>
                            <ItemsPanelTemplate><Canvas/></ItemsPanelTemplate>
                        </ItemsControl.ItemsPanel>
                        <ItemsControl.ItemTemplate>
                            <DataTemplate>
                                <Border CornerRadius="6" Padding="8,6" Cursor="Hand"
                                        Canvas.Top="{Binding TopOffset}" Canvas.Left="6"
                                        Width="{Binding PlanWidth}" Height="{Binding BlockHeight}">
                                    <Border.Background>
                                        <SolidColorBrush Color="{Binding BackgroundColor}"
                                                         Opacity="0.12"/>
                                    </Border.Background>
                                    <Border BorderBrush="{Binding AccentBrush}"
                                            BorderThickness="4,0,0,0" CornerRadius="6">
                                        <StackPanel>
                                            <TextBlock Text="{Binding Title}" FontSize="12"
                                                       FontWeight="SemiBold"/>
                                            <TextBlock Text="{Binding Subtitle}" FontSize="10"
                                                       Foreground="#888" TextTrimming="CharacterEllipsis"/>
                                        </StackPanel>
                                    </Border>
                                </Border>
                            </DataTemplate>
                        </ItemsControl.ItemTemplate>
                    </ItemsControl>

                    <!-- Current Time Line -->
                    <Border x:Name="TimeLine" BorderBrush="{StaticResource DangerBrush}"
                            BorderThickness="0,2,0,0" VerticalAlignment="Top"
                            HorizontalAlignment="Stretch" Margin="-5,0,0,0">
                        <Ellipse Width="10" Height="10" Fill="{StaticResource DangerBrush}"
                                 HorizontalAlignment="Left" VerticalAlignment="Top"
                                 Margin="-5,-6,0,0"/>
                    </Border>
                </Grid>

                <!-- Actual Column -->
                <Border Grid.Column="2" Padding="4">
                    <StackPanel VerticalAlignment="Center" HorizontalAlignment="Center">
                        <TextBlock Text="实际" FontSize="11" Foreground="#888" FontWeight="SemiBold"
                                   HorizontalAlignment="Center" Margin="0,0,0,4"/>
                        <TextBlock Text="暂未接入数据" FontSize="12" Foreground="#CCC"
                                   HorizontalAlignment="Center"/>
                        <TextBlock Text="实际活动将显示在此处" FontSize="11" Foreground="#CCC"
                                   HorizontalAlignment="Center"/>
                    </StackPanel>
                </Border>
            </Grid>
        </ScrollViewer>
    </Grid>
</UserControl>
```

- [ ] **Step 3: 编写 TimelineView.xaml.cs**

```csharp
using System.Windows.Controls;
using System.Windows.Threading;

namespace Pim.Client.App.Views;

public partial class TimelineView : UserControl
{
    private readonly DispatcherTimer _timer;

    public TimelineView()
    {
        InitializeComponent();
        _timer = new DispatcherTimer(TimeSpan.FromSeconds(60), DispatcherPriority.Normal,
            (_, _) => UpdateTimeLine(), Dispatcher);
        Loaded += (_, _) => UpdateTimeLine();
    }

    private void UpdateTimeLine()
    {
        if (DataContext is ViewModels.TimelineViewModel vm)
        {
            var now = DateTime.Now;
            var top = (now.Hour + now.Minute / 60.0) * 80;
            TimeLine.Margin = new System.Windows.Thickness(-5, top, 0, 0);
            TimeScroller.ScrollToVerticalOffset(Math.Max(0, top - 200));
        }
    }
}
```

- [ ] **Step 4: 构建验证**

```bash
dotnet build src/client-windows/Pim.Client.App/Pim.Client.App.csproj
```

- [ ] **Step 5: Commit**

```bash
git add src/client-windows/Pim.Client.App/ViewModels/TimelineViewModel.cs src/client-windows/Pim.Client.App/Views/TimelineView.xaml src/client-windows/Pim.Client.App/Views/TimelineView.xaml.cs
git commit -m "feat: add TimelineView with time grid, plan/actual columns, and current time indicator

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

### Task 8: 创建本周视图 (WeekView + WeekViewModel)

**Files:**
- Create: `src/client-windows/Pim.Client.App/ViewModels/WeekViewModel.cs`
- Create: `src/client-windows/Pim.Client.App/Views/WeekView.xaml`
- Create: `src/client-windows/Pim.Client.App/Views/WeekView.xaml.cs`

- [ ] **Step 1: 编写 WeekViewModel**

```csharp
using System.Collections.ObjectModel;
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using Pim.Client.Core.Models;
using Pim.Client.Core.Services;

namespace Pim.Client.App.ViewModels;

public partial class WeekViewModel : ObservableObject
{
    private readonly ApiClient _apiClient;

    [ObservableProperty] private DateTime _selectedDate = DateTime.Today;
    [ObservableProperty] private ObservableCollection<WeekDayColumn> _dayColumns = new();
    [ObservableProperty] private string _weekRangeText = "";
    [ObservableProperty] private int _year;

    public WeekViewModel(ApiClient apiClient)
    {
        _apiClient = apiClient;
    }

    public async Task LoadAsync(DateTime date)
    {
        SelectedDate = date;
        Year = date.Year;
        var monday = date.AddDays(-(int)date.DayOfWeek + 1);
        if (date.DayOfWeek == DayOfWeek.Sunday) monday = date.AddDays(-6);
        WeekRangeText = $"{monday.Month}/{monday.Day} - {monday.AddDays(6).Month}/{monday.AddDays(6).Day}";

        var weekStart = monday.Date;
        var weekEnd = weekStart.AddDays(7);

        var events = await _apiClient.GetAsync<List<EventResponse>>(
            $"calendars/events?from={weekStart:O}&to={weekEnd:O}");
        var tasks = await _apiClient.GetAsync<List<TaskResponse>>("calendars/tasks");

        var columns = new List<WeekDayColumn>();
        for (int i = 0; i < 7; i++)
        {
            var day = weekStart.AddDays(i);
            var dayEvents = events?.Where(e => e.Start.Date == day).ToList() ?? new();
            var dayTasks = tasks?.Where(t => t.DtStart?.Date == day).ToList() ?? new();
            var items = new List<WeekItemDisplay>();
            items.AddRange(dayEvents.Select(WeekItemDisplay.FromEvent));
            items.AddRange(dayTasks.Select(WeekItemDisplay.FromTask));
            columns.Add(new WeekDayColumn
            {
                Date = day,
                DayNumber = day.Day,
                WeekdayLabel = new[] { "一", "二", "三", "四", "五", "六", "日" }[i],
                IsToday = day.Date == DateTime.Today,
                IsSunday = day.DayOfWeek == DayOfWeek.Sunday,
                Items = new ObservableCollection<WeekItemDisplay>(items.OrderBy(i => i.TopOffset))
            });
        }
        DayColumns = new ObservableCollection<WeekDayColumn>(columns);
    }

    [RelayCommand]
    private void GoToPrevWeek() => LoadAsync(SelectedDate.AddDays(-7));

    [RelayCommand]
    private void GoToNextWeek() => LoadAsync(SelectedDate.AddDays(7));

    [RelayCommand]
    private void GoToToday() => LoadAsync(DateTime.Today);
}

public class WeekDayColumn : ObservableObject
{
    public DateTime Date { get; set; }
    public int DayNumber { get; set; }
    public string WeekdayLabel { get; set; } = "";
    public bool IsToday { get; set; }
    public bool IsSunday { get; set; }
    public ObservableCollection<WeekItemDisplay> Items { get; set; } = new();
}

public class WeekItemDisplay
{
    public string Id { get; set; } = "";
    public string Title { get; set; } = "";
    public string TimeLabel { get; set; } = "";
    public string ColorHex { get; set; } = "#6B5EE4";
    public double TopOffset { get; set; }
    public double Height { get; set; }

    public static WeekItemDisplay FromEvent(EventResponse e)
    {
        var start = e.Start;
        var end = e.End ?? start.AddHours(1);
        var minutes = (end - start).TotalMinutes;
        return new WeekItemDisplay
        {
            Id = e.Id.ToString(),
            Title = e.Title,
            TimeLabel = $"{start:HH:mm}",
            ColorHex = e.ColorHex ?? "#6B5EE4",
            TopOffset = (start.Hour + start.Minute / 60.0) * 60,
            Height = Math.Max(minutes / 60.0 * 60, 20)
        };
    }

    public static WeekItemDisplay FromTask(TaskResponse t)
    {
        var start = t.DtStart ?? DateTimeOffset.Now;
        var minutes = t.EstimatedDurationMinutes() ?? 60;
        return new WeekItemDisplay
        {
            Id = t.Id.ToString(),
            Title = t.Title,
            TimeLabel = $"{start:HH:mm}",
            ColorHex = t.Priority switch { 1 => "#E53935", 3 => "#43A047", _ => "#FFA726" },
            TopOffset = (start.Hour + start.Minute / 60.0) * 60,
            Height = Math.Max(minutes / 60.0 * 60, 20)
        };
    }
}
```

- [ ] **Step 2: 编写 WeekView.xaml**

```xml
<UserControl x:Class="Pim.Client.App.Views.WeekView"
             xmlns="http://schemas.microsoft.com/winfx/2006/xaml/presentation"
             xmlns:x="http://schemas.microsoft.com/winfx/2006/xaml">
    <Grid>
        <Grid.RowDefinitions>
            <RowDefinition Height="Auto"/>
            <RowDefinition Height="Auto"/>
            <RowDefinition Height="*"/>
        </Grid.RowDefinitions>

        <!-- Header -->
        <Border Grid.Row="0" Padding="16,12" Background="{StaticResource CardBrush}"
                BorderBrush="{StaticResource BorderBrush}" BorderThickness="0,0,0,1">
            <Grid>
                <StackPanel Orientation="Horizontal">
                    <TextBlock Text="{Binding WeekRangeText}" FontSize="16" FontWeight="Bold"/>
                    <TextBlock Text="{Binding Year, StringFormat=' {0}'}" FontSize="12"
                               Foreground="#888" VerticalAlignment="Center" Margin="8,0,0,0"/>
                </StackPanel>
                <StackPanel Orientation="Horizontal" HorizontalAlignment="Right">
                    <Button Content="今日" Style="{StaticResource SecondaryButton}"
                            Padding="8,4" FontSize="12" Margin="0,0,8,0"
                            Command="{Binding GoToTodayCommand}"/>
                    <Button Content="‹" Style="{StaticResource SecondaryButton}"
                            Padding="6,4" FontSize="14" Margin="0,0,2,0"
                            Command="{Binding GoToPrevWeekCommand}"/>
                    <Button Content="›" Style="{StaticResource SecondaryButton}"
                            Padding="6,4" FontSize="14"
                            Command="{Binding GoToNextWeekCommand}"/>
                </StackPanel>
            </Grid>
        </Border>

        <!-- Day Column Headers -->
        <Border Grid.Row="1" BorderBrush="{StaticResource BorderBrush}" BorderThickness="0,0,0,1">
            <Grid>
                <Grid.ColumnDefinitions>
                    <ColumnDefinition Width="52"/>
                    <ColumnDefinition Width="*"/>
                    <ColumnDefinition Width="*"/>
                    <ColumnDefinition Width="*"/>
                    <ColumnDefinition Width="*"/>
                    <ColumnDefinition Width="*"/>
                    <ColumnDefinition Width="*"/>
                    <ColumnDefinition Width="*"/>
                </Grid.ColumnDefinitions>
                <ItemsControl Grid.Column="1" Grid.ColumnSpan="7"
                              ItemsSource="{Binding DayColumns}">
                    <ItemsControl.ItemsPanel>
                        <ItemsPanelTemplate>
                            <UniformGrid Rows="1"/>
                        </ItemsPanelTemplate>
                    </ItemsControl.ItemsPanel>
                    <ItemsControl.ItemTemplate>
                        <DataTemplate>
                            <Border Padding="8,6" Background="{Binding IsToday, Converter={StaticResource BoolToVisibility}, ConverterParameter=#E3F0FD}">
                                <StackPanel HorizontalAlignment="Center">
                                    <TextBlock Text="{Binding WeekdayLabel}" FontSize="10"
                                               Foreground="{Binding IsSunday, Converter={StaticResource InverseBoolValue}}"
                                               HorizontalAlignment="Center"/>
                                    <Border Width="32" Height="32" CornerRadius="16" Margin="0,4,0,0">
                                        <Border.Style>
                                            <Style TargetType="Border">
                                                <Setter Property="Background" Value="Transparent"/>
                                                <Style.Triggers>
                                                    <DataTrigger Binding="{Binding IsToday}" Value="True">
                                                        <Setter Property="Background" Value="{StaticResource PrimaryBrush}"/>
                                                    </DataTrigger>
                                                </Style.Triggers>
                                            </Style>
                                        </Border.Style>
                                        <TextBlock Text="{Binding DayNumber}" FontSize="14"
                                                   FontWeight="SemiBold" HorizontalAlignment="Center"
                                                   VerticalAlignment="Center"/>
                                    </Border>
                                </StackPanel>
                            </Border>
                        </DataTemplate>
                    </ItemsControl.ItemTemplate>
                </ItemsControl>
            </Grid>
        </Border>

        <!-- Time Grid -->
        <ScrollViewer Grid.Row="2" VerticalScrollBarVisibility="Auto">
            <Grid Height="1080"> <!-- 18 hours (6-24) * 60px -->
                <Grid.ColumnDefinitions>
                    <ColumnDefinition Width="52"/>
                    <ColumnDefinition Width="*"/>
                </Grid.ColumnDefinitions>

                <!-- Time Labels -->
                <StackPanel Grid.Column="0">
                    <!-- Hours 6-23 rendered via ItemsControl or hardcoded -->
                </StackPanel>
                <Border Grid.Column="0" BorderBrush="#EEE" BorderThickness="0,0,1,0"/>

                <!-- 7 Day Columns -->
                <ItemsControl Grid.Column="1" ItemsSource="{Binding DayColumns}">
                    <ItemsControl.ItemsPanel>
                        <ItemsPanelTemplate>
                            <UniformGrid Rows="1"/>
                        </ItemsPanelTemplate>
                    </ItemsControl.ItemsPanel>
                    <ItemsControl.ItemTemplate>
                        <DataTemplate>
                            <Grid>
                                <ItemsControl ItemsSource="{Binding Items}">
                                    <ItemsControl.ItemsPanel>
                                        <ItemsPanelTemplate><Canvas/></ItemsPanelTemplate>
                                    </ItemsControl.ItemsPanel>
                                    <ItemsControl.ItemTemplate>
                                        <DataTemplate>
                                            <Border CornerRadius="4" Canvas.Top="{Binding TopOffset}"
                                                    Canvas.Left="2" Height="{Binding Height}">
                                                <!-- Block content -->
                                            </Border>
                                        </DataTemplate>
                                    </ItemsControl.ItemTemplate>
                                </ItemsControl>
                            </Grid>
                        </DataTemplate>
                    </ItemsControl.ItemTemplate>
                </ItemsControl>
            </Grid>
        </ScrollViewer>
    </Grid>
</UserControl>
```

- [ ] **Step 3: 编写 WeekView.xaml.cs 和 年月视图框架**

WeekView.xaml.cs 为最简单的 code-behind：

```csharp
using System.Windows.Controls;

namespace Pim.Client.App.Views;

public partial class WeekView : UserControl
{
    public WeekView() => InitializeComponent();
}
```

- [ ] **Step 4: 构建验证并 Commit**

```bash
dotnet build src/client-windows/Pim.Client.App/Pim.Client.App.csproj
git add src/client-windows/Pim.Client.App/ViewModels/WeekViewModel.cs src/client-windows/Pim.Client.App/Views/WeekView.xaml src/client-windows/Pim.Client.App/Views/WeekView.xaml.cs
git commit -m "feat: add WeekView with 7-day column layout and time grid

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

### Task 9: 创建月视图 (MonthView + MonthViewModel)

**Files:**
- Create: `src/client-windows/Pim.Client.App/ViewModels/MonthViewModel.cs`
- Create: `src/client-windows/Pim.Client.App/Views/MonthView.xaml`
- Create: `src/client-windows/Pim.Client.App/Views/MonthView.xaml.cs`

- [ ] **Step 1: 编写 MonthViewModel**

```csharp
using System.Collections.ObjectModel;
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using Pim.Client.Core.Models;
using Pim.Client.Core.Services;

namespace Pim.Client.App.ViewModels;

public partial class MonthViewModel : ObservableObject
{
    private readonly ApiClient _apiClient;

    [ObservableProperty] private DateTime _selectedDate = DateTime.Today;
    [ObservableProperty] private DateTime _displayMonth;
    [ObservableProperty] private ObservableCollection<CalendarDay> _days = new();
    [ObservableProperty] private ObservableCollection<PreviewItem> _selectedDayItems = new();
    [ObservableProperty] private string _selectedDayLabel = "";
    [ObservableProperty] private bool _hasEvents;
    [ObservableProperty] private bool _hasTasks;
    [ObservableProperty] private int _totalDays;
    [ObservableProperty] private int _month;
    [ObservableProperty] private int _year;

    public MonthViewModel(ApiClient apiClient)
    {
        _apiClient = apiClient;
        _displayMonth = new DateTime(DateTime.Today.Year, DateTime.Today.Month, 1);
    }

    public async Task LoadAsync(DateTime monthStart)
    {
        DisplayMonth = monthStart;
        Month = monthStart.Month;
        Year = monthStart.Year;

        var monthEnd = monthStart.AddMonths(1);
        var events = await _apiClient.GetAsync<List<EventResponse>>(
            $"calendars/events?from={monthStart:O}&to={monthEnd:O}");
        var tasks = await _apiClient.GetAsync<List<TaskResponse>>("calendars/tasks");

        var dotMap = new Dictionary<string, List<string>>();
        if (events != null)
            foreach (var e in events)
                AddDot(dotMap, e.Start, e.ColorHex ?? "#6B5EE4");
        if (tasks != null)
            foreach (var t in tasks.Where(t => t.DtStart.HasValue))
                AddDot(dotMap, t.DtStart!.Value,
                    t.Priority switch { 1 => "#E53935", 3 => "#43A047", _ => "#FFA726" });

        var startDay = monthStart.AddDays(-(int)monthStart.DayOfWeek);
        var days = new List<CalendarDay>();
        for (int i = 0; i < 42; i++)
        {
            var date = startDay.AddDays(i);
            var key = $"{date:yyyy-MM-dd}";
            days.Add(new CalendarDay
            {
                Date = date,
                Day = date.Day,
                IsCurrentMonth = date.Month == monthStart.Month,
                IsToday = date.Date == DateTime.Today,
                IsSunday = date.DayOfWeek == DayOfWeek.Sunday,
                Dots = dotMap.TryGetValue(key, out var d) ? d : new List<string>()
            });
        }
        Days = new ObservableCollection<CalendarDay>(days);
        await SelectDay(DateTime.Today);
    }

    private static void AddDot(Dictionary<string, List<string>> map, DateTimeOffset date, string color)
    {
        var key = $"{date:yyyy-MM-dd}";
        if (!map.ContainsKey(key)) map[key] = new();
        if (map[key].Count < 4) map[key].Add(color);
    }

    [RelayCommand]
    private async Task SelectDay(DateTime date)
    {
        SelectedDate = date;
        SelectedDayLabel = $"{date.Month}月{date.Day}日 {new[] { "星期日", "星期一", "星期二", "星期三", "星期四", "星期五", "星期六" }[(int)date.DayOfWeek]}";

        var from = date.Date;
        var to = from.AddDays(1);
        var events = await _apiClient.GetAsync<List<EventResponse>>(
            $"calendars/events?from={from:O}&to={to:O}");
        var tasks = await _apiClient.GetAsync<List<TaskResponse>>("calendars/tasks");

        var items = new List<PreviewItem>();
        if (events != null)
        {
            HasEvents = events.Count > 0;
            items.AddRange(events.Select(PreviewItem.FromEvent));
        }
        else HasEvents = false;

        if (tasks != null)
        {
            var dayTasks = tasks.Where(t => t.DtStart?.Date == date.Date).ToList();
            HasTasks = dayTasks.Count > 0;
            items.AddRange(dayTasks.Select(PreviewItem.FromTask));
        }
        else HasTasks = false;

        SelectedDayItems = new ObservableCollection<PreviewItem>(items);
    }

    [RelayCommand]
    private void PrevMonth() => LoadAsync(DisplayMonth.AddMonths(-1));

    [RelayCommand]
    private void NextMonth() => LoadAsync(DisplayMonth.AddMonths(1));
}

public class CalendarDay
{
    public DateTime Date { get; set; }
    public int Day { get; set; }
    public bool IsCurrentMonth { get; set; }
    public bool IsToday { get; set; }
    public bool IsSunday { get; set; }
    public List<string> Dots { get; set; } = new();
}

public class PreviewItem
{
    public string Id { get; set; } = "";
    public string Title { get; set; } = "";
    public string Subtitle { get; set; } = "";
    public string ColorHex { get; set; } = "#6B5EE4";
    public bool IsEvent { get; set; }

    public static PreviewItem FromEvent(EventResponse e) => new()
    {
        Id = e.Id.ToString(), Title = e.Title, IsEvent = true,
        ColorHex = e.ColorHex ?? "#6B5EE4",
        Subtitle = $"{e.Start:HH:mm}{(e.End.HasValue ? $"-{e.End:HH:mm}" : "")}{(string.IsNullOrEmpty(e.Location) ? "" : $" · {e.Location}")}"
    };

    public static PreviewItem FromTask(TaskResponse t) => new()
    {
        Id = t.Id.ToString(), Title = t.Title, IsEvent = false,
        ColorHex = t.Priority switch { 1 => "#E53935", 3 => "#43A047", _ => "#FFA726" },
        Subtitle = $"{(t.DtStart.HasValue ? $"{t.DtStart:HH:mm} · " : "")}{(t.EstimatedDurationMinutes() is { } m ? (m < 60 ? $"{m}分钟" : $"{m / 60}小时") : "")}{(string.IsNullOrEmpty(t.Location) ? "" : $" · {t.Location}")}"
    };
}
```

- [ ] **Step 2: 编写 MonthView.xaml**

```xml
<UserControl x:Class="Pim.Client.App.Views.MonthView"
             xmlns="http://schemas.microsoft.com/winfx/2006/xaml/presentation"
             xmlns:x="http://schemas.microsoft.com/winfx/2006/xaml">
    <Grid>
        <Grid.RowDefinitions>
            <RowDefinition Height="Auto"/>
            <RowDefinition Height="*"/>
            <RowDefinition Height="200"/>
        </Grid.RowDefinitions>

        <!-- Month Header -->
        <Border Grid.Row="0" Padding="16,12" Background="{StaticResource CardBrush}"
                BorderBrush="{StaticResource BorderBrush}" BorderThickness="0,0,0,1">
            <StackPanel Orientation="Horizontal" HorizontalAlignment="Center">
                <Button Content="‹" Style="{StaticResource SecondaryButton}"
                        Padding="8,4" FontSize="16" Margin="0,0,8,0"
                        Command="{Binding PrevMonthCommand}"/>
                <TextBlock FontSize="16" FontWeight="Bold" VerticalAlignment="Center">
                    <Run Text="{Binding Year}"/><Run Text="年 "/>
                    <Run Text="{Binding Month}"/><Run Text="月"/>
                </TextBlock>
                <Button Content="›" Style="{StaticResource SecondaryButton}"
                        Padding="8,4" FontSize="16" Margin="8,0,0,0"
                        Command="{Binding NextMonthCommand}"/>
            </StackPanel>
        </Border>

        <!-- Calendar Grid + Preview -->
        <Grid Grid.Row="1">
            <Grid.RowDefinitions>
                <RowDefinition Height="Auto"/>
                <RowDefinition Height="*"/>
            </Grid.RowDefinitions>

            <!-- Weekday Headers -->
            <UniformGrid Grid.Row="0" Rows="1">
                <TextBlock Text="日" Foreground="#E53935" HorizontalAlignment="Center" Padding="4"/>
                <TextBlock Text="一" HorizontalAlignment="Center" Padding="4"/>
                <TextBlock Text="二" HorizontalAlignment="Center" Padding="4"/>
                <TextBlock Text="三" HorizontalAlignment="Center" Padding="4"/>
                <TextBlock Text="四" HorizontalAlignment="Center" Padding="4"/>
                <TextBlock Text="五" HorizontalAlignment="Center" Padding="4"/>
                <TextBlock Text="六" HorizontalAlignment="Center" Padding="4"/>
            </UniformGrid>

            <!-- Day Cells -->
            <ItemsControl Grid.Row="1" ItemsSource="{Binding Days}">
                <ItemsControl.ItemsPanel>
                    <ItemsPanelTemplate>
                        <UniformGrid Columns="7"/>
                    </ItemsPanelTemplate>
                </ItemsControl.ItemsPanel>
                <ItemsControl.ItemTemplate>
                    <DataTemplate>
                        <Border Padding="4" Cursor="Hand"
                                Command="{Binding DataContext.SelectDayCommand, RelativeSource={RelativeSource AncestorType=UserControl}}"
                                CommandParameter="{Binding Date}">
                            <Border.Background>
                                <SolidColorBrush Color="{Binding IsToday, Converter={StaticResource InverseBoolValue}}"/>
                            </Border.Background>
                            <StackPanel>
                                <Border Width="28" Height="28" CornerRadius="14"
                                        Background="Transparent" HorizontalAlignment="Center">
                                    <TextBlock Text="{Binding Day}" FontSize="13"
                                               Foreground="{Binding IsCurrentMonth, Converter={StaticResource InverseBoolValue}}"
                                               HorizontalAlignment="Center" VerticalAlignment="Center"/>
                                </Border>
                                <ItemsControl ItemsSource="{Binding Dots}" Margin="0,2,0,0"
                                              HorizontalAlignment="Center">
                                    <ItemsControl.ItemsPanel>
                                        <ItemsPanelTemplate>
                                            <StackPanel Orientation="Horizontal"/>
                                        </ItemsPanelTemplate>
                                    </ItemsControl.ItemsPanel>
                                    <ItemsControl.ItemTemplate>
                                        <DataTemplate>
                                            <Ellipse Width="5" Height="5" Margin="1,0">
                                                <Ellipse.Fill>
                                                    <SolidColorBrush Color="{Binding Converter={StaticResource HexToBrush}}"/>
                                                </Ellipse.Fill>
                                            </Ellipse>
                                        </DataTemplate>
                                    </ItemsControl.ItemTemplate>
                                </ItemsControl>
                            </StackPanel>
                        </Border>
                    </DataTemplate>
                </ItemsControl.ItemTemplate>
            </ItemsControl>
        </Grid>

        <!-- Selected Day Preview -->
        <Border Grid.Row="2" BorderBrush="{StaticResource BorderBrush}" BorderThickness="0,1,0,0"
                Background="{StaticResource CardBrush}" Padding="16,12">
            <Grid>
                <Grid.RowDefinitions>
                    <RowDefinition Height="Auto"/>
                    <RowDefinition Height="*"/>
                </Grid.RowDefinitions>
                <TextBlock Text="{Binding SelectedDayLabel}" FontSize="13" FontWeight="Bold" Margin="0,0,0,8"/>
                <ScrollViewer Grid.Row="1">
                    <StackPanel>
                        <TextBlock Text="日程" FontSize="11" Foreground="{StaticResource PrimaryBrush}"
                                   FontWeight="SemiBold" Margin="0,4,0,2"
                                   Visibility="{Binding HasEvents, Converter={StaticResource BoolToVisibility}}"/>
                        <ItemsControl ItemsSource="{Binding SelectedDayItems}">
                            <ItemsControl.ItemTemplate>
                                <DataTemplate>
                                    <Grid Visibility="{Binding IsEvent, Converter={StaticResource BoolToVisibility}}">
                                        <Grid.ColumnDefinitions>
                                            <ColumnDefinition Width="4"/>
                                            <ColumnDefinition Width="*"/>
                                        </Grid.ColumnDefinitions>
                                        <Border Width="4" CornerRadius="2"
                                                Background="{Binding ColorHex, Converter={StaticResource HexToBrush}}"/>
                                        <StackPanel Grid.Column="1" Margin="10,4,0,4">
                                            <TextBlock Text="{Binding Title}" FontSize="13" FontWeight="Medium"/>
                                            <TextBlock Text="{Binding Subtitle}" FontSize="11" Foreground="#999"/>
                                        </StackPanel>
                                    </Grid>
                                </DataTemplate>
                            </ItemsControl.ItemTemplate>
                        </ItemsControl>
                        <TextBlock Text="任务" FontSize="11" Foreground="{StaticResource PrimaryBrush}"
                                   FontWeight="SemiBold" Margin="0,10,0,2"
                                   Visibility="{Binding HasTasks, Converter={StaticResource BoolToVisibility}}"/>
                    </StackPanel>
                </ScrollViewer>
            </Grid>
        </Border>
    </Grid>
</UserControl>
```

- [ ] **Step 3: 编写 MonthView.xaml.cs**

```csharp
using System.Windows.Controls;

namespace Pim.Client.App.Views;

public partial class MonthView : UserControl
{
    public MonthView() => InitializeComponent();
}
```

- [ ] **Step 4: 构建验证并 Commit**

```bash
dotnet build src/client-windows/Pim.Client.App/Pim.Client.App.csproj
git add src/client-windows/Pim.Client.App/ViewModels/MonthViewModel.cs src/client-windows/Pim.Client.App/Views/MonthView.xaml src/client-windows/Pim.Client.App/Views/MonthView.xaml.cs
git commit -m "feat: add MonthView with calendar grid, day dots, and selected day preview

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

### Task 10: 重写任务视图 (TaskListView + TaskListViewModel)

**Files:**
- Modify: `src/client-windows/Pim.Client.App/ViewModels/TaskListViewModel.cs`
- Rewrite: `src/client-windows/Pim.Client.App/Views/TaskListView.xaml`
- Modify: `src/client-windows/Pim.Client.App/Views/TaskListView.xaml.cs`

- [ ] **Step 1: 重写 TaskListViewModel**

将 `src/client-windows/Pim.Client.App/ViewModels/TaskListViewModel.cs` 替换为：

```csharp
using System.Collections.ObjectModel;
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using Pim.Client.Core.Models;
using Pim.Client.Core.Services;

namespace Pim.Client.App.ViewModels;

public partial class TaskListViewModel : ObservableObject
{
    private readonly ApiClient _apiClient;

    [ObservableProperty] private ObservableCollection<TaskDisplayItem> _tasks = new();
    [ObservableProperty] private string _filter = "all";
    [ObservableProperty] private string _searchText = "";
    [ObservableProperty] private string _summaryText = "";

    public TaskListViewModel(ApiClient apiClient)
    {
        _apiClient = apiClient;
    }

    public async Task LoadAsync()
    {
        try
        {
            var tasks = await _apiClient.GetAsync<List<TaskResponse>>("calendars/tasks");
            var taskLists = await _apiClient.GetAsync<List<TaskListResponse>>("calendars/task-lists");
            var listMap = (taskLists ?? new()).ToDictionary(tl => tl.Id, tl => tl.Name);

            var items = (tasks ?? new()).Select(t => new TaskDisplayItem
            {
                Id = t.Id.ToString(),
                Title = t.Title,
                Description = t.Description ?? "",
                TaskListName = t.TaskListId is { } lid && listMap.TryGetValue(lid, out var name) ? name : "",
                DurationMinutes = t.EstimatedDurationMinutes(),
                DtStart = t.DtStart,
                Due = t.Due,
                Priority = t.Priority,
                IsInbox = t.DtStart == null
            }).ToList();

            ApplyFilter(items);
            SummaryText = $"共 {items.Count} 个任务 · {items.Count(t => t.IsInbox)} 个未排程";
        }
        catch { }
    }

    private void ApplyFilter(List<TaskDisplayItem> source)
    {
        var filtered = Filter switch
        {
            "inbox" => source.Where(t => t.IsInbox),
            "high" => source.Where(t => t.Priority == 1),
            "today" => source.Where(t => t.DtStart?.Date == DateTime.Today),
            _ => source.AsEnumerable()
        };
        if (!string.IsNullOrWhiteSpace(SearchText))
            filtered = filtered.Where(t => t.Title.Contains(SearchText, StringComparison.OrdinalIgnoreCase));
        Tasks = new ObservableCollection<TaskDisplayItem>(filtered.OrderByDescending(t => t.Priority));
    }

    [RelayCommand]
    private void SetFilter(string filter)
    {
        Filter = filter;
        LoadAsync();
    }
}

public class TaskDisplayItem
{
    public string Id { get; set; } = "";
    public string Title { get; set; } = "";
    public string Description { get; set; } = "";
    public string TaskListName { get; set; } = "";
    public int? DurationMinutes { get; set; }
    public DateTimeOffset? DtStart { get; set; }
    public DateTimeOffset? Due { get; set; }
    public int Priority { get; set; }
    public bool IsInbox { get; set; }
}
```

- [ ] **Step 2: 重写 TaskListView.xaml**

```xml
<UserControl x:Class="Pim.Client.App.Views.TaskListView"
             xmlns="http://schemas.microsoft.com/winfx/2006/xaml/presentation"
             xmlns:x="http://schemas.microsoft.com/winfx/2006/xaml">
    <Grid>
        <Grid.RowDefinitions>
            <RowDefinition Height="Auto"/>
            <RowDefinition Height="Auto"/>
            <RowDefinition Height="*"/>
            <RowDefinition Height="Auto"/>
        </Grid.RowDefinitions>

        <!-- Header -->
        <Border Grid.Row="0" Padding="16,14" Background="{StaticResource CardBrush}"
                BorderBrush="{StaticResource BorderBrush}" BorderThickness="0,0,0,1">
            <Grid>
                <TextBlock Text="任务" FontSize="18" FontWeight="Bold"/>
                <TextBlock Text="{Binding SummaryText}" FontSize="12" Foreground="#888"
                           HorizontalAlignment="Right" VerticalAlignment="Center"/>
            </Grid>
        </Border>

        <!-- Filter Bar -->
        <Border Grid.Row="1" Padding="12,8" Background="{StaticResource CardBrush}"
                BorderBrush="{StaticResource BorderBrush}" BorderThickness="0,0,0,1">
            <Grid>
                <StackPanel Orientation="Horizontal">
                    <Button Content="全部" Command="{Binding SetFilterCommand}" CommandParameter="all"
                            Padding="8,4" FontSize="11" Margin="0,0,4,0">
                        <Button.Style>
                            <Style TargetType="Button" BasedOn="{StaticResource SecondaryButton}">
                                <Style.Triggers>
                                    <DataTrigger Binding="{Binding Filter}" Value="all">
                                        <Setter Property="Background" Value="{StaticResource PrimaryBrush}"/>
                                        <Setter Property="Foreground" Value="White"/>
                                    </DataTrigger>
                                </Style.Triggers>
                            </Style>
                        </Button.Style>
                    </Button>
                    <Button Content="未排程" Command="{Binding SetFilterCommand}" CommandParameter="inbox"
                            Padding="8,4" FontSize="11" Margin="0,0,4,0"
                            Style="{StaticResource SecondaryButton}"/>
                    <Button Content="高优先级" Command="{Binding SetFilterCommand}" CommandParameter="high"
                            Padding="8,4" FontSize="11" Margin="0,0,4,0"
                            Style="{StaticResource SecondaryButton}"/>
                    <Button Content="今日" Command="{Binding SetFilterCommand}" CommandParameter="today"
                            Padding="8,4" FontSize="11"
                            Style="{StaticResource SecondaryButton}"/>
                </StackPanel>
                <TextBox Text="{Binding SearchText, UpdateSourceTrigger=PropertyChanged}"
                         Width="180" FontSize="12" Padding="8,6"
                         HorizontalAlignment="Right" Style="{StaticResource ModernTextBox}"/>
            </Grid>
        </Border>

        <!-- Task List -->
        <ScrollViewer Grid.Row="2">
            <ItemsControl ItemsSource="{Binding Tasks}">
                <ItemsControl.ItemTemplate>
                    <DataTemplate>
                        <Border Padding="14,10" BorderBrush="#F0F0F0" BorderThickness="0,0,0,1"
                                Background="{StaticResource CardBrush}">
                            <Grid>
                                <Grid.ColumnDefinitions>
                                    <ColumnDefinition Width="Auto"/>
                                    <ColumnDefinition Width="*"/>
                                    <ColumnDefinition Width="Auto"/>
                                    <ColumnDefinition Width="Auto"/>
                                    <ColumnDefinition Width="Auto"/>
                                    <ColumnDefinition Width="Auto"/>
                                    <ColumnDefinition Width="Auto"/>
                                </Grid.ColumnDefinitions>

                                <!-- Priority Dot -->
                                <Ellipse Grid.Column="0" Width="10" Height="10" Margin="0,0,10,0">
                                    <Ellipse.Fill>
                                        <SolidColorBrush Color="{Binding Priority, Converter={StaticResource PriorityToColor}}"/>
                                    </Ellipse.Fill>
                                </Ellipse>

                                <!-- Title + Description -->
                                <StackPanel Grid.Column="1">
                                    <TextBlock Text="{Binding Title}" FontSize="13" FontWeight="SemiBold"/>
                                    <TextBlock Text="{Binding Description}" FontSize="10" Foreground="#999"
                                               TextTrimming="CharacterEllipsis" MaxLines="1"/>
                                </StackPanel>

                                <!-- Task List -->
                                <Border Grid.Column="2" Padding="4,2" CornerRadius="4"
                                        Background="#F5F5F5" Margin="8,0">
                                    <TextBlock Text="{Binding TaskListName}" FontSize="10" Foreground="#888"/>
                                </Border>

                                <!-- Duration -->
                                <TextBlock Grid.Column="3" Margin="8,0" FontSize="11" Foreground="#888"
                                           Text="{Binding DurationMinutes, Converter={StaticResource MinutesToDurationLabel}}"
                                           VerticalAlignment="Center"/>

                                <!-- Schedule Time / Due -->
                                <TextBlock Grid.Column="4" Margin="8,0" FontSize="11"
                                           VerticalAlignment="Center"
                                           Text="{Binding DtStart, StringFormat='🕐 {0:MM/dd HH:mm}'}"
                                           Foreground="#43A047"/>
                                <TextBlock Grid.Column="4" Margin="8,0" FontSize="11"
                                           VerticalAlignment="Center"
                                           Text="{Binding Due, StringFormat='📅 {0:MM/dd}'}"
                                           Foreground="#E53935"/>

                                <!-- Priority Label -->
                                <Border Grid.Column="5" Padding="5,2" CornerRadius="8" Margin="4,0"
                                        Background="{Binding Priority, Converter={StaticResource PriorityToColor}}">
                                    <TextBlock Text="{Binding Priority}" FontSize="10" Foreground="White"/>
                                </Border>

                                <!-- Status -->
                                <Border Grid.Column="6" Padding="5,2" CornerRadius="8" Margin="4,0">
                                    <Border.Style>
                                        <Style TargetType="Border">
                                            <Setter Property="Background" Value="#E8F5E9"/>
                                            <Style.Triggers>
                                                <DataTrigger Binding="{Binding IsInbox}" Value="True">
                                                    <Setter Property="Background" Value="#F5F5F5"/>
                                                </DataTrigger>
                                            </Style.Triggers>
                                        </Style>
                                    </Border.Style>
                                    <TextBlock FontSize="10" VerticalAlignment="Center">
                                        <TextBlock.Style>
                                            <Style TargetType="TextBlock">
                                                <Setter Property="Text" Value="已排程"/>
                                                <Setter Property="Foreground" Value="#43A047"/>
                                                <Style.Triggers>
                                                    <DataTrigger Binding="{Binding IsInbox}" Value="True">
                                                        <Setter Property="Text" Value="收件箱"/>
                                                        <Setter Property="Foreground" Value="#888"/>
                                                    </DataTrigger>
                                                </Style.Triggers>
                                            </Style>
                                        </TextBlock.Style>
                                    </TextBlock>
                                </Border>

                                <TextBlock Grid.Column="7" Text="›" FontSize="16" Foreground="#CCC"
                                           VerticalAlignment="Center" Margin="8,0,0,0"/>
                            </Grid>
                        </Border>
                    </DataTemplate>
                </ItemsControl.ItemTemplate>
            </ItemsControl>
        </ScrollViewer>

        <!-- Bottom Actions -->
        <Border Grid.Row="3" Padding="14,10" Background="{StaticResource CardBrush}"
                BorderBrush="{StaticResource BorderBrush}" BorderThickness="0,1,0,0">
            <Button Content="+ 新建任务" Style="{StaticResource PrimaryButton}"
                    HorizontalAlignment="Left" Padding="16,8" FontSize="13"/>
        </Border>
    </Grid>
</UserControl>
```

- [ ] **Step 3: 构建验证并 Commit**

```bash
dotnet build src/client-windows/Pim.Client.App/Pim.Client.App.csproj
git add src/client-windows/Pim.Client.App/ViewModels/TaskListViewModel.cs src/client-windows/Pim.Client.App/Views/TaskListView.xaml src/client-windows/Pim.Client.App/Views/TaskListView.xaml.cs
git commit -m "feat: rewrite TaskListView with filter chips, search, and rich task rows

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

### Task 11: 创建收件箱面板 (InboxPanel + InboxPanelViewModel)

**Files:**
- Create: `src/client-windows/Pim.Client.App/ViewModels/InboxPanelViewModel.cs`
- Create: `src/client-windows/Pim.Client.App/Views/InboxPanel.xaml`
- Create: `src/client-windows/Pim.Client.App/Views/InboxPanel.xaml.cs`

- [ ] **Step 1: 编写 InboxPanelViewModel**

```csharp
using System.Collections.ObjectModel;
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using Pim.Client.Core.Models;
using Pim.Client.Core.Services;

namespace Pim.Client.App.ViewModels;

public partial class InboxPanelViewModel : ObservableObject
{
    private readonly ApiClient _apiClient;

    [ObservableProperty] private ObservableCollection<InboxTaskItem> _items = new();
    [ObservableProperty] private bool _isEmpty;
    [ObservableProperty] private string _emptyText = "所有任务均已排入日程";

    public InboxPanelViewModel(ApiClient apiClient)
    {
        _apiClient = apiClient;
    }

    public async Task LoadAsync()
    {
        try
        {
            var tasks = await _apiClient.GetAsync<List<TaskResponse>>("calendars/tasks");
            var unscheduled = (tasks ?? new()).Where(t => t.DtStart == null).ToList();
            var taskLists = await _apiClient.GetAsync<List<TaskListResponse>>("calendars/task-lists");
            var listMap = (taskLists ?? new()).ToDictionary(tl => tl.Id, tl => tl.Name);

            Items = new ObservableCollection<InboxTaskItem>(
                unscheduled.Select(t => new InboxTaskItem
                {
                    Id = t.Id.ToString(),
                    Title = t.Title,
                    TaskListName = t.TaskListId is { } lid && listMap.TryGetValue(lid, out var n) ? n : "",
                    DurationMinutes = t.EstimatedDurationMinutes(),
                    Due = t.Due,
                    Priority = t.Priority
                }));
            IsEmpty = Items.Count == 0;
            EmptyText = IsEmpty ? "所有任务均已排入日程" : "";
        }
        catch { }
    }
}

public class InboxTaskItem
{
    public string Id { get; set; } = "";
    public string Title { get; set; } = "";
    public string TaskListName { get; set; } = "";
    public int? DurationMinutes { get; set; }
    public DateTimeOffset? Due { get; set; }
    public int Priority { get; set; }
}
```

- [ ] **Step 2: 编写 InboxPanel.xaml**

```xml
<UserControl x:Class="Pim.Client.App.Views.InboxPanel"
             xmlns="http://schemas.microsoft.com/winfx/2006/xaml/presentation"
             xmlns:x="http://schemas.microsoft.com/winfx/2006/xaml">
    <Border Background="{StaticResource NavBgBrush}"
            BorderBrush="{StaticResource BorderBrush}" BorderThickness="1,0,0,0">
        <Grid>
            <Grid.RowDefinitions>
                <RowDefinition Height="Auto"/>
                <RowDefinition Height="*"/>
                <RowDefinition Height="Auto"/>
            </Grid.RowDefinitions>

            <!-- Header -->
            <Border Grid.Row="0" Padding="14,12"
                    BorderBrush="{StaticResource BorderBrush}" BorderThickness="0,0,0,1">
                <Grid>
                    <StackPanel Orientation="Horizontal">
                        <TextBlock Text="📥" FontSize="14" VerticalAlignment="Center"/>
                        <TextBlock Text=" 收件箱 / 未排程" FontSize="13" FontWeight="SemiBold"
                                   VerticalAlignment="Center" Margin="4,0,0,0"/>
                    </StackPanel>
                    <TextBlock Text="ⓘ" FontSize="12" Foreground="#AAA"
                               HorizontalAlignment="Right" VerticalAlignment="Center"
                               ToolTip="长按任务卡片可拖拽到时间轴排程"/>
                </Grid>
            </Border>

            <!-- Task Cards -->
            <ScrollViewer Grid.Row="1" VerticalScrollBarVisibility="Auto">
                <StackPanel Margin="8">
                    <!-- Empty State -->
                    <StackPanel VerticalAlignment="Center" HorizontalAlignment="Center"
                                Margin="0,40,0,0"
                                Visibility="{Binding IsEmpty, Converter={StaticResource BoolToVisibility}}">
                        <TextBlock Text="✓" FontSize="48" Foreground="#CCC"
                                   HorizontalAlignment="Center"/>
                        <TextBlock Text="{Binding EmptyText}" FontSize="13" Foreground="#999"
                                   HorizontalAlignment="Center" Margin="0,8,0,0"/>
                    </StackPanel>

                    <!-- Task Items -->
                    <ItemsControl ItemsSource="{Binding Items}">
                        <ItemsControl.ItemTemplate>
                            <DataTemplate>
                                <Border Background="{StaticResource CardBrush}" CornerRadius="10"
                                        BorderBrush="#F0F0F0" BorderThickness="1"
                                        Padding="12" Margin="0,3">
                                    <StackPanel>
                                        <Grid>
                                            <Grid.ColumnDefinitions>
                                                <ColumnDefinition Width="Auto"/>
                                                <ColumnDefinition Width="*"/>
                                                <ColumnDefinition Width="Auto"/>
                                            </Grid.ColumnDefinitions>
                                            <Ellipse Grid.Column="0" Width="10" Height="10" Margin="0,0,8,0">
                                                <Ellipse.Fill>
                                                    <SolidColorBrush Color="{Binding Priority, Converter={StaticResource PriorityToColor}}"/>
                                                </Ellipse.Fill>
                                            </Ellipse>
                                            <TextBlock Grid.Column="1" Text="{Binding Title}" FontSize="13"
                                                       FontWeight="Medium" MaxLines="2"
                                                       TextTrimming="CharacterEllipsis"/>
                                            <TextBlock Grid.Column="2" Text="⠿" FontSize="14" Foreground="#CCC"/>
                                        </Grid>
                                        <StackPanel Orientation="Horizontal" Margin="0,6,0,0">
                                            <Border Padding="4,2" CornerRadius="4" Background="#F5F5F5" Margin="0,0,6,0">
                                                <TextBlock Text="{Binding TaskListName}" FontSize="10" Foreground="#888"/>
                                            </Border>
                                            <Border Padding="4,2" CornerRadius="4" Background="#F5F5F5" Margin="0,0,6,0">
                                                <TextBlock FontSize="10" Foreground="#888"
                                                           Text="{Binding DurationMinutes, Converter={StaticResource MinutesToDurationLabel}}"/>
                                            </Border>
                                            <Border Padding="4,2" CornerRadius="4" Background="#FFF0F0" Margin="0,0,6,0">
                                                <TextBlock FontSize="10" Foreground="#E53935"
                                                           Text="{Binding Due, StringFormat='📅 {0:MM/dd}'}"/>
                                            </Border>
                                        </StackPanel>
                                    </StackPanel>
                                </Border>
                            </DataTemplate>
                        </ItemsControl.ItemTemplate>
                    </ItemsControl>
                </StackPanel>
            </ScrollViewer>

            <!-- Bottom Buttons -->
            <Border Grid.Row="2" Padding="12,10"
                    BorderBrush="{StaticResource BorderBrush}" BorderThickness="0,1,0,0">
                <StackPanel>
                    <Button Content="+ 新建" Style="{StaticResource PrimaryButton}"
                            Padding="0,10" FontSize="13" Margin="0,0,0,6"/>
                    <Button Content="⚡ 一键重排" Style="{StaticResource SecondaryButton}"
                            Padding="0,10" FontSize="13"/>
                </StackPanel>
            </Border>
        </Grid>
    </Border>
</UserControl>
```

- [ ] **Step 3: 编写 InboxPanel.xaml.cs**

```csharp
using System.Windows.Controls;

namespace Pim.Client.App.Views;

public partial class InboxPanel : UserControl
{
    public InboxPanel() => InitializeComponent();
}
```

- [ ] **Step 4: 构建验证并 Commit**

```bash
dotnet build src/client-windows/Pim.Client.App/Pim.Client.App.csproj
git add src/client-windows/Pim.Client.App/ViewModels/InboxPanelViewModel.cs src/client-windows/Pim.Client.App/Views/InboxPanel.xaml src/client-windows/Pim.Client.App/Views/InboxPanel.xaml.cs
git commit -m "feat: add InboxPanel with unscheduled task cards and action buttons

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

### Task 12: 创建日程编辑器对话框 (EventEditorDialog + EventEditorViewModel)

**Files:**
- Create: `src/client-windows/Pim.Client.App/ViewModels/EventEditorViewModel.cs`
- Create: `src/client-windows/Pim.Client.App/Views/EventEditorDialog.xaml`
- Create: `src/client-windows/Pim.Client.App/Views/EventEditorDialog.xaml.cs`

- [ ] **Step 1: 编写 EventEditorViewModel**

```csharp
using System.Collections.ObjectModel;
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using Pim.Client.Core.Models;
using Pim.Client.Core.Services;

namespace Pim.Client.App.ViewModels;

public partial class EventEditorViewModel : ObservableObject
{
    private readonly ApiClient _apiClient;

    [ObservableProperty] private string _title = "";
    [ObservableProperty] private bool _isAllDay;
    [ObservableProperty] private DateTime _startDate = DateTime.Today;
    [ObservableProperty] private string _startTime = "09:00";
    [ObservableProperty] private DateTime _endDate = DateTime.Today;
    [ObservableProperty] private string _endTime = "10:00";
    [ObservableProperty] private string _location = "";
    [ObservableProperty] private string _description = "";
    [ObservableProperty] private string _colorHex = "#6B5EE4";
    [ObservableProperty] private string _status = "CONFIRMED";
    [ObservableProperty] private string? _rrule;
    [ObservableProperty] private bool _isBlock;
    [ObservableProperty] private Guid? _selectedCalendarId;
    [ObservableProperty] private ObservableCollection<CalendarResponse> _calendars = new();
    [ObservableProperty] private string? _errorMessage;
    [ObservableProperty] private bool _isSaving;
    [ObservableProperty] private bool _isEditing;
    [ObservableProperty] private string _dialogTitle = "新建日程";

    public Guid? EventId { get; set; }

    public List<string> ColorPalette => new()
    { "#6B5EE4", "#0EA8A0", "#E91E63", "#FF9800", "#2196F3", "#4CAF50", "#E53935" };

    public List<StatusOption> StatusOptions => new()
    {
        new("已确认", "CONFIRMED"),
        new("暂定", "TENTATIVE"),
        new("已取消", "CANCELLED")
    };

    public List<RepeatOption> RepeatOptions => new()
    {
        new("不重复", null),
        new("每天", "FREQ=DAILY"),
        new("每周", "FREQ=WEEKLY"),
        new("每月", "FREQ=MONTHLY"),
        new("每年", "FREQ=YEARLY")
    };

    public EventEditorViewModel(ApiClient apiClient)
    {
        _apiClient = apiClient;
    }

    public async Task LoadCalendarsAsync()
    {
        try
        {
            var list = await _apiClient.GetAsync<List<CalendarResponse>>("calendars");
            Calendars = new ObservableCollection<CalendarResponse>(list ?? new());
            if (SelectedCalendarId == null && Calendars.Count > 0)
                SelectedCalendarId = Calendars[0].Id;
        }
        catch { }
    }

    public void LoadEvent(EventResponse evt)
    {
        EventId = evt.Id;
        IsEditing = true;
        DialogTitle = "编辑日程";
        Title = evt.Title;
        StartDate = evt.Start.Date;
        StartTime = evt.Start.ToString("HH:mm");
        EndDate = (evt.End ?? evt.Start.AddHours(1)).Date;
        EndTime = (evt.End ?? evt.Start.AddHours(1)).ToString("HH:mm");
        Location = evt.Location ?? "";
        Description = evt.Description ?? "";
        ColorHex = evt.ColorHex ?? "#6B5EE4";
        Status = evt.Status ?? "CONFIRMED";
        Rrule = evt.Rrule;
        SelectedCalendarId = evt.CalendarId;
    }

    [RelayCommand]
    private void SelectColor(string hex) => ColorHex = hex;

    [RelayCommand]
    private void SelectStatus(string status) => Status = status;

    [RelayCommand]
    private void SelectRepeat(string? rrule) => Rrule = rrule;

    [RelayCommand]
    private async Task Save()
    {
        ErrorMessage = null;
        if (string.IsNullOrWhiteSpace(Title))
        {
            ErrorMessage = "请输入日程标题";
            return;
        }
        if (SelectedCalendarId == null)
        {
            ErrorMessage = "请选择日历本";
            return;
        }

        IsSaving = true;
        try
        {
            var start = StartDate.Date + TimeSpan.Parse(StartTime);
            var end = EndDate.Date + TimeSpan.Parse(EndTime);
            if (end <= start) end = start.AddHours(1);

            var payload = new
            {
                calendarId = SelectedCalendarId,
                title = Title.Trim(),
                description = string.IsNullOrWhiteSpace(Description) ? null : Description.Trim(),
                location = string.IsNullOrWhiteSpace(Location) ? null : Location.Trim(),
                start,
                end,
                colorHex = ColorHex,
                status = Status,
                rrule = Rrule,
                isBlock = IsBlock
            };

            var result = IsEditing
                ? await _apiClient.PutAsync<EventResponse>($"calendars/events/{EventId}", payload)
                : await _apiClient.PostAsync<EventResponse>("calendars/events", payload);

            Saved?.Invoke(result);
        }
        finally { IsSaving = false; }
    }

    public event Action<EventResponse?>? Saved;
}

public record StatusOption(string Label, string Value);
public record RepeatOption(string Label, string? Value);
```

- [ ] **Step 2: 编写 EventEditorDialog.xaml**

```xml
<UserControl x:Class="Pim.Client.App.Views.EventEditorDialog"
             xmlns="http://schemas.microsoft.com/winfx/2006/xaml/presentation"
             xmlns:x="http://schemas.microsoft.com/winfx/2006/xaml">
    <Border CornerRadius="16" Background="{StaticResource CardBrush}"
            Padding="24" Width="560" MaxHeight="700">
        <ScrollViewer VerticalScrollBarVisibility="Auto">
            <StackPanel>
                <!-- Title -->
                <TextBlock Text="{Binding DialogTitle}" FontSize="18" FontWeight="Bold" Margin="0,0,0,16"/>
                <TextBox Text="{Binding Title, UpdateSourceTrigger=PropertyChanged}"
                         FontSize="16" FontWeight="Medium" Padding="10"
                         Style="{StaticResource ModernTextBox}" Margin="0,0,0,12"/>

                <!-- Calendar Selector -->
                <ItemsControl ItemsSource="{Binding Calendars}" Margin="0,0,0,12">
                    <ItemsControl.ItemsPanel>
                        <ItemsPanelTemplate><WrapPanel/></ItemsPanelTemplate>
                    </ItemsControl.ItemsPanel>
                    <ItemsControl.ItemTemplate>
                        <DataTemplate>
                            <Button Content="{Binding Name}" Padding="10,6" Margin="0,0,6,4"
                                    FontSize="11" Command="{Binding DataContext.SelectCalendarCommand, RelativeSource={RelativeSource AncestorType=UserControl}}"
                                    CommandParameter="{Binding Id}"/>
                        </DataTemplate>
                    </ItemsControl.ItemTemplate>
                </ItemsControl>

                <!-- All-Day Toggle -->
                <CheckBox Content="全天" Margin="0,0,0,10"/>

                <!-- DateTime Pickers -->
                <StackPanel Margin="0,0,0,8">
                    <DatePicker SelectedDate="{Binding StartDate}" Margin="0,0,0,4"/>
                    <TextBox Text="{Binding StartTime, UpdateSourceTrigger=PropertyChanged}"
                             Style="{StaticResource ModernTextBox}" Margin="0,0,0,8"/>
                    <DatePicker SelectedDate="{Binding EndDate}" Margin="0,0,0,4"/>
                    <TextBox Text="{Binding EndTime, UpdateSourceTrigger=PropertyChanged}"
                             Style="{StaticResource ModernTextBox}" Margin="0,0,0,12"/>
                </StackPanel>

                <!-- Location -->
                <TextBox Text="{Binding Location, UpdateSourceTrigger=PropertyChanged}"
                         Style="{StaticResource ModernTextBox}" Margin="0,0,0,10"/>

                <!-- Description -->
                <TextBox Text="{Binding Description, UpdateSourceTrigger=PropertyChanged}"
                         Style="{StaticResource ModernTextBox}" TextWrapping="Wrap"
                         AcceptsReturn="True" MinLines="3" Margin="0,0,0,12"/>

                <!-- Color Picker -->
                <ItemsControl ItemsSource="{Binding ColorPalette}" Margin="0,0,0,12">
                    <ItemsControl.ItemsPanel>
                        <ItemsPanelTemplate><WrapPanel/></ItemsPanelTemplate>
                    </ItemsControl.ItemsPanel>
                    <ItemsControl.ItemTemplate>
                        <DataTemplate>
                            <Button Width="28" Height="28" Margin="0,0,6,0"
                                    Command="{Binding DataContext.SelectColorCommand, RelativeSource={RelativeSource AncestorType=UserControl}}"
                                    CommandParameter="{Binding .}">
                                <Button.Template>
                                    <ControlTemplate TargetType="Button">
                                        <Border Width="28" Height="28" CornerRadius="14"
                                                Background="{Binding .}"/>
                                    </ControlTemplate>
                                </Button.Template>
                            </Button>
                        </DataTemplate>
                    </ItemsControl.ItemTemplate>
                </ItemsControl>

                <!-- Status -->
                <ItemsControl ItemsSource="{Binding StatusOptions}" Margin="0,0,0,12">
                    <ItemsControl.ItemsPanel>
                        <ItemsPanelTemplate><WrapPanel/></ItemsPanelTemplate>
                    </ItemsControl.ItemsPanel>
                    <ItemsControl.ItemTemplate>
                        <DataTemplate>
                            <Button Content="{Binding Label}" Padding="10,6" Margin="0,0,6,4" FontSize="11"
                                    Command="{Binding DataContext.SelectStatusCommand, RelativeSource={RelativeSource AncestorType=UserControl}}"
                                    CommandParameter="{Binding Value}"/>
                        </DataTemplate>
                    </ItemsControl.ItemTemplate>
                </ItemsControl>

                <!-- Repeat -->
                <ItemsControl ItemsSource="{Binding RepeatOptions}" Margin="0,0,0,12">
                    <ItemsControl.ItemsPanel>
                        <ItemsPanelTemplate><WrapPanel/></ItemsPanelTemplate>
                    </ItemsControl.ItemsPanel>
                    <ItemsControl.ItemTemplate>
                        <DataTemplate>
                            <Button Content="{Binding Label}" Padding="10,6" Margin="0,0,6,4" FontSize="11"
                                    Command="{Binding DataContext.SelectRepeatCommand, RelativeSource={RelativeSource AncestorType=UserControl}}"
                                    CommandParameter="{Binding Value}"/>
                        </DataTemplate>
                    </ItemsControl.ItemTemplate>
                </ItemsControl>

                <!-- Block Auto-Schedule -->
                <CheckBox Content="阻挡自动排程（开启后此时间段不会被自动排入任务）"
                          IsChecked="{Binding IsBlock}" Margin="0,0,0,14"/>

                <!-- Error Message -->
                <TextBlock Text="{Binding ErrorMessage}" Foreground="{StaticResource DangerBrush}"
                           FontSize="12" Margin="0,0,0,8"
                           Visibility="{Binding ErrorMessage, Converter={StaticResource StringNotEmpty}}"/>

                <!-- Action Buttons -->
                <StackPanel Orientation="Horizontal" HorizontalAlignment="Right">
                    <Button Content="保存" Style="{StaticResource PrimaryButton}"
                            Command="{Binding SaveCommand}" Margin="0,0,8,0"/>
                </StackPanel>
            </StackPanel>
        </ScrollViewer>
    </Border>
</UserControl>
```

- [ ] **Step 3: 编写 EventEditorDialog.xaml.cs** (minimal code-behind)

```csharp
using System.Windows.Controls;

namespace Pim.Client.App.Views;

public partial class EventEditorDialog : UserControl
{
    public EventEditorDialog() => InitializeComponent();
}
```

- [ ] **Step 4: 构建验证并 Commit**

```bash
dotnet build src/client-windows/Pim.Client.App/Pim.Client.App.csproj
git add src/client-windows/Pim.Client.App/ViewModels/EventEditorViewModel.cs src/client-windows/Pim.Client.App/Views/EventEditorDialog.xaml src/client-windows/Pim.Client.App/Views/EventEditorDialog.xaml.cs
git commit -m "feat: add EventEditorDialog with full event editing fields

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

### Task 13: 创建任务编辑器对话框 (TaskEditorDialog + TaskEditorViewModel)

**Files:**
- Create: `src/client-windows/Pim.Client.App/ViewModels/TaskEditorViewModel.cs`
- Create: `src/client-windows/Pim.Client.App/Views/TaskEditorDialog.xaml`
- Create: `src/client-windows/Pim.Client.App/Views/TaskEditorDialog.xaml.cs`

- [ ] **Step 1: 编写 TaskEditorViewModel**

```csharp
using System.Collections.ObjectModel;
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using Pim.Client.Core.Models;
using Pim.Client.Core.Services;

namespace Pim.Client.App.ViewModels;

public partial class TaskEditorViewModel : ObservableObject
{
    private readonly ApiClient _apiClient;

    [ObservableProperty] private string _title = "";
    [ObservableProperty] private string _description = "";
    [ObservableProperty] private string _location = "";
    [ObservableProperty] private int _durationMinutes = 60;
    [ObservableProperty] private DateTime? _dueDate;
    [ObservableProperty] private string? _dueTime;
    [ObservableProperty] private int _priority = 2;
    [ObservableProperty] private string? _rrule;
    [ObservableProperty] private int _reminderMinutes = 15;
    [ObservableProperty] private bool _isAutoScheduled = true;
    [ObservableProperty] private bool _isSplittable;
    [ObservableProperty] private bool _isLocked;
    [ObservableProperty] private Guid? _selectedTaskListId;
    [ObservableProperty] private ObservableCollection<TaskListResponse> _taskLists = new();
    [ObservableProperty] private string? _errorMessage;
    [ObservableProperty] private bool _isSaving;
    [ObservableProperty] private bool _isEditing;
    [ObservableProperty] private string _dialogTitle = "新建任务";

    public List<int> DurationOptions => new() { 15, 30, 60, 90, 120, 180, 240, 360, 480 };
    public List<PriorityOption> PriorityOptions => new()
    {
        new("高", 1, "#E53935"),
        new("中", 2, "#FFA726"),
        new("低", 3, "#43A047")
    };
    public List<RepeatOption> RepeatOptions => new()
    {
        new("不重复", null),
        new("每天", "FREQ=DAILY"),
        new("每周", "FREQ=WEEKLY"),
        new("每月", "FREQ=MONTHLY")
    };
    public List<ReminderOption> ReminderOptions => new()
    {
        new("准时", 0), new("5分钟", 5), new("15分钟", 15),
        new("30分钟", 30), new("1小时", 60), new("1天", 1440)
    };

    public Guid? TaskId { get; set; }

    public TaskEditorViewModel(ApiClient apiClient)
    {
        _apiClient = apiClient;
    }

    public async Task LoadTaskListsAsync()
    {
        try
        {
            var list = await _apiClient.GetAsync<List<TaskListResponse>>("calendars/task-lists");
            TaskLists = new ObservableCollection<TaskListResponse>(list ?? new());
            if (SelectedTaskListId == null && TaskLists.Count > 0)
                SelectedTaskListId = TaskLists[0].Id;
        }
        catch { }
    }

    public void LoadTask(TaskResponse task)
    {
        TaskId = task.Id;
        IsEditing = true;
        DialogTitle = "编辑任务";
        Title = task.Title;
        Description = task.Description ?? "";
        Location = task.Location ?? "";
        DurationMinutes = task.EstimatedDurationMinutes() ?? 60;
        DueDate = task.Due?.Date;
        DueTime = task.Due?.ToString("HH:mm");
        Priority = task.Priority;
        Rrule = task.Rrule;
        SelectedTaskListId = task.TaskListId;
    }

    [RelayCommand]
    private void SelectDuration(int minutes) => DurationMinutes = minutes;

    [RelayCommand]
    private void SelectPriority(int value) => Priority = value;

    [RelayCommand]
    private void SelectRepeat(string? rrule) => Rrule = rrule;

    [RelayCommand]
    private void SelectReminder(int minutes) => ReminderMinutes = minutes;

    [RelayCommand]
    private async Task Save()
    {
        ErrorMessage = null;
        if (string.IsNullOrWhiteSpace(Title))
        {
            ErrorMessage = "请输入任务标题";
            return;
        }
        if (SelectedTaskListId == null)
        {
            ErrorMessage = "请选择任务本";
            return;
        }

        IsSaving = true;
        try
        {
            DateTimeOffset? due = DueDate.HasValue
                ? new DateTimeOffset(DueDate.Value.Date + (TimeSpan.TryParse(DueTime, out var t) ? t : TimeSpan.Zero))
                : null;

            var payload = new
            {
                taskListId = SelectedTaskListId,
                title = Title.Trim(),
                description = string.IsNullOrWhiteSpace(Description) ? null : Description.Trim(),
                location = string.IsNullOrWhiteSpace(Location) ? null : Location.Trim(),
                estimatedDuration = $"PT{DurationMinutes}M",
                priority = Priority,
                due = due,
                rrule = Rrule,
                reminderMinutesBefore = ReminderMinutes
            };

            var result = IsEditing
                ? await _apiClient.PutAsync<TaskResponse>($"calendars/tasks/{TaskId}", payload)
                : await _apiClient.PostAsync<TaskResponse>("calendars/tasks", payload);

            Saved?.Invoke(result);
        }
        finally { IsSaving = false; }
    }

    public event Action<TaskResponse?>? Saved;
}

public record PriorityOption(string Label, int Value, string Color);
public record ReminderOption(string Label, int Value);
```

- [ ] **Step 2: 编写 TaskEditorDialog.xaml**

```xml
<UserControl x:Class="Pim.Client.App.Views.TaskEditorDialog"
             xmlns="http://schemas.microsoft.com/winfx/2006/xaml/presentation"
             xmlns:x="http://schemas.microsoft.com/winfx/2006/xaml">
    <Border CornerRadius="16" Background="{StaticResource CardBrush}"
            Padding="24" Width="560" MaxHeight="700">
        <ScrollViewer VerticalScrollBarVisibility="Auto">
            <StackPanel>
                <TextBlock Text="{Binding DialogTitle}" FontSize="18" FontWeight="Bold" Margin="0,0,0,16"/>
                <TextBox Text="{Binding Title, UpdateSourceTrigger=PropertyChanged}"
                         FontSize="16" FontWeight="Medium" Padding="10"
                         Style="{StaticResource ModernTextBox}" Margin="0,0,0,10"/>

                <!-- Task List Selector -->
                <ItemsControl ItemsSource="{Binding TaskLists}" Margin="0,0,0,10">
                    <ItemsControl.ItemsPanel>
                        <ItemsPanelTemplate><WrapPanel/></ItemsPanelTemplate>
                    </ItemsControl.ItemsPanel>
                    <ItemsControl.ItemTemplate>
                        <DataTemplate>
                            <Button Content="{Binding Name}" Padding="8,4" Margin="0,0,6,4" FontSize="11"/>
                        </DataTemplate>
                    </ItemsControl.ItemTemplate>
                </ItemsControl>

                <TextBox Text="{Binding Description, UpdateSourceTrigger=PropertyChanged}"
                         Style="{StaticResource ModernTextBox}" TextWrapping="Wrap"
                         AcceptsReturn="True" MinLines="2" Margin="0,0,0,8"/>
                <TextBox Text="{Binding Location, UpdateSourceTrigger=PropertyChanged}"
                         Style="{StaticResource ModernTextBox}" Margin="0,0,0,10"/>

                <!-- Duration -->
                <TextBlock Text="预计时长" FontSize="11" Foreground="#888" Margin="0,0,0,4"/>
                <ItemsControl ItemsSource="{Binding DurationOptions}" Margin="0,0,0,12">
                    <ItemsControl.ItemsPanel>
                        <ItemsPanelTemplate><WrapPanel/></ItemsPanelTemplate>
                    </ItemsControl.ItemsPanel>
                    <ItemsControl.ItemTemplate>
                        <DataTemplate>
                            <Button Content="{Binding .}" Padding="8,4" Margin="0,0,6,4" FontSize="11"
                                    Command="{Binding DataContext.SelectDurationCommand, RelativeSource={RelativeSource AncestorType=UserControl}}"
                                    CommandParameter="{Binding .}"/>
                        </DataTemplate>
                    </ItemsControl.ItemTemplate>
                </ItemsControl>

                <!-- Priority -->
                <TextBlock Text="优先级" FontSize="11" Foreground="#888" Margin="0,0,0,4"/>
                <ItemsControl ItemsSource="{Binding PriorityOptions}" Margin="0,0,0,12">
                    <ItemsControl.ItemsPanel>
                        <ItemsPanelTemplate><WrapPanel/></ItemsPanelTemplate>
                    </ItemsControl.ItemsPanel>
                    <ItemsControl.ItemTemplate>
                        <DataTemplate>
                            <Button Content="{Binding Label}" Padding="10,6" Margin="0,0,6,4" FontSize="11"
                                    CommandParameter="{Binding Value}"
                                    Background="{Binding Color}"/>
                        </DataTemplate>
                    </ItemsControl.ItemTemplate>
                </ItemsControl>

                <!-- Toggles -->
                <CheckBox Content="自动排程" IsChecked="{Binding IsAutoScheduled}" Margin="0,0,0,6"/>
                <CheckBox Content="允许拆分" IsChecked="{Binding IsSplittable}" Margin="0,0,0,6"/>
                <CheckBox Content="锁定排程" IsChecked="{Binding IsLocked}" Margin="0,0,0,14"/>

                <!-- Error -->
                <TextBlock Text="{Binding ErrorMessage}" Foreground="{StaticResource DangerBrush}"
                           FontSize="12" Margin="0,0,0,8"/>

                <Button Content="保存" Style="{StaticResource PrimaryButton}"
                        Command="{Binding SaveCommand}" HorizontalAlignment="Right"/>
            </StackPanel>
        </ScrollViewer>
    </Border>
</UserControl>
```

- [ ] **Step 3: 构建验证并 Commit**

```bash
dotnet build src/client-windows/Pim.Client.App/Pim.Client.App.csproj
git add src/client-windows/Pim.Client.App/ViewModels/TaskEditorViewModel.cs src/client-windows/Pim.Client.App/Views/TaskEditorDialog.xaml src/client-windows/Pim.Client.App/Views/TaskEditorDialog.xaml.cs
git commit -m "feat: add TaskEditorDialog with task editing fields

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

### Task 14: 集成测试与编译验证

- [ ] **Step 1: 完整构建**

```bash
dotnet build src/client-windows/Pim.Client.App/Pim.Client.App.csproj --no-restore
```

Expected: Build succeeded with 0 errors.

- [ ] **Step 2: 编译整个解决方案**

```bash
dotnet build
```

- [ ] **Step 3: 修复编译错误（如有）**

仔细检查每个编译错误并修复。常见问题：
- 命名空间引用缺失
- XAML 绑定路径错误
- 转换器 Key 不匹配
- Model 属性名称与 API 返回不一致

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "fix: resolve build errors from calendar UI redesign integration"
```

---

### Task 15: 最终验证清单

- [ ] 启动应用，验证登录后进入新的 Shell 布局
- [ ] 左侧导航 4 个视图可切换
- [ ] 时间轴视图显示日期条、时间网格、计划/实际栏
- [ ] 本周视图显示 7 天列和周范围
- [ ] 月视图显示日历网格和选中日预览
- [ ] 任务视图显示筛选栏和任务列表
- [ ] 收件箱面板显示未排程任务
- [ ] 新建/编辑对话框功能正常

---

### File Map (Summary)

| File | Action | Responsibility |
|------|--------|----------------|
| `Pim.Client.App.csproj` | Modify | Add MaterialDesignThemes NuGet |
| `App.xaml` | Modify | Add MD resource dictionary + converters |
| `Styles/Theme.xaml` | Modify | Updated color palette + styles |
| `Converters/Converters.cs` | Modify | Add 6 converters |
| `MainWindow.xaml` | Rewrite | Shell layout |
| `MainWindow.xaml.cs` | Modify | Adapt to ShellViewModel |
| `Startup.cs` | Modify | Update DI registrations |
| `App.xaml.cs` | Modify | Replace MainViewModel→ShellViewModel |
| `ViewModels/ShellViewModel.cs` | Create | Nav + date + calendar list |
| `ViewModels/TimelineViewModel.cs` | Create | Day view logic |
| `ViewModels/WeekViewModel.cs` | Create | Week view logic |
| `ViewModels/MonthViewModel.cs` | Create | Month view logic |
| `ViewModels/TaskListViewModel.cs` | Rewrite | Task list with filter |
| `ViewModels/InboxPanelViewModel.cs` | Create | Unscheduled tasks |
| `ViewModels/EventEditorViewModel.cs` | Create | Event CRUD |
| `ViewModels/TaskEditorViewModel.cs` | Create | Task CRUD |
| `Views/TimelineView.xaml` + `.cs` | Create | Timeline UI |
| `Views/WeekView.xaml` + `.cs` | Create | Week UI |
| `Views/MonthView.xaml` + `.cs` | Create | Month UI |
| `Views/TaskListView.xaml` + `.cs` | Rewrite | Task list UI |
| `Views/InboxPanel.xaml` + `.cs` | Create | Inbox panel UI |
| `Views/EventEditorDialog.xaml` + `.cs` | Create | Event editor UI |
| `Views/TaskEditorDialog.xaml` + `.cs` | Create | Task editor UI |
