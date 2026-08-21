namespace Pim.Shell.App;

public static class ShellBridge
{
    // 契约见 docs/superpowers/specs/2026-08-21-pim-shell-clients-design.md 第 5 章
    public const string Script = "window.__PIM_SHELL__ = Object.freeze({ version: 1, platform: 'windows' });";
}
