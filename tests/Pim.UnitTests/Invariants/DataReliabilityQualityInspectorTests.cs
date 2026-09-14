using System;
using System.Collections.Generic;
using System.Data;
using System.Data.Common;
using System.Threading;
using System.Threading.Tasks;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Logging.Abstractions;
using Microsoft.Extensions.Options;
using Pim.Core.Invariants;
using Pim.Infrastructure.Data;
using Pim.Infrastructure.Operations;
using Xunit;

namespace Pim.UnitTests.Invariants;

public class DataReliabilityQualityInspectorTests
{
    [Fact]
    public void Invariants_WhenCollectionsEmpty_ReturnUnknownStatus()
    {
        // 验证空集合不得视为通过（绝不亮假绿灯），全部判为 ⚪ UNKNOWN
        var r1 = DataReliabilityInvariants.CheckS1_NoOverlap(new List<EventTimeSpan>());
        Assert.Equal(InvariantStatus.Unknown, r1.Status);
        Assert.False(r1.Pass);
        Assert.StartsWith("INV-P16 UNKNOWN", r1.Detail);

        var r2 = DataReliabilityInvariants.CheckS2_OverlongEventEvidence(new List<LongEventCandidate>());
        Assert.Equal(InvariantStatus.Unknown, r2.Status);
        Assert.False(r2.Pass);

        var r3 = DataReliabilityInvariants.CheckS3_DailyDurationBounded(new List<DailyActiveDuration>());
        Assert.Equal(InvariantStatus.Unknown, r3.Status);
        Assert.False(r3.Pass);

        var r4 = DataReliabilityInvariants.CheckS4_BusinessKeyUnique(new List<BusinessRecordKey>());
        Assert.Equal(InvariantStatus.Unknown, r4.Status);
        Assert.False(r4.Pass);

        var r5 = DataReliabilityInvariants.CheckS5_ClockTrustworthy(new List<ClockEventItem>());
        Assert.Equal(InvariantStatus.Unknown, r5.Status);
        Assert.False(r5.Pass);

        var r6 = DataReliabilityInvariants.CheckS6_OfflineDeclared(new DeviceActivityTrace());
        Assert.Equal(InvariantStatus.Unknown, r6.Status);
        Assert.False(r6.Pass);

        var r7 = DataReliabilityInvariants.CheckS7_TimelineGapMarked(new List<TimelineInterval>());
        Assert.Equal(InvariantStatus.Unknown, r7.Status);
        Assert.False(r7.Pass);

        var r8 = DataReliabilityInvariants.CheckS8_DayBoundaryConsistent(new List<DayBoundarySample>());
        Assert.Equal(InvariantStatus.Unknown, r8.Status);
        Assert.False(r8.Pass);

        var r9 = DataReliabilityInvariants.CheckS9_GapHasSignal(new CoverageSignalReport { OnlineDurationSeconds = 0 });
        Assert.Equal(InvariantStatus.Unknown, r9.Status);
        Assert.False(r9.Pass);

        var r10 = DataReliabilityInvariants.CheckS10_TaskHasOutput(new List<BackgroundTaskRun>());
        Assert.Equal(InvariantStatus.Unknown, r10.Status);
        Assert.False(r10.Pass);

        var r11 = DataReliabilityInvariants.CheckS11_StatusSemantics(new List<BatchSyncStatusRecord>());
        Assert.Equal(InvariantStatus.Unknown, r11.Status);
        Assert.False(r11.Pass);

        var r12 = DataReliabilityInvariants.CheckS12_DerivedTableActive(new List<DerivedTableStatus>());
        Assert.Equal(InvariantStatus.Unknown, r12.Status);
        Assert.False(r12.Pass);

        var r13 = DataReliabilityInvariants.CheckS13_SingleInstance(new List<CollectionHeartbeat>());
        Assert.Equal(InvariantStatus.Unknown, r13.Status);
        Assert.False(r13.Pass);
    }

    [Fact]
    public async Task Inspector_WhenDbNull_ReturnsUnhealthyAndThirteenUnknowns()
    {
        var options = Options.Create(new InvariantOptions());
        var inspector = new DataReliabilityQualityInspector(null, options, NullLogger<DataReliabilityQualityInspector>.Instance);

        var result = await inspector.InspectAsync(DateTimeOffset.UtcNow);

        Assert.False(result.IsHealthy);
        Assert.Equal(13, result.IssueCount);
        Assert.Contains("S1_INV-P16", result.Details.Keys);
        Assert.Contains("S13_INV-P22", result.Details.Keys);
        foreach (var kvp in result.Details)
        {
            Assert.StartsWith("⚪ UNKNOWN", kvp.Value);
        }
    }

    [Fact]
    public void InvariantResult_FourStatesProperties_AreMutuallyConsistent()
    {
        var pass = InvariantResult.Success("OK");
        Assert.True(pass.IsPass);
        Assert.False(pass.IsWarning);
        Assert.False(pass.IsFail);
        Assert.False(pass.IsUnknown);

        var warn = InvariantResult.Warning("Warn");
        Assert.False(warn.IsPass);
        Assert.True(warn.IsWarning);
        Assert.False(warn.IsFail);
        Assert.False(warn.IsUnknown);

        var fail = InvariantResult.Failure("Fail");
        Assert.False(fail.IsPass);
        Assert.False(fail.IsWarning);
        Assert.True(fail.IsFail);
        Assert.False(fail.IsUnknown);

        var unknown = InvariantResult.Unknown("Unknown");
        Assert.False(unknown.IsPass);
        Assert.False(unknown.IsWarning);
        Assert.False(unknown.IsFail);
        Assert.True(unknown.IsUnknown);
    }
}
