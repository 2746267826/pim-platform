using System;
using System.Collections.Generic;
using System.Threading.Tasks;
using Microsoft.Extensions.Logging.Abstractions;
using Microsoft.Extensions.Options;
using Pim.Core.Invariants;
using Pim.Infrastructure.Operations;
using Xunit;

namespace Pim.UnitTests.Invariants;

/// <summary>
/// Issue #255 判据上移与配置化单元测试
/// 验收标准:
/// 1. 判据库位于 Pim.Core.Invariants，无 EF/DB 依赖
/// 2. 判据库被两处引用：测试工程 + 体检服务
/// 3. 改动阈值配置后，判定结果随之变化；非法配置回退默认并在结果中标注
/// </summary>
public class InvariantOptionsAndMigrationTests
{
    [Fact]
    public void Options_ValidConfiguration_PassesValidation()
    {
        var options = new InvariantOptions
        {
            MinInputDensityPerMinute = 1.0,
            LongEventThresholdMinutes = 30.0,
            UndeclaredOfflineGapMinutes = 30.0,
            MaxDailyActiveHours = 24.0,
            CoverageRedRatio = 0.95,
            CoverageYellowRatio = 0.99
        };

        bool isValid = options.Validate(out var error);
        Assert.True(isValid);
        Assert.Null(error);

        var (resolved, fallback, note) = InvariantOptions.Resolve(options);
        Assert.False(fallback);
        Assert.Null(note);
        Assert.Equal(30.0, resolved.LongEventThresholdMinutes);
    }

    [Fact]
    public void Options_IllegalConfiguration_FallsBackToDefaultsWithNote()
    {
        // 设置非法阈值：例如单日最大时长为负数，覆盖率红线大于 1.0
        var illegalOptions = new InvariantOptions
        {
            MaxDailyActiveHours = -5.0,
            CoverageRedRatio = 1.5,
            ClockSkewToleranceMinutes = -1.0
        };

        bool isValid = illegalOptions.Validate(out var error);
        Assert.False(isValid);
        Assert.NotNull(error);

        var (resolved, fallback, note) = InvariantOptions.Resolve(illegalOptions);
        Assert.True(fallback);
        Assert.NotNull(note);
        Assert.Contains("回退默认值", note);
        // 验证回退到了安全的默认值
        Assert.Equal(24.0, resolved.MaxDailyActiveHours);
        Assert.Equal(0.95, resolved.CoverageRedRatio);
        Assert.Equal(5.0, resolved.ClockSkewToleranceMinutes);
    }

    [Fact]
    public void ChangingThreshold_DynamicallyChangesEvaluationOutcome()
    {
        var baseTime = new DateTime(2026, 3, 10, 10, 0, 0, DateTimeKind.Utc);
        // 构造一个 40 分钟无输入的事件
        var events = new List<LongEventCandidate>
        {
            new()
            {
                DeviceId = "PC-1",
                EventType = "WindowActivity",
                StartTime = baseTime,
                EndTime = baseTime.AddMinutes(40),
                Keystrokes = 0,
                MouseClicks = 0
            }
        };

        // 默认阈值 30 分钟：40m > 30m，判定为超长未收尾 (FAIL)
        var defaultResult = DataReliabilityInvariants.CheckS2_OverlongEventEvidence(events);
        Assert.False(defaultResult.Pass);

        // 修改配置阈值为 60 分钟：40m <= 60m，不视为超长 (PASS)
        var relaxedOptions = new InvariantOptions { LongEventThresholdMinutes = 60.0 };
        var relaxedResult = DataReliabilityInvariants.CheckS2_OverlongEventEvidence(events, relaxedOptions);
        Assert.True(relaxedResult.Pass);
    }

    [Fact]
    public async Task Inspector_ConsumesProductionInvariants_ReturnsResult()
    {
        // 验证体检服务能正常注入并执行（引用 Pim.Core.Invariants）
        var optionsMock = Options.Create(new InvariantOptions());
        var inspector = new DataReliabilityQualityInspector(
            null!, // 纯内存/在线校验不依赖真实上下文
            optionsMock,
            NullLogger<DataReliabilityQualityInspector>.Instance);

        var report = await inspector.InspectAsync(DateTimeOffset.UtcNow);

        Assert.Equal("data_reliability", inspector.CheckName);
        Assert.True(report.IsHealthy);
        Assert.NotNull(report.Details);
        Assert.Contains("S12_INV-M22", report.Details.Keys);
        Assert.Contains("S11_INV-M21", report.Details.Keys);
    }
}
