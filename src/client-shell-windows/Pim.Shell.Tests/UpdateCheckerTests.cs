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

    [Theory]
    [InlineData("2026.08.9", "2026.08.10", true)]   // N 递增
    [InlineData("2026.08.10", "2026.08.9", false)]
    [InlineData("2026.08.12+android.1", "2026.08.12-pr.5+abc", false)] // 同 N 忽略后缀判相等
    [InlineData("2026.05.100", "2026.08.101", true)]
    public void IsNewer_ComparesOnlyLastSegment(string current, string remote, bool expected)
    {
        Assert.Equal(expected, UpdateChecker.IsNewer(current, remote));
    }

    [Fact]
    public void IsNewer_NullCurrent_ReturnsTrue_WhenRemotePresent()
    {
        Assert.True(UpdateChecker.IsNewer(null, "2026.08.212"));
        Assert.False(UpdateChecker.IsNewer("2026.08.212", null));
        Assert.False(UpdateChecker.IsNewer("2026.08.212", ""));
    }
}
