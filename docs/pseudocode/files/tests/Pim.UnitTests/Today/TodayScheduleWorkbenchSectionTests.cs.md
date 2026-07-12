# tests/Pim.UnitTests/Today/TodayScheduleWorkbenchSectionTests.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.UnitTests
- 职责：源码契约：Today 注册表包含排程工作台相关 section kind，且 Program 注册对应 Provider。
- 主要依赖：`TodaySectionProviders.cs`、`Program.cs` 源文件
- 被谁使用：xUnit

## 函数级结构化伪代码

### TodayScheduleWorkbenchSectionTests
#### TodayRegistryIncludesScheduleWorkbenchSections()
- 输入：无
- 输出：无
- 副作用：读仓库源文件
- 步骤：
  1. RepoPath 向上查找 `TodaySectionProviders.cs` 与 `Program.cs`
  2. 断言 providers 源含 calendar.* / operations / sync / reminders / reports / endpoints / pc.* 等 kind 字符串
  3. 断言 Program 含 `ITodaySectionProvider, {ProviderName}` 注册列表
- 分支与异常：找不到文件 → FileNotFoundException
- 调用：`File.ReadAllText`、`RepoPath`

#### RepoPath(params string[] parts)
- 输入：相对路径段
- 输出：绝对路径
- 副作用：无
- 步骤：从 BaseDirectory 向上找存在的候选路径
- 分支与异常：未找到抛 FileNotFoundException
- 调用：`Directory.GetParent`

## 近逐行中文伪代码

1. 读取 TodaySectionProviders 与 Program 源码
2. 校验 13 个 section kind 字面量存在
3. 校验 10 个 Provider 类型 DI 注册字符串
4. RepoPath 自底向上定位仓库文件

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/Pim.UnitTests/Today/TodayScheduleWorkbenchSectionTests.cs",
      "label": "TodayScheduleWorkbenchSectionTests",
      "path": "tests/Pim.UnitTests/Today/TodayScheduleWorkbenchSectionTests.cs",
      "doc": "docs/pseudocode/files/tests/Pim.UnitTests/Today/TodayScheduleWorkbenchSectionTests.cs.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "tests/Pim.UnitTests/Today/TodayScheduleWorkbenchSectionTests.cs", "to": "src/Pim.Api/Today/TodaySectionProviders.cs", "type": "tests" },
    { "from": "tests/Pim.UnitTests/Today/TodayScheduleWorkbenchSectionTests.cs", "to": "src/Pim.Api/Program.cs", "type": "tests" }
  ]
}
```
