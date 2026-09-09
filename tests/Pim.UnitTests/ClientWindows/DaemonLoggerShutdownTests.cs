using Pim.Client.App.Services;
using Xunit;

namespace Pim.UnitTests.ClientWindows;

/// <summary>
/// Logger.Shutdown 安全性测试。本类故意不调用 Logger.Initialize()：
/// 未初始化时 Shutdown 必须是安全 no-op，且退出路径上可重复调用。
/// （Logger 经 Compile Link 编入测试程序集，与 Pim.Client.App 中的实例互不影响。）
/// </summary>
public class DaemonLoggerShutdownTests
{
    [Fact]
    public void Shutdown_WithoutInitialize_IsSafeNoOp()
    {
        var ex = Record.Exception(() => Logger.Shutdown());

        Assert.Null(ex);
    }

    [Fact]
    public void Shutdown_IsIdempotent_CanCallRepeatedly()
    {
        var ex = Record.Exception(() =>
        {
            Logger.Shutdown();
            Logger.Shutdown();
            Logger.Shutdown();
        });

        Assert.Null(ex);
    }

    [Fact]
    public void Write_AfterShutdown_FallsBackWithoutThrowing()
    {
        Logger.Shutdown();

        var ex = Record.Exception(() =>
        {
            Logger.Info("info after shutdown");
            Logger.Warn("warn after shutdown");
            Logger.Error("error after shutdown", new InvalidOperationException("boom"));
        });

        Assert.Null(ex);
    }
}
