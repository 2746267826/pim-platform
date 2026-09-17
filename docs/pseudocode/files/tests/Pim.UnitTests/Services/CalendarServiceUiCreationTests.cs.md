# tests/Pim.UnitTests/Services/CalendarServiceUiCreationTests.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.UnitTests
- 职责：UI 创建事件时 `CalendarId` 为空 Guid 则自动使用/创建默认日历。
- 主要依赖：`CalendarService`、`PimDbContext`、`RecurrenceService`、`ICurrentUserService`
- 被谁使用：xUnit

## 函数级结构化伪代码

### CalendarServiceUiCreationTests
#### CreateEventAsync_WithEmptyCalendarId_UsesDefaultCalendar()
- 输入：无
- 输出：无
- 副作用：InMemory DB 写入日历与事件
- 步骤：
  1. `CreateService` 注册 Calendar 模块、固定用户、InMemory DB、`CalendarService`
  2. `CreateEventAsync` 传入 `Guid.Empty` 作为 CalendarId
  3. 断言唯一日历 Id 等于事件 CalendarId、UserId 匹配、`IsDefault` 为 true
- 分支与异常：无
- 调用：`CalendarService.CreateEventAsync`

#### CreateService() / FixedCurrentUserService
- 输入：无 / 固定 userId
- 输出：服务三元组 / 当前用户
- 副作用：注册程序集、建库
- 步骤：
  1. RegisterModuleAssembly(CalendarEntity)
  2. 构造 FixedCurrentUserService 与 RecurrenceService
- 分支与异常：无
- 调用：`PimDbContext`、`CalendarService`

## 近逐行中文伪代码

1. 空 CalendarId 创建事件
2. DB 仅一条默认日历，绑定当前用户
3. 事件 CalendarId 回填默认日历 Id
4. 测试替身：FixedCurrentUserService 提供 UserId 与 Role

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/Pim.UnitTests/Services/CalendarServiceUiCreationTests.cs",
      "label": "CalendarServiceUiCreationTests",
      "path": "tests/Pim.UnitTests/Services/CalendarServiceUiCreationTests.cs",
      "doc": "docs/pseudocode/files/tests/Pim.UnitTests/Services/CalendarServiceUiCreationTests.cs.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "tests/Pim.UnitTests/Services/CalendarServiceUiCreationTests.cs", "to": "src/modules/Pim.Module.Calendar/Services/CalendarService.cs", "type": "tests" }
  ]
}
```
