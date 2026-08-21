using Pim.Shell.App;
using Xunit;

public class UpdateCheckerTests
{
    [Fact]
    public void IsNewerReturnsTrueWhenRemoteIsNewer()
    {
        Assert.True(UpdateChecker.IsNewer("1.0.0", "1.0.1"));
        Assert.False(UpdateChecker.IsNewer("1.0.1", "1.0.1"));
        Assert.True(UpdateChecker.IsNewer(null, "1.0.1"));
    }
}
