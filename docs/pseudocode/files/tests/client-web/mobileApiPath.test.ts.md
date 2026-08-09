# tests/client-web/mobileApiPath.test.ts

## 元信息
- 语言：TypeScript
- 程序集或包：tests/client-web
- 职责：断言 mobile API 默认时区、生活分类常量与各路径 query/编码。
- 主要依赖：`mobileApiPaths`、`MOBILE_DEFAULT_TIMEZONE`、`MOBILE_LIFE_CATEGORIES`
- 被谁使用：Node 测试脚本

## 函数级结构化伪代码

### 顶层断言
- 时区 Asia/Shanghai；16 类生活分类中文列表
- devices/summary/timeline/location*/quality/analytics*/catalog/goals 路径与 encodeURI 行为（含 `/` 与中文）

## 近逐行中文伪代码

1. [L1-L33] 导入与 expectedLifeCategories
2. [L34-L129] 逐路径 assert.equal

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/client-web/mobileApiPath.test.ts",
      "label": "mobileApiPath.test",
      "path": "tests/client-web/mobileApiPath.test.ts",
      "doc": "docs/pseudocode/files/tests/client-web/mobileApiPath.test.ts.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "tests/client-web/mobileApiPath.test.ts", "to": "src/client-web/src/api/mobile.ts", "type": "tests" }
  ]
}
```
