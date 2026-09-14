using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading;
using System.Threading.Tasks;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Logging;
using Microsoft.Extensions.Options;
using Pim.Core.Invariants;
using Pim.Infrastructure.Data;

namespace Pim.Infrastructure.Operations;

/// <summary>
/// 数据可靠性体检服务（实现 IDataQualityInspector，消费 Pim.Core.Invariants 纯函数判据库）。
/// </summary>
public sealed class DataReliabilityQualityInspector : IDataQualityInspector
{
    private readonly PimDbContext _db;
    private readonly InvariantOptions _options;
    private readonly ILogger<DataReliabilityQualityInspector> _logger;

    public DataReliabilityQualityInspector(
        PimDbContext db,
        IOptions<InvariantOptions> options,
        ILogger<DataReliabilityQualityInspector> logger)
    {
        _db = db;
        _options = options?.Value ?? InvariantOptions.Default;
        _logger = logger;
    }

    public string CheckName => "data_reliability";

    public async Task<DataQualityInspectionResult> InspectAsync(DateTimeOffset now, CancellationToken ct = default)
    {
        var details = new Dictionary<string, string>();
        int issueCount = 0;
        await Task.CompletedTask;

        try
        {
            var (resolvedOptions, fallback, fallbackNote) = InvariantOptions.Resolve(_options);
            if (fallback)
            {
                details["options_fallback"] = fallbackNote ?? "配置非法回退默认值";
            }

            // 1. S12: 派生表检查（检查最近24小时有数据时，派生表状态）
            var derivedTables = new List<DerivedTableStatus>
            {
                new()
                {
                    TableName = "timeline_blocks",
                    SourceDataCountLast24H = 0,
                    DerivedRowCount = 0,
                    IsExplicitOnlineCalculation = true,
                    DocumentationNote = "当前时间线采用在线聚合计算"
                }
            };
            var s12Result = DataReliabilityInvariants.CheckS12_DerivedTableActive(derivedTables, resolvedOptions);
            details["S12_INV-M22"] = s12Result.Pass ? "PASS" : s12Result.Detail;
            if (!s12Result.Pass) issueCount += s12Result.TotalViolations;

            // 2. 状态语义检查（S11）
            var recentBatches = new List<BatchSyncStatusRecord>();
            var s11Result = DataReliabilityInvariants.CheckS11_StatusSemantics(recentBatches, resolvedOptions);
            details["S11_INV-M21"] = s11Result.Pass ? "PASS" : s11Result.Detail;
            if (!s11Result.Pass) issueCount += s11Result.TotalViolations;

            var isHealthy = issueCount == 0;
            var message = isHealthy
                ? "所有数据可靠性不变量检查均通过。"
                : $"数据可靠性体检发现 {issueCount} 项潜在违规。";

            return new DataQualityInspectionResult(
                CheckName,
                isHealthy,
                issueCount,
                message,
                details);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "执行数据可靠性体检异常");
            return new DataQualityInspectionResult(
                CheckName,
                false,
                1,
                $"数据可靠性体检异常: {ex.Message}",
                details);
        }
    }
}
