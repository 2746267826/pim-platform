# tests/client-web/scheduleWorkbenchLocalization.test.ts

## 元信息
- 语言：TypeScript
- 程序集或包：tests/client-web
- 职责：工作台中文 i18n 键存在且无长英文单词。
- 主要依赖：scheduleWorkbench.zh-CN
- 被谁使用：Node 测试脚本

## 函数级结构化伪代码

### 模块顶层
- 步骤：关键键 typeof string；值不匹配 [A-Za-z]{4,}

## 近逐行中文伪代码

1. 导入 scheduleWorkbenchZhCN
2. 循环 11 个 key 断言

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/client-web/scheduleWorkbenchLocalization.test.ts",
      "label": "scheduleWorkbenchLocalization.test.ts",
      "path": "tests/client-web/scheduleWorkbenchLocalization.test.ts",
      "doc": "docs/pseudocode/files/tests/client-web/scheduleWorkbenchLocalization.test.ts.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [{"from":"tests/client-web/scheduleWorkbenchLocalization.test.ts","to":"src/client-web/src/i18n/scheduleWorkbench.zh-CN.ts","type":"tests"}]
}
```