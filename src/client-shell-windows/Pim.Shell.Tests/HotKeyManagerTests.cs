using Pim.Shell.App;
using Xunit;

public class HotKeyManagerTests
{
    [Fact]
    public void HotKeyIdIsStableForSameModifierAndKey()
    {
        Assert.Equal(HotKeyManager.BuildId(2, 0x4E), HotKeyManager.BuildId(2, 0x4E));
        Assert.NotEqual(HotKeyManager.BuildId(2, 0x4E), HotKeyManager.BuildId(1, 0x4E));
    }
    [Fact]
    public void DefaultShortcutIsCtrlAltN()
    {
        Assert.Equal(0x4E, HotKeyManager.DefaultKey);
        Assert.Equal(0x0002 | 0x0001, HotKeyManager.DefaultModifiers); // MOD_CONTROL | MOD_ALT
    }
}
