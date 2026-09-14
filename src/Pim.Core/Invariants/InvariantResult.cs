using System;
using System.Collections.Generic;

namespace Pim.Core.Invariants;

/// <summary>
/// 不变量判定统一返回结果结构。
/// 同时支持单条判定（(pass, detail) 解构与隐式转换）与设置页/CI体检场景（统计量、样例、时间范围、回退标注）。
/// </summary>
public sealed class InvariantResult
{
    public bool Pass { get; init; } = true;
    public string Detail { get; init; } = string.Empty;
    public int TotalViolations { get; init; } = 0;
    public int NewViolations { get; init; } = 0;
    public int HistoricalViolations { get; init; } = 0;
    public IReadOnlyList<string> Samples { get; init; } = Array.Empty<string>();
    public DateTime? EarliestOccurrence { get; init; }
    public DateTime? LatestOccurrence { get; init; }
    public bool ThresholdFallback { get; init; } = false;
    public string? ThresholdNote { get; init; }

    public void Deconstruct(out bool pass, out string detail)
    {
        pass = Pass;
        detail = Detail;
    }

    public static implicit operator (bool pass, string detail)(InvariantResult r) => (r.Pass, r.Detail);

    public static implicit operator InvariantResult((bool pass, string detail) tuple) => new()
    {
        Pass = tuple.pass,
        Detail = tuple.detail,
        TotalViolations = tuple.pass ? 0 : 1
    };

    public static InvariantResult Success(string detail, string? thresholdNote = null, bool thresholdFallback = false) => new()
    {
        Pass = true,
        Detail = detail,
        ThresholdNote = thresholdNote,
        ThresholdFallback = thresholdFallback
    };

    public static InvariantResult Failure(
        string detail,
        int totalViolations = 1,
        int newViolations = 0,
        int historicalViolations = 0,
        IReadOnlyList<string>? samples = null,
        DateTime? earliestOccurrence = null,
        DateTime? latestOccurrence = null,
        string? thresholdNote = null,
        bool thresholdFallback = false)
    {
        string fullDetail = detail;
        if (samples != null && samples.Count > 0)
        {
            fullDetail = $"{detail}. 样本: [{string.Join("; ", samples)}]";
        }

        return new()
        {
            Pass = false,
            Detail = fullDetail,
            TotalViolations = totalViolations > 0 ? totalViolations : (newViolations + historicalViolations),
            NewViolations = newViolations,
            HistoricalViolations = historicalViolations,
            Samples = samples ?? Array.Empty<string>(),
            EarliestOccurrence = earliestOccurrence,
            LatestOccurrence = latestOccurrence,
            ThresholdNote = thresholdNote,
            ThresholdFallback = thresholdFallback
        };
    }
}
