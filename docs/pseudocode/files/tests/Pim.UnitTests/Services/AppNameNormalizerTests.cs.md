# tests/Pim.UnitTests/Services/AppNameNormalizerTests.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.UnitTests
- 职责：验证 `AppNameNormalizer.Normalize` 产出稳定小写应用键，空白回落 `unknown`。
- 主要依赖：`AppNameNormalizer`
- 被谁使用：xUnit

## 函数级结构化伪代码

### AppNameNormalizerTests
#### Normalize_ReturnsStableLowercaseAppKey(string input, string expected)
- 输入：Theory 用例（含 `.exe`、大小写、空格、多段名）
- 输出：无
- 副作用：无
- 步骤：
  1. 调用 `AppNameNormalizer.Normalize(input)`
  2. 断言等于 expected（去扩展名、Trim、小写）
- 分支与异常：无
- 调用：`AppNameNormalizer.Normalize`

#### Normalize_ReturnsUnknownForBlankInput()
- 输入：无
- 输出：无
- 副作用：无
- 步骤：
  1. 空串与 null 均断言为 `"unknown"`
- 分支与异常：无
- 调用：`AppNameNormalizer.Normalize`

## 近逐行中文伪代码

1. 引入 `Pim.Module.PcTracker.Services` 与 Xunit
2. Theory：`msedge.exe`→`msedge`；已无扩展名保持；`Codex.exe`→`codex`；`Google Chrome`→`google chrome`；前后空白与多段 `.exe` 归一
3. Fact：`""` 与 `null` → `"unknown"`

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/Pim.UnitTests/Services/AppNameNormalizerTests.cs",
      "label": "AppNameNormalizerTests",
      "path": "tests/Pim.UnitTests/Services/AppNameNormalizerTests.cs",
      "doc": "docs/pseudocode/files/tests/Pim.UnitTests/Services/AppNameNormalizerTests.cs.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "tests/Pim.UnitTests/Services/AppNameNormalizerTests.cs", "to": "src/modules/Pim.Module.PcTracker/Services/AppNameNormalizer.cs", "type": "tests" }
  ]
}
```
