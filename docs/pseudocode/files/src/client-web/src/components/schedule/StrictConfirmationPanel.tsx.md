# src/client-web/src/components/schedule/StrictConfirmationPanel.tsx

## 元信息
- 语言：TypeScript/TSX
- 程序集或包：client-web
- 职责：UI 组件 `StrictConfirmationPanel`：交互面板/控件，展示数据并回传用户操作。
- 主要依赖：`src/client-web/src/types`
- 被谁使用：阅读时由总控/关系图汇总；本文件边中列出 depends_on

## 函数级结构化伪代码

### (file)
#### 模块顶层
- 输入：见导入与导出
- 输出：导出符号
- 副作用：见近逐行
- 步骤：
  1. 定义类型 `StrictConfirmationPanelProps`
  2. 执行：confirmation: OperationConfirmation;
  3. 执行：armed: boolean;
  4. 执行：onArm: () => void;
  5. 默认导出函数 `StrictConfirmationPanel`
  6. 执行：confirmation,
  7. 赋值 `strict` = confirmation.requiresStrictConfirmation || confirmation.riskLevel === 'L4BatchOrDestructiveGovernanc
  8. 返回 JSX/结构
  9. 执行：<section className={`rounded-lg border p-3 text-sm ${
  10. 执行：strict ? 'border-red-200 bg-red-50 text-red-800' : 'border-amber-200 bg-amber-50 text-amber-800'
  11. 执行：<h3 className="font-semibold">{strict ? '严格确认' : '二级确认'}</h3>
  12. 执行：<p className="mt-1 text-xs leading-5">
  13. 执行：? 'L4 或破坏性治理操作需要严格确认，并保留恢复路径。'
  14. 执行：: '此操作需要二级确认，先复核影响对象、来源和回写影响。'}
  15. 执行：type="button"
  16. 执行：onClick={onArm}
  17. 执行：disabled={armed}
  18. 执行：className="mt-3 rounded-lg border border-current px-3 py-1.5 text-xs font-semibold disabled:opacity-60"
  19. 执行：{armed ? '已就绪' : '我已复核'}
  20. 执行：</button>
  21. 执行：</section>
- 分支与异常：无显著分支
- 调用：StrictConfirmationPanel

## 近逐行中文伪代码

1. [L3] 定义类型 `StrictConfirmationPanelProps`
2. [L4] 执行：confirmation: OperationConfirmation;
3. [L5] 执行：armed: boolean;
4. [L6] 执行：onArm: () => void;
5. [L9] 默认导出函数 `StrictConfirmationPanel`
6. [L10] 执行：confirmation,
7. [L14] 赋值 `strict` = confirmation.requiresStrictConfirmation || confirmation.riskLevel === 'L4BatchOrDestructiveGovernanc
8. [L16] 返回 JSX/结构
9. [L17] 执行：<section className={`rounded-lg border p-3 text-sm ${
10. [L18] 执行：strict ? 'border-red-200 bg-red-50 text-red-800' : 'border-amber-200 bg-amber-50 text-amber-800'
11. [L20] 执行：<h3 className="font-semibold">{strict ? '严格确认' : '二级确认'}</h3>
12. [L21] 执行：<p className="mt-1 text-xs leading-5">
13. [L23] 执行：? 'L4 或破坏性治理操作需要严格确认，并保留恢复路径。'
14. [L24] 执行：: '此操作需要二级确认，先复核影响对象、来源和回写影响。'}
15. [L27] 执行：type="button"
16. [L28] 执行：onClick={onArm}
17. [L29] 执行：disabled={armed}
18. [L30] 执行：className="mt-3 rounded-lg border border-current px-3 py-1.5 text-xs font-semibold disabled:opacity-60"
19. [L32] 执行：{armed ? '已就绪' : '我已复核'}
20. [L33] 执行：</button>
21. [L34] 执行：</section>

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-web/src/components/schedule/StrictConfirmationPanel.tsx",
      "label": "StrictConfirmationPanel",
      "path": "src/client-web/src/components/schedule/StrictConfirmationPanel.tsx",
      "doc": "docs/pseudocode/files/src/client-web/src/components/schedule/StrictConfirmationPanel.tsx.md",
      "layer": "client-web",
      "kind": "ui"
    }
  ],
  "edges": [
    {
      "from": "src/client-web/src/components/schedule/StrictConfirmationPanel.tsx",
      "to": "src/client-web/src/types",
      "type": "depends_on"
    }
  ]
}
```
