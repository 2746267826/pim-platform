# src/modules/Pim.Module.PcTracker/Services/AppNameNormalizer.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.PcTracker
- 职责：将进程/应用名规范化为小写且去掉 `.exe` 后缀，空白回退 `"unknown"`。
- 主要依赖：无
- 被谁使用：`PcTrackerService`（写入/查询时填充 `AppNameNormalized`）

## 函数级结构化伪代码

### AppNameNormalizer
#### Normalize(string? appName)
- 输入：原始应用名
- 输出：规范化字符串
- 副作用：无
- 步骤：
  1. null/空白 → `"unknown"`。
  2. Trim + ToLowerInvariant。
  3. 若以 `.exe` 结尾则去掉四字符后缀，否则原样返回。
- 分支与异常：无
- 调用：无

## 近逐行中文伪代码

1. 静态类 `AppNameNormalizer`。
2. 空白返回 unknown。
3. 小写 trim；去掉末尾 `.exe`。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.PcTracker/Services/AppNameNormalizer.cs",
      "label": "AppNameNormalizer",
      "path": "src/modules/Pim.Module.PcTracker/Services/AppNameNormalizer.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.PcTracker/Services/AppNameNormalizer.cs.md",
      "layer": "module.pctracker",
      "kind": "service"
    }
  ],
  "edges": [
    { "from": "src/modules/Pim.Module.PcTracker/Services/PcTrackerService.cs", "to": "src/modules/Pim.Module.PcTracker/Services/AppNameNormalizer.cs", "type": "calls" }
  ]
}
```
