namespace Pim.Client.Core;

public static class ClientDefaults
{
    public static string DefaultServerUrl => Environment.GetEnvironmentVariable("PIM_SERVER_URL") ?? "http://127.0.0.1:5858";
    public static string AwBaseUrl => Environment.GetEnvironmentVariable("AW_BASE_URL") ?? "http://127.0.0.1:5600";
    public static string KeyStatsBaseUrl => Environment.GetEnvironmentVariable("KEYSTATS_BASE_URL") ?? "http://127.0.0.1:18080";
}
