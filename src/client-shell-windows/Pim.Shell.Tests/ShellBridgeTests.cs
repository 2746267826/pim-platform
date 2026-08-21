using Pim.Shell.App;
using Xunit;

public class ShellBridgeTests
{
    [Fact]
    public void ScriptDeclaresVersionOneAndPlatform()
    {
        Assert.Contains("__PIM_SHELL__", ShellBridge.Script);
        Assert.Contains("version: 1", ShellBridge.Script);
        Assert.Contains("platform: 'windows'", ShellBridge.Script);
    }
}
