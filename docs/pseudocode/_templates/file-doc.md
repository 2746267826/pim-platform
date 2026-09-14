# <source-relative-path>

## 元信息
- 语言：
- 程序集或包：
- 职责：
- 主要依赖：
- 被谁使用：

## 函数级结构化伪代码

### <TypeName>
#### <MethodSignature>
- 输入：
- 输出：
- 副作用：
- 步骤：
  1. ...
- 分支与异常：
- 调用：

## 近逐行中文伪代码

1. ...
2. ...

## 关系边
```json
{
  "nodes": [
    {
      "id": "<source-relative-path>",
      "label": "<TypeOrFileName>",
      "path": "<source-relative-path>",
      "doc": "docs/pseudocode/files/<source-relative-path>.md",
      "layer": "<layer>",
      "kind": "<kind>"
    }
  ],
  "edges": [
    { "from": "<path-or-type>", "to": "<path-or-type>", "type": "depends_on|calls|implements|extends|tests|http" }
  ]
}
```

`layer` 取值：`core` | `infrastructure` | `api` | `module.stats` | `module.quicknotes` | `module.files` | `module.mobile` | `module.pctracker` | `module.calendar` | `client-web` | `client-windows` | `client-android` | `tests`

`kind` 取值：`entrypoint` | `endpoint` | `service` | `entity` | `dto` | `middleware` | `ui` | `test` | `other`
