using Pim.Shell.App;
using Xunit;

public class UpdateCheckerTests
{
    [Fact]
    public void IsNewerReturnsTrueWhenRemoteIsNewer()
    {
        Assert.True(UpdateChecker.IsNewer("1.0.0", "1.0.1"));
        Assert.False(UpdateChecker.IsNewer("1.0.1", "1.0.1"));
        Assert.False(UpdateChecker.IsNewer(null, "1.0.1")); // 未配置视为无新版
    }
}
