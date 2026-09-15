using Npgsql;
using Xunit;

namespace Pim.UnitTests.Harness.RealDb;

/// <summary>
/// 真库用例的连接约定（与仓库既定标准一致）：
///
/// 1. 只读环境变量 <c>PIM_TEST_CONN</c>，源码里不内置口令；
/// 2. 拿不到可用的库时用 <see cref="Skip"/> **显式跳过**（CI 报告里显示为 Skipped），
///    而不是静默 <c>return</c> —— 静默返回会让"什么都没验证"的用例长期以 Passed 计入，
///    断言也会退化成恒真式，失去回归防护意义。
///
/// 使用方必须是 <c>[SkippableFact]</c>/<c>[SkippableTheory]</c>，否则 Skip 会被当成失败。
/// </summary>
internal static class RealDbTestConnection
{
    /// <summary>环境变量里的连接串（未设置时为 null）。</summary>
    public static string? FromEnvironment => Environment.GetEnvironmentVariable("PIM_TEST_CONN");

    /// <summary>不可用时抛 <see cref="Xunit.SkipException"/>（用例显示为 Skipped）。</summary>
    public static void SkipIfUnavailable()
    {
        var connectionString = FromEnvironment;
        Skip.If(
            string.IsNullOrWhiteSpace(connectionString),
            "RealDb unavailable (PIM_TEST_CONN not set), skipping test.");

        try
        {
            using var connection = new NpgsqlConnection(connectionString);
            connection.Open();
            using var command = new NpgsqlCommand("SELECT 1", connection);
            command.ExecuteScalar();
        }
        catch (Exception ex)
        {
            throw new Xunit.SkipException($"RealDb connection failed: {ex.Message}");
        }
    }

    /// <summary>校验可用并返回连接串。</summary>
    public static string Require()
    {
        SkipIfUnavailable();
        return FromEnvironment!;
    }
}
