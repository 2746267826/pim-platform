# src/modules/Pim.Module.Files/Providers/NextcloudOptions.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.Files
- 职责：Nextcloud 提供商配置选项，当前仅定义 HTTP 请求超时默认 30 秒。
- 主要依赖：无（纯 options POCO）
- 被谁使用：Nextcloud 客户端/Provider 绑定与 Options 绑定

## 函数级结构化伪代码

### NextcloudOptions
#### 属性 TimeSpan RequestTimeout
- 输入：配置绑定或代码赋值
- 输出：超时时长
- 副作用：无
- 步骤：默认值 `TimeSpan.FromSeconds(30)`
- 分支与异常：无
- 调用：无

## 近逐行中文伪代码

1. 命名空间 `Pim.Module.Files.Providers`
2. sealed 类 `NextcloudOptions`
3. 属性 `RequestTimeout` 默认 30 秒

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.Files/Providers/NextcloudOptions.cs",
      "label": "NextcloudOptions",
      "path": "src/modules/Pim.Module.Files/Providers/NextcloudOptions.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.Files/Providers/NextcloudOptions.cs.md",
      "layer": "module.files",
      "kind": "other"
    }
  ],
  "edges": [
    { "from": "src/modules/Pim.Module.Files/Providers", "to": "src/modules/Pim.Module.Files/Providers/NextcloudOptions.cs", "type": "depends_on" }
  ]
}
```
