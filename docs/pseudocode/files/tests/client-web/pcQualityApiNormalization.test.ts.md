# tests/client-web/pcQualityApiNormalization.test.ts

## 元信息
- 语言：TypeScript
- 程序集或包：tests/client-web
- 职责：验证 `normalizePcQuality` 将数值状态/空字段规范为枚举字符串与中文标签。
- 主要依赖：`src/client-web/src/api/pcTracker` 的 `normalizePcQuality`
- 被谁使用：Node 测试脚本

## 函数级结构化伪代码

### 顶层脚本
#### normalizePcQuality 契约
- 输入：含 overallStatus=2、components status 3/99、issues severity=3、nextSteps 混类型
- 输出：规范化 quality 对象
- 副作用：无
- 步骤：
  1. 调用 `normalizePcQuality` 传入原始 payload
  2. 断言 overallStatus→`Warning`，label→`有警告`
  3. 组件 status：3→Critical，99→Unknown；details 数字转字符串，null→{}
  4. issue severity→Critical；nextSteps 全转字符串
- 分支与异常：assert 失败抛错
- 调用：`normalizePcQuality`

## 近逐行中文伪代码

1. [L1-L2] 导入 assert 与 normalizePcQuality
2. [L4-L35] 构造含两组件、一 issue、混类型 nextSteps 的输入并规范化
3. [L37-L45] 断言状态映射、中文 label、details 与 nextSteps 规范化

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/client-web/pcQualityApiNormalization.test.ts",
      "label": "pcQualityApiNormalization.test",
      "path": "tests/client-web/pcQualityApiNormalization.test.ts",
      "doc": "docs/pseudocode/files/tests/client-web/pcQualityApiNormalization.test.ts.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "tests/client-web/pcQualityApiNormalization.test.ts", "to": "src/client-web/src/api/pcTracker.ts", "type": "tests" }
  ]
}
```
