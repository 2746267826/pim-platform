# tests/client-web/localizationSmoke.test.ts

## 元信息
- 语言：TypeScript (node:assert + fs)
- 程序集或包：tests/client-web
- 职责：跨 Web/API/模块源文件的本地化冒烟：禁止未翻译英文可见文案，并要求关键中文 UI 文案出现。
- 主要依赖：node:fs、node:path；扫描多条业务源路径
- 被谁使用：测试脚本运行

## 函数级结构化伪代码

### 模块顶层
#### 聚合 source
- 输入：files 常量列表
- 输出：拼接后的 source 大字符串
- 副作用：同步读盘
- 步骤：对每个路径 readFileSync，前缀路径名后 join
- 调用：`readFileSync`、`resolve`

#### escapeRegExp(value)
- 输入：字符串
- 输出：转义后的正则字面量
- 步骤：转义正则特殊字符

#### 禁止模式循环
- 步骤：对 forbiddenVisiblePatterns 每个 pattern，assert source 不匹配

#### 必需中文循环
- 步骤：对 requiredChineseText 每个 text，assert source 匹配转义正则

## 近逐行中文伪代码

1. [L1-L3] 导入 assert、fs、path
2. [L5-L34] files 列表：client-web 组件/API、Pim.Api、Core/Infra AI、Files/Calendar/QuickNotes/PcTracker 模块
3. [L36-L38] 读入并拼接全部源
4. [L40-L42] escapeRegExp
5. [L44-L108] 禁止英文可见文案/错误短语/Ok 短状态串等
6. [L110-L116] 逐 pattern 断言不出现
7. [L118-L186] 必需中文文案列表
8. [L188-L190] 逐 text 断言必须出现

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/client-web/localizationSmoke.test.ts",
      "label": "localizationSmoke.test",
      "path": "tests/client-web/localizationSmoke.test.ts",
      "doc": "docs/pseudocode/files/tests/client-web/localizationSmoke.test.ts.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    {
      "from": "tests/client-web/localizationSmoke.test.ts",
      "to": "src/client-web/src/api/client.ts",
      "type": "tests"
    },
    {
      "from": "tests/client-web/localizationSmoke.test.ts",
      "to": "src/Pim.Api/Middleware/ExceptionMiddleware.cs",
      "type": "tests"
    },
    {
      "from": "tests/client-web/localizationSmoke.test.ts",
      "to": "src/modules/Pim.Module.Files/FilesModule.cs",
      "type": "tests"
    }
  ]
}
```
