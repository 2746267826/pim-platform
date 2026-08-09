# tests/Pim.UnitTests/Services/ActivityTimelineSmoothingServiceTests.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.UnitTests
- 职责：时间线条平滑：合并低置信短 fallback；保留强沟通短块；跨 gap 不合并；非 fallback 短块保留。
- 主要依赖：`ActivityTimelineSmoothingService`、`TimelineItem`
- 被谁使用：dotnet test

## 函数级结构化伪代码

### Smooth_MergesLowConfidenceShortBlockBetweenSameProjectBlocks
- 步骤：两侧编程/PIM 夹 fallback 其他 → 合并 30 分钟并写 explanation

### Smooth_KeepsStrongShortCommunicationBlock
- 步骤：1 分钟高置信沟通保留

### Smooth_DoesNotMergeAcrossGaps
- 步骤：时间不连续保留 3 段

### Smooth_KeepsLowConfidenceShortNonFallbackBlocks
- 步骤：manual/heuristic 源不平滑掉

### Item 工厂
- 步骤：由 start+minutes 构造 TimelineItem

## 近逐行中文伪代码

1. [L9-29] 合并 fallback
2. [L31-48] 保留沟通
3. [L50-66] gap
4. [L68-86] 非 fallback
5. [L88-111] Item 辅助

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/Pim.UnitTests/Services/ActivityTimelineSmoothingServiceTests.cs",
      "label": "ActivityTimelineSmoothingServiceTests",
      "path": "tests/Pim.UnitTests/Services/ActivityTimelineSmoothingServiceTests.cs",
      "doc": "docs/pseudocode/files/tests/Pim.UnitTests/Services/ActivityTimelineSmoothingServiceTests.cs.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "tests/Pim.UnitTests/Services/ActivityTimelineSmoothingServiceTests.cs", "to": "src/Pim.Module.PcTracker/Services/ActivityTimelineSmoothingService.cs", "type": "tests" }
  ]
}
```
