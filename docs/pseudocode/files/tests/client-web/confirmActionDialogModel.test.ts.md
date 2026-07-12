# tests/client-web/confirmActionDialogModel.test.ts

## 元信息
- 语言：TypeScript (Node test assert)
- 程序集或包：tests/client-web
- 职责：验证 buildDeleteConfirmationCopy 各 targetType 文案；静态扫描 ConfirmActionDialog 具备焦点陷阱/Escape/Tab 相关实现。
- 主要依赖：confirmActionDialogModel、ConfirmActionDialog.tsx、node:assert/strict、fs
- 被谁使用：client-web 测试脚本

## 函数级结构化伪代码

### 顶层脚本（无 export 类）
#### 单日程删除文案
- targetType event、affectedCount 1 → 标题「删除日程」、描述含回收站、确认「移动到回收站」、samples 回传

#### 日历本级联
- calendar / calendar-book：标题「删除日历本」、描述「N 个关联项目」、确认「确认移动 N 项」

#### 任务本
- task-book：标题「删除任务本」、确认「确认移动 2 项」

#### 对话框源码契约
- 读 ConfirmActionDialog.tsx，匹配 useEffect/useRef/previouslyFocusedRef/tabIndex=-1/onKeyDown Escape/Tab/querySelectorAll

## 近逐行中文伪代码

1. 构造 eventSamples 样例。
2. buildDeleteConfirmationCopy 断言 event 单条文案。
3. calendar 与 calendar-book 级联文案。
4. task-book 文案。
5. readFileSync 对话框组件，正则断言焦点与键盘处理。

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/client-web/confirmActionDialogModel.test.ts",
      "label": "confirmActionDialogModel.test",
      "path": "tests/client-web/confirmActionDialogModel.test.ts",
      "doc": "docs/pseudocode/files/tests/client-web/confirmActionDialogModel.test.ts.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "tests/client-web/confirmActionDialogModel.test.ts", "to": "src/client-web/src/ui/confirmActionDialogModel.ts", "type": "tests" },
    { "from": "tests/client-web/confirmActionDialogModel.test.ts", "to": "src/client-web/src/ui/ConfirmActionDialog.tsx", "type": "tests" }
  ]
}
```
