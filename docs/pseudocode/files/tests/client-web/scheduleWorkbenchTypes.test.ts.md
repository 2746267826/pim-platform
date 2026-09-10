# tests/client-web/scheduleWorkbenchTypes.test.ts

## 元信息
- 语言：TypeScript
- 程序集或包：tests/client-web
- 职责：编译期/运行期锁定工作台类型字面量与样例对象形状。
- 主要依赖：`src/client-web/src/types` 中日历/确认/Outlook 类型
- 被谁使用：Node 测试脚本

## 函数级结构化伪代码

### 顶层
- 赋值 riskLevel/layerId/densityMode 等字面量
- 构造 taskSegment、calendarLayers、dataCenterQuery、outlookSettings、deviceCode、syncBatch、confirmation
- assert 关键字面量值；void 其余对象防未使用

## 近逐行中文伪代码

1. [L1-L13] 类型导入
2. [L15-L22] 枚举字面量
3. [L24-L137] 样例响应对象
4. [L139-L152] 断言与 void

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/client-web/scheduleWorkbenchTypes.test.ts",
      "label": "scheduleWorkbenchTypes.test",
      "path": "tests/client-web/scheduleWorkbenchTypes.test.ts",
      "doc": "docs/pseudocode/files/tests/client-web/scheduleWorkbenchTypes.test.ts.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "tests/client-web/scheduleWorkbenchTypes.test.ts", "to": "src/client-web/src/types/index.ts", "type": "tests" }
  ]
}
```
