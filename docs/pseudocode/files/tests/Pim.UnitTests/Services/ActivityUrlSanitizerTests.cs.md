# tests/Pim.UnitTests/Services/ActivityUrlSanitizerTests.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.UnitTests
- 职责：活动 URL 清洗：去 query/fragment/userInfo；opaque 段 redacted；非 web 返回 null。
- 主要依赖：`ActivityUrlSanitizer.Sanitize`
- 被谁使用：dotnet test

## 函数级结构化伪代码

### Sanitize 多场景
- 去敏感；JWT 段 redacted；保留正常 slug；null/blank/invalid/file/data/javascript → null；百分号编码 opaque 同样 redacted

## 近逐行中文伪代码

1. [L8-46] 清洗与保留
2. [L48-81] null 与非 web
3. [L83-89] 编码 opaque

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/Pim.UnitTests/Services/ActivityUrlSanitizerTests.cs",
      "label": "ActivityUrlSanitizerTests",
      "path": "tests/Pim.UnitTests/Services/ActivityUrlSanitizerTests.cs",
      "doc": "docs/pseudocode/files/tests/Pim.UnitTests/Services/ActivityUrlSanitizerTests.cs.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "tests/Pim.UnitTests/Services/ActivityUrlSanitizerTests.cs", "to": "src/Pim.Module.PcTracker/Services/ActivityUrlSanitizer.cs", "type": "tests" }
  ]
}
```
