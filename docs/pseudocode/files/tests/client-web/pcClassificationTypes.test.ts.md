# tests/client-web/pcClassificationTypes.test.ts

## 元信息
- 语言：TypeScript
- 程序集或包：tests/client-web
- 职责：构造分类预览/设置/应用范围类型样例并断言。
- 主要依赖：src/client-web/src/types
- 被谁使用：Node 测试脚本

## 函数级结构化伪代码

### 模块顶层
- 步骤：ActivityClassificationPreview/Settings/ApplyRange 字面量；assert confirmation/duration/mode

## 近逐行中文伪代码

1. 导入类型
2. 构造 preview/settings/range
3. 字段断言

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/client-web/pcClassificationTypes.test.ts",
      "label": "pcClassificationTypes.test.ts",
      "path": "tests/client-web/pcClassificationTypes.test.ts",
      "doc": "docs/pseudocode/files/tests/client-web/pcClassificationTypes.test.ts.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [{"from":"tests/client-web/pcClassificationTypes.test.ts","to":"src/client-web/src/types/index.ts","type":"tests"}]
}
```