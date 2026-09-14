# tests/client-web/filesLocalization.test.ts

## 元信息
- 语言：TypeScript
- 程序集或包：tests/client-web
- 职责：静态扫描 `FilesPage.tsx`：禁止英文可见文案；强制关键中文 UI 字符串存在。
- 主要依赖：node:assert、fs、path
- 被谁使用：Node 测试执行

## 函数级结构化伪代码

### escapeRegExp(value)
- 输入：字符串
- 输出：转义后正则安全串
- 副作用：无
- 步骤：替换正则元字符
- 调用：`replace`

### hasVisibleText(text)
- 输入：候选可见文案
- 输出：boolean
- 副作用：无
- 步骤：
  1. 匹配引号包裹字面量
  2. 匹配 JSX 文本节点 `>text`
  3. 含 `?` 的 prompt 子串也算出现
- 分支与异常：无
- 调用：`RegExp.test`

### 主流程（模块顶层）
- 步骤：
  1. 读入 `src/client-web/src/pages/FilesPage.tsx`
  2. 对 `forbiddenVisibleText` 每项 assert 不存在
  3. 对 `requiredChineseText` 每项 assert.match 存在
- 分支与异常：assert 失败即测试失败
- 调用：`hasVisibleText`、`assert.equal`、`assert.match`

## 近逐行中文伪代码

1. [L5] 读取 FilesPage 源码
2. [L7-9] escapeRegExp
3. [L11-17] hasVisibleText 三种匹配
4. [L19-79] 英文禁用列表
5. [L81-87] 循环断言禁用文案不出现
6. [L89-114] 中文必填列表
7. [L116-118] 循环断言中文存在

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/client-web/filesLocalization.test.ts",
      "label": "filesLocalization.test",
      "path": "tests/client-web/filesLocalization.test.ts",
      "doc": "docs/pseudocode/files/tests/client-web/filesLocalization.test.ts.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    {
      "from": "tests/client-web/filesLocalization.test.ts",
      "to": "src/client-web/src/pages/FilesPage.tsx",
      "type": "tests"
    }
  ]
}
```
