# tests/client-web/confirmationSecondLevel.test.ts

## 元信息
- 语言：TypeScript (node:assert)
- 程序集或包：tests/client-web
- 职责：验证 ConfirmationsPage.getConfirmActionState 二级确认（arm）状态机文案与 requiresArm。
- 主要依赖：getConfirmActionState from ConfirmationsPage
- 被谁使用：node 测试脚本

## 函数级结构化伪代码

### 顶层断言脚本
#### case requiresSecondLevel=false, armed=false
- 期望 { label: 'Confirm', requiresArm: false }

#### case requiresSecondLevel=true, armed=false
- 期望 { label: 'Confirm', requiresArm: true }

#### case requiresSecondLevel=true, armed=true
- 期望 { label: 'Confirm final', requiresArm: false }

## 近逐行中文伪代码

1. 导入 assert 与 getConfirmActionState。
2. 三组 deepEqual 覆盖未二级/需 arm/已 arm 最终确认。

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/client-web/confirmationSecondLevel.test.ts",
      "label": "confirmationSecondLevel.test",
      "path": "tests/client-web/confirmationSecondLevel.test.ts",
      "doc": "docs/pseudocode/files/tests/client-web/confirmationSecondLevel.test.ts.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "tests/client-web/confirmationSecondLevel.test.ts", "to": "src/client-web/src/pages/ConfirmationsPage.tsx", "type": "tests" }
  ]
}
```
