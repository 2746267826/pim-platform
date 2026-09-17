# src/client-android/app/src/main/java/com/pim/app/mobile/usage/AppMetadataCollector.kt

## 元信息
- 语言：Kotlin
- 程序集或包：client-android / com.pim.app.mobile.usage
- 职责：从 PackageManager 采集应用元数据，产出 `MobileAppMetadataEntity` 列表（含 rawJson）。
- 主要依赖：Android Context/PackageManager、MobileAppMetadataEntity、Hilt `@ApplicationContext`
- 被谁使用：移动端 usage/sync 上传链路

## 函数级结构化伪代码

### AppMetadataCollector
#### collectForPackages(packageNames, collectedAtUtc)
- 输入：包名集合；采集时间 UTC 毫秒（默认 now）
- 输出：`List<MobileAppMetadataEntity>`
- 副作用：只读 PackageManager
- 步骤：
  1. 空集合直接返回 emptyList
  2. 过滤空白、去重
  3. 逐包 `collectPackage`，mapNotNull 丢弃失败项
- 分支与异常：NameNotFound/其它异常在 collectPackage 内吞掉返回 null
- 调用：collectPackage

#### collectInstalledApps(collectedAtUtc)
- 输入：采集时间
- 输出：已安装应用元数据列表
- 步骤：installedPackages → 包名序列 → 过滤/去重 → collectPackage
- 调用：installedPackages、collectPackage

#### collectPackage(packageManager, packageName, collectedAtUtc) [private]
- 输入：PM、包名、时间
- 输出：实体或 null
- 步骤：
  1. 取 PackageInfo / ApplicationInfo
  2. label = loadLabel，空白则回退 packageName
  3. installer、系统应用标志、category、versionCode
  4. 组装 MobileAppMetadataEntity 与 JSON rawJson
- 分支与异常：NameNotFoundException / Exception → null

#### installedPackages / packageInfo / applicationInfo / installerPackageName / appCategory / versionCode [private]
- 按 SDK 版本分流：TIRAMISU+ 用 Flags API；R+ 用 InstallSourceInfo；O+ category；P+ longVersionCode
- installer 失败返回 null；category UNDEFINED 视为 null

## 近逐行中文伪代码

1. Hilt 单例注入 ApplicationContext。
2. collectForPackages：空集返回；否则过滤空白去重后逐包采集。
3. collectInstalledApps：枚举已安装包名后同样逐包采集。
4. collectPackage：try 读 PackageInfo/ApplicationInfo。
5. 显示名优先 loadLabel，否则包名。
6. 计算 installer、是否系统应用、category、versionCode。
7. 构造实体字段与 rawJson（JSONObject 序列化）。
8. 包不存在或其它异常返回 null。
9. installedPackages/packageInfo/applicationInfo：API 33+ 用 PackageInfoFlags/ApplicationInfoFlags，否则旧 API。
10. installerPackageName：API 30+ getInstallSourceInfo，否则 getInstallerPackageName；异常 null。
11. appCategory：API 26+ 且非 UNDEFINED 才返回。
12. versionCode：API 28+ longVersionCode，否则 int 转 Long。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-android/app/src/main/java/com/pim/app/mobile/usage/AppMetadataCollector.kt",
      "label": "AppMetadataCollector",
      "path": "src/client-android/app/src/main/java/com/pim/app/mobile/usage/AppMetadataCollector.kt",
      "doc": "docs/pseudocode/files/src/client-android/app/src/main/java/com/pim/app/mobile/usage/AppMetadataCollector.kt.md",
      "layer": "client-android",
      "kind": "service"
    }
  ],
  "edges": [
    {
      "from": "src/client-android/app/src/main/java/com/pim/app/mobile/usage/AppMetadataCollector.kt",
      "to": "src/client-android/app/src/main/java/com/pim/app/data/MobileAppMetadataEntity.kt",
      "type": "depends_on"
    }
  ]
}
```
