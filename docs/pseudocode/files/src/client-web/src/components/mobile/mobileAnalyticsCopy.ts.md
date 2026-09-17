# src/client-web/src/components/mobile/mobileAnalyticsCopy.ts

## 元信息
- 语言：TypeScript
- 程序集或包：client-web
- 职责：手机分析中文文案常量：生活分类标签列表、中文文案守卫词列表。
- 主要依赖：无
- 被谁使用：`api/mobile.ts`（再导出 `MOBILE_LIFE_CATEGORIES`）；UI/测试可引用守卫词

## 函数级结构化伪代码

### MOBILE_LIFE_CATEGORY_LABELS
- 输入：无
- 输出：只读字符串元组（16 项：社交通讯…未分类）
- 副作用：无
- 步骤：`as const` 数组字面量
- 分支与异常：无
- 调用：无

### MOBILE_CHINESE_COPY_GUARDS
- 输入：无
- 输出：只读字符串元组（手机记录、使用热力图、历史位置、社交通讯）
- 副作用：无
- 步骤：`as const` 数组字面量，供文案一致性/测试守卫
- 分支与异常：无
- 调用：无

## 近逐行中文伪代码

1. 导出生活分类中文标签常量数组（含未分类）。
2. 导出中文文案守卫词常量数组。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-web/src/components/mobile/mobileAnalyticsCopy.ts",
      "label": "mobileAnalyticsCopy",
      "path": "src/client-web/src/components/mobile/mobileAnalyticsCopy.ts",
      "doc": "docs/pseudocode/files/src/client-web/src/components/mobile/mobileAnalyticsCopy.ts.md",
      "layer": "client-web",
      "kind": "other"
    }
  ],
  "edges": [
    { "from": "src/client-web/src/api/mobile.ts", "to": "src/client-web/src/components/mobile/mobileAnalyticsCopy.ts", "type": "depends_on" }
  ]
}
```
