using Pim.Shell.App;
using Xunit;

public class ServerHealthClientTests
{
    private sealed class StubHandler(Func<HttpRequestMessage, HttpResponseMessage> responder) : HttpMessageHandler
    {
        protected override Task<HttpResponseMessage> SendAsync(HttpRequestMessage request, CancellationToken cancellationToken)
            => Task.FromResult(responder(request));
    }

    [Fact]
    public async Task HealthyServerReturnsHealthyWithNormalizedUrl()
    {
        var client = new ServerHealthClient(new HttpClient(new StubHandler(_ => new HttpResponseMessage(System.Net.HttpStatusCode.OK))));
        var result = await client.CheckAsync("pim.example.com");
        Assert.Equal(HealthCheckStatus.Healthy, result.Status);
        Assert.Equal("https://pim.example.com", result.NormalizedUrl);
    }

    [Fact]
    public async Task ErrorStatusReturnsUnreachable()
    {
        var client = new ServerHealthClient(new HttpClient(new StubHandler(_ => new HttpResponseMessage(System.Net.HttpStatusCode.InternalServerError))));
        Assert.Equal(HealthCheckStatus.Unreachable, (await client.CheckAsync("https://pim.example.com")).Status);
    }

    [Fact]
    public async Task NetworkFailureReturnsUnreachable()
    {
        var client = new ServerHealthClient(new HttpClient(new StubHandler(_ => throw new HttpRequestException("offline"))));
        Assert.Equal(HealthCheckStatus.Unreachable, (await client.CheckAsync("https://pim.example.com")).Status);
    }

    [Fact]
    public async Task InvalidAddressDoesNotIssueRequest()
    {
        var requested = false;
        var handler = new StubHandler(_ => { requested = true; return new HttpResponseMessage(System.Net.HttpStatusCode.OK); });
        var result = await new ServerHealthClient(new HttpClient(handler)).CheckAsync("   ");
        Assert.Equal(HealthCheckStatus.Unreachable, result.Status);
        Assert.False(requested);
    }
}
