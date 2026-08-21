using Pim.Shell.App;
using Xunit;

public class ShellConfigTests
{
    [Fact]
    public void RoundTripsThroughFile()
    {
        var path = Path.Combine(Path.GetTempPath(), $"shell-config-{Guid.NewGuid():N}.json");
        try
        {
            new ShellConfig { ServerUrl = "https://pim.example.com" }.Save(path);
            Assert.Equal("https://pim.example.com", ShellConfig.Load(path).ServerUrl);
        }
        finally { if (File.Exists(path)) File.Delete(path); }
    }

    [Fact]
    public void CorruptedFileFallsBackToFreshConfig()
    {
        var path = Path.Combine(Path.GetTempPath(), $"shell-config-{Guid.NewGuid():N}.json");
        try
        {
            File.WriteAllText(path, "{ not json");
            Assert.Equal("", ShellConfig.Load(path).ServerUrl);
        }
        finally { if (File.Exists(path)) File.Delete(path); }
    }
}
