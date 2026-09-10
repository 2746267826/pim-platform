# tests/client-web/mobileFormatting.test.ts

## 元信息
- 语言：TypeScript
- 程序集或包：tests/client-web
- 职责：验证手机分析日期范围、上海时区 UTC 边界与时长/百分比格式化。
- 主要依赖：`mobileFormatting`
- 被谁使用：Node 测试脚本

## 函数级结构化伪代码

- buildMobileAnalyticsDateRange('7d') → 本地 7 日
- toMobileAnalyticsUtcRange → 前一日 16:00Z 至末日 16:00Z、Asia/Shanghai
- formatDuration 分钟/小时分钟/0秒；formatPercent 百分与 NaN/Inf→0%

## 近逐行中文伪代码

1. [L1-L28] 导入与全部 assert

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/client-web/mobileFormatting.test.ts",
      "label": "mobileFormatting.test",
      "path": "tests/client-web/mobileFormatting.test.ts",
      "doc": "docs/pseudocode/files/tests/client-web/mobileFormatting.test.ts.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "tests/client-web/mobileFormatting.test.ts", "to": "src/client-web/src/components/mobile/mobileFormatting.ts", "type": "tests" }
  ]
}
```
