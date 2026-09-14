using System;
using System.Threading.Tasks;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Logging.Abstractions;
using Microsoft.Extensions.Options;
using Npgsql;
using Pim.Core.Invariants;
using Pim.Infrastructure.Data;
using Pim.Infrastructure.Operations;
using Xunit;
using Xunit.Abstractions;

namespace Pim.UnitTests.Invariants;

public class LiveDbQualityInspectionTests
{
    private readonly ITestOutputHelper _output;

    public LiveDbQualityInspectionTests(ITestOutputHelper output)
    {
        _output = output;
    }

    [Fact]
    public async Task LiveDb_InspectAsync_VerifiesGroundTruthViolations()
    {
        const string connString = "Host=127.0.0.1;Port=5432;Database=pim;Username=opencode;Password=62f0a50bb963bb648f8e400399def95a";

        // 检查数据库是否可连接
        try
        {
            await using var testConn = new NpgsqlConnection(connString);
            await testConn.OpenAsync();
        }
        catch (Exception ex)
        {
            _output.WriteLine($"本地真实 PostgreSQL 未运行或连接失败: {ex.Message}，跳过实机校验");
            return;
        }

        var optionsBuilder = new DbContextOptionsBuilder<PimDbContext>();
        optionsBuilder.UseNpgsql(connString);

        await using var db = new PimDbContext(optionsBuilder.Options);

        var options = Options.Create(new InvariantOptions());
        var inspector = new DataReliabilityQualityInspector(db, options, NullLogger<DataReliabilityQualityInspector>.Instance);

        var result = await inspector.InspectAsync(DateTimeOffset.UtcNow);

        _output.WriteLine($"=== 数据可靠性实机体检结果 ===");
        _output.WriteLine($"Healthy: {result.IsHealthy}");
        _output.WriteLine($"Issues: {result.IssueCount}");
        _output.WriteLine($"Message: {result.Message}");
        _output.WriteLine($"Details Count: {result.Details.Count}");

        foreach (var kvp in result.Details)
        {
            _output.WriteLine($"[{kvp.Key}] => {kvp.Value}");
        }

        // 必须不健康（真实生产数据存在违规，绝不得为假绿灯）
        Assert.False(result.IsHealthy);

        // 验证评审指出的必须判红的基准尺子：S2, S4, S6, S7, S10, S11, S12
        Assert.Contains("S2_INV-P17", result.Details.Keys);
        Assert.Contains("S4_INV-C18", result.Details.Keys);
        Assert.Contains("S6_INV-P20", result.Details.Keys);
        Assert.Contains("S7_INV-P21", result.Details.Keys);
        Assert.Contains("S10_INV-C21", result.Details.Keys);
        Assert.Contains("S11_INV-M21", result.Details.Keys);
        Assert.Contains("S12_INV-M22", result.Details.Keys);

        Assert.StartsWith("🔴 FAIL", result.Details["S2_INV-P17"]);
        Assert.StartsWith("🔴 FAIL", result.Details["S4_INV-C18"]);
        Assert.StartsWith("🔴 FAIL", result.Details["S6_INV-P20"]);
        Assert.StartsWith("🔴 FAIL", result.Details["S7_INV-P21"]);
        Assert.StartsWith("🔴 FAIL", result.Details["S10_INV-C21"]);
        Assert.StartsWith("🔴 FAIL", result.Details["S11_INV-M21"]);
        Assert.StartsWith("🔴 FAIL", result.Details["S12_INV-M22"]);
    }
}
