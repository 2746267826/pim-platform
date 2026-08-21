using Pim.Shell.App;
using Xunit;

public class ServerAddressTests
{
    [Theory]
    [InlineData("pim.example.com", "https://pim.example.com")]
    [InlineData("https://pim.example.com/", "https://pim.example.com")]
    [InlineData("http://192.168.1.10:5858", "http://192.168.1.10:5858")]
    public void NormalizeAcceptsValidAddresses(string input, string expected)
        => Assert.Equal(expected, ServerAddress.Normalize(input));

    [Theory]
    [InlineData("")]
    [InlineData("   ")]
    [InlineData("ftp://example.com")]
    public void NormalizeRejectsInvalidAddresses(string input)
        => Assert.Null(ServerAddress.Normalize(input));

    [Fact]
    public void InsecureDetection()
    {
        Assert.True(ServerAddress.IsInsecure("http://192.168.1.10:5858"));
        Assert.False(ServerAddress.IsInsecure("https://pim.example.com"));
    }
}
