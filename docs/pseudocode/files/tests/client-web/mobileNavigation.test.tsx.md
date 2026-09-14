# tests/client-web/mobileNavigation.test.tsx

## 元信息
- 语言：TypeScript
- 程序集或包：tests/client-web
- 职责：断言侧栏主导航中「手机记录」「历史位置」路径稳定。
- 主要依赖：`src/client-web/src/layout/Sidebar` 的 `primaryNavItems`
- 被谁使用：Node 测试脚本

## 函数级结构化伪代码

### 模块顶层
- 步骤：
  1. 在 `primaryNavItems` 中按 label 查找两项
  2. 断言 path 分别为 `/mobile-records` 与 `/location-history`

## 近逐行中文伪代码

1. [L1-2] 导入 assert 与 primaryNavItems
2. [L4-5] find 手机记录 / 历史位置
3. [L7-8] equal 路径

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/client-web/mobileNavigation.test.tsx",
      "label": "mobileNavigation.test",
      "path": "tests/client-web/mobileNavigation.test.tsx",
      "doc": "docs/pseudocode/files/tests/client-web/mobileNavigation.test.tsx.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    {
      "from": "tests/client-web/mobileNavigation.test.tsx",
      "to": "src/client-web/src/layout/Sidebar.tsx",
      "type": "tests"
    }
  ]
}
```
