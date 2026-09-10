# tests/client-web/pcClassificationApiPath.test.ts

## 元信息
- 语言：TypeScript
- 程序集或包：tests/client-web
- 职责：断言 pcClassificationApiPaths 规则/预览/应用/建议/重算/设置/标签路径。
- 主要依赖：src/client-web/src/api/pcTracker
- 被谁使用：Node 测试脚本

## 函数级结构化伪代码

### 模块顶层
- 步骤：组装 9 条路径 deepEqual 期望；每条 startsWith /pc/classification

## 近逐行中文伪代码

1. 导入 pcClassificationApiPaths
2. 构造 rules/preview/apply/suggestions/preview/apply/recompute/settings/recentProjectTags
3. deepEqual 与前缀断言

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/client-web/pcClassificationApiPath.test.ts",
      "label": "pcClassificationApiPath.test.ts",
      "path": "tests/client-web/pcClassificationApiPath.test.ts",
      "doc": "docs/pseudocode/files/tests/client-web/pcClassificationApiPath.test.ts.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": {"from":"tests/client-web/pcClassificationApiPath.test.ts","to":"src/client-web/src/api/pcTracker.ts","type":"tests"}
}
```