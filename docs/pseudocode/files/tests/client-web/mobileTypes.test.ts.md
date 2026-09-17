# tests/client-web/mobileTypes.test.ts

## 元信息
- 语言：TypeScript
- 程序集或包：tests/client-web
- 职责：编译期/运行期校验 mobile API 返回类型与样例 DTO 字段契约。
- 主要依赖：`src/client-web/src/api/mobile` 类型与函数
- 被谁使用：Node 测试脚本

## 函数级结构化伪代码

### accepts*Return 辅助
- 步骤：用函数签名约束 ReturnType → Promise\<具体类型\>；void 引用防 tree-shake

### 样例字面量
- 步骤：构造 MobileAnalyticsQuery/Overview/Heatmap/Chart/TimelineBlock/Override/Rule/Goal/Location* 完整样例

### 运行断言
- 步骤：默认时区 Asia/Shanghai；生命分类含生活服务；样例字段 equal

## 近逐行中文伪代码

1. [L1-47] 导入 API 与类型
2. [L49-114] 类型接受器与 void 引用
3. [L116-306] 构造 analytics/location 样例
4. [L308-323] assert 关键字段

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/client-web/mobileTypes.test.ts",
      "label": "mobileTypes.test",
      "path": "tests/client-web/mobileTypes.test.ts",
      "doc": "docs/pseudocode/files/tests/client-web/mobileTypes.test.ts.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "tests/client-web/mobileTypes.test.ts", "to": "src/client-web/src/api/mobile.ts", "type": "tests" }
  ]
}
```
