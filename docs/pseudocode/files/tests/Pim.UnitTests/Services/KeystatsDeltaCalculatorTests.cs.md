# tests/Pim.UnitTests/Services/KeystatsDeltaCalculatorTests.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.UnitTests
- 职责：键鼠采样差分：正常增量、计数回落 reset、采样间隔 gap、无 previous 的 gap 基线。
- 主要依赖：`KeystatsDeltaCalculator`、`KeystatsSampleEntity`
- 被谁使用：xUnit

## 函数级结构化伪代码

### KeystatsDeltaCalculatorTests
#### Calculate_ReturnsDifferenceBetweenConsecutiveSamples()
- 输入：前后采样 keys/clicks 递增
- 输出：无
- 副作用：无
- 步骤：差分 7/3；IsGap/IsReset 假；DeviceId 与 MinuteStartUtc 来自 current
- 分支与异常：无
- 调用：`KeystatsDeltaCalculator.Calculate`

#### Calculate_MarksResetWhenCountersDecrease()
- 输入：总计数下降
- 输出：无
- 副作用：无
- 步骤：各差分为 0；IsReset 真
- 分支与异常：无
- 调用：同上

#### Calculate_MarksResetWhenIndividualClickCounterDecreases()
- 输入：LeftClicks 下降但 total 上升
- 输出：无
- 副作用：无
- 步骤：仍视为 reset，差分清零
- 分支与异常：无
- 调用：同上

#### Calculate_MarksGapWhenSamplesMoreThanTwoMinutesApart()
- 输入：间隔 ≥3 分钟
- 输出：无
- 副作用：无
- 步骤：仍算差分但 IsGap 真
- 分支与异常：无
- 调用：同上

#### Calculate_MarksGapAndUsesCurrentCountersWhenPreviousIsMissing()
- 输入：previous=null
- 输出：无
- 副作用：无
- 步骤：使用 current 累计值；IsGap 真
- 分支与异常：无
- 调用：同上

#### Sample(...)
- 输入：时间串、keys、clicks
- 输出：KeystatsSampleEntity
- 副作用：无
- 步骤：填充 Device/距离派生字段
- 分支与异常：无
- 调用：无

## 近逐行中文伪代码

1. 连续采样正常差分
2. 总计数回落 → reset
3. 分项点击回落 → reset
4. 超 2 分钟 → gap 仍差分
5. 无 previous → gap + 用当前累计
6. Sample 工厂

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/Pim.UnitTests/Services/KeystatsDeltaCalculatorTests.cs",
      "label": "KeystatsDeltaCalculatorTests",
      "path": "tests/Pim.UnitTests/Services/KeystatsDeltaCalculatorTests.cs",
      "doc": "docs/pseudocode/files/tests/Pim.UnitTests/Services/KeystatsDeltaCalculatorTests.cs.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "tests/Pim.UnitTests/Services/KeystatsDeltaCalculatorTests.cs", "to": "src/modules/Pim.Module.PcTracker/Services/KeystatsDeltaCalculator.cs", "type": "tests" }
  ]
}
```
