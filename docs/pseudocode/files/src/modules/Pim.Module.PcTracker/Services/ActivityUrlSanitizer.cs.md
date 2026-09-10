# src/modules/Pim.Module.PcTracker/Services/ActivityUrlSanitizer.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.PcTracker
- 职责：将活动 URL 消毒为可安全展示/聚类的形态：仅 http(s)、剥离用户凭据与查询/片段，路径中疑似 token 段替换为 `[redacted]`。
- 主要依赖：`System.Text.RegularExpressions`（GeneratedRegex）
- 被谁使用：活动建议/分类上下文构建、时间线展示侧（同模块服务）

## 函数级结构化伪代码

### ActivityUrlSanitizer
#### `static string? Sanitize(string? url)`
- 输入：原始 URL 或 null
- 输出：消毒后绝对 URL 字符串，或 null
- 副作用：无
- 步骤：
  1. 空白 → null
  2. `Uri.TryCreate` Absolute 失败 → null
  3. Scheme 非 http/https → null
  4. UriBuilder：清空 UserName/Password/Query/Fragment
  5. AbsolutePath 按 `/` 分段；`LooksSensitive(segment)` 则替换为 `[redacted]`
  6. 拼 Path；返回 `ToString().TrimEnd('/')`
- 分支与异常：不抛；解析失败静默 null
- 调用：`LooksSensitive`、`UriBuilder`

#### `static bool LooksSensitive(string segment)`
- 输入：路径段
- 输出：是否敏感
- 副作用：无
- 步骤：
  1. `Uri.UnescapeDataString`
  2. 长度 ≥ 24 且（DottedToken 匹配 或 LooksLikeNonDottedToken）
- 分支与异常：无
- 调用：`DottedTokenRegex`、`LooksLikeNonDottedToken`

#### `static bool LooksLikeNonDottedToken(string segment)`
- 输入：段
- 输出：bool
- 副作用：无
- 步骤：
  1. 非 `^[A-Za-z0-9_-]+$` → false
  2. 数字数≥8 或（有数字且大小写混）或（含 `_` 且大小写混）→ true
- 分支与异常：无
- 调用：`NonDottedTokenRegex`

#### `DottedTokenRegex` / `NonDottedTokenRegex`（GeneratedRegex）
- 输入：字符串
- 输出：Regex 匹配
- 副作用：无
- 步骤：点分 token 至少两段点；非点分字母数字下划线横线
- 分支与异常：无
- 调用：无

## 近逐行中文伪代码

1. partial static 类 ActivityUrlSanitizer
2. Sanitize：空/非绝对/非 http(s) → null
3. 清凭据与 query/fragment；路径敏感段 → [redacted]
4. LooksSensitive：解码后长≥24 且像 token
5. LooksLikeNonDottedToken：字符集 + 数字/大小写启发式
6. GeneratedRegex 两个模式

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.PcTracker/Services/ActivityUrlSanitizer.cs",
      "label": "ActivityUrlSanitizer",
      "path": "src/modules/Pim.Module.PcTracker/Services/ActivityUrlSanitizer.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.PcTracker/Services/ActivityUrlSanitizer.cs.md",
      "layer": "module.pctracker",
      "kind": "service"
    }
  ],
  "edges": [
    { "from": "src/modules/Pim.Module.PcTracker/Services", "to": "src/modules/Pim.Module.PcTracker/Services/ActivityUrlSanitizer.cs", "type": "calls" }
  ]
}
```
