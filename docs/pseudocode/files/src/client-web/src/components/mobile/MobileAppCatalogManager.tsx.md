# src/client-web/src/components/mobile/MobileAppCatalogManager.tsx

## 元信息
- 语言：TypeScript/TSX
- 程序集或包：client-web
- 职责：手机应用目录管理 UI——手动包名修正与批量分类规则的增删改表单与列表。
- 主要依赖：
  - `../../api/mobile` 类型与 `MOBILE_LIFE_CATEGORIES`
- 被谁使用：Mobile 管理/分析父页面

## 函数级结构化伪代码

### `readString(formData, key)` private
- 输入：FormData、字段名
- 输出：trim 后字符串或 `''`
- 副作用：无
- 步骤：get 后 typeof string 才 trim

### MobileAppCatalogManager（default）
#### 组件
- 输入：overrides、rules、loading/saving 标志、四个回调
- 输出：JSX 双栏管理区
- 副作用：通过回调把保存/删除交给父组件
- 步骤：
  1. 标题「应用管理」与保存状态文案。
  2. 左栏手动修正：表单 + 列表（保存/删除）。
  3. 右栏批量规则：表单 + 列表。
  4. 底部分类参考 select；loading 提示。
- 分支与异常：空 packageName/pattern 提交直接 return
- 调用：`handleOverrideSubmit`、`handleRuleSubmit`、props 回调

#### `handleOverrideSubmit(event)`
- 输入：表单 submit 事件
- 输出：void
- 副作用：`onSaveOverride`；reset 表单
- 步骤：
  1. preventDefault；读 packageName 小写，空则 return。
  2. 组装 override：显示名、lifeCategory 默认 CATEGORIES[15]、isSystemNoise/hideShortEvents 复选。
  3. onSaveOverride 后 reset。
- 调用：`readString`、`onSaveOverride`

#### `handleRuleSubmit(event)`
- 输入：表单 submit
- 输出：void
- 副作用：`onSaveRule`；reset
- 步骤：
  1. pattern 小写必填。
  2. ruleType 默认 package-prefix；priority 默认 100；分类默认 CATEGORIES[15]；启用/噪声复选。
  3. onSaveRule 后 reset。
- 调用：`readString`、`onSaveRule`

## 近逐行中文伪代码

1. 纯展示+表单组件，数据与持久化由父组件回调完成。
2. 手动修正：包名、显示名、生活分类、系统噪声/隐藏短事件。
3. 批量规则：类型、优先级、pattern、分类、启用/噪声。
4. 列表项可「保存」（原对象回传）与「删除」；空列表占位文案。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-web/src/components/mobile/MobileAppCatalogManager.tsx",
      "label": "MobileAppCatalogManager",
      "path": "src/client-web/src/components/mobile/MobileAppCatalogManager.tsx",
      "doc": "docs/pseudocode/files/src/client-web/src/components/mobile/MobileAppCatalogManager.tsx.md",
      "layer": "client-web",
      "kind": "ui"
    }
  ],
  "edges": [
    { "from": "src/client-web/src/components/mobile/MobileAppCatalogManager.tsx", "to": "src/client-web/src/api/mobile.ts", "type": "depends_on" }
  ]
}
```
