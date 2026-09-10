# tests/Pim.UnitTests/Services/PcCategoryServiceTests.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.UnitTests
- 职责：`SeedDefaultsAsync` 幂等补齐内置分类、同名不重复、子类挂已有同名父。
- 主要依赖：`PcCategoryService`、`PcCategoryEntity`
- 被谁使用：xUnit

## 函数级结构化伪代码

### PcCategoryServiceTests
#### SeedDefaultsAsync_AddsMissingBuiltinsWhenCategoriesAlreadyExist()
- 输入：已有 Custom 分类
- 输出：无
- 副作用：写内置分类
- 步骤：
  1. Seed 后含 Custom + 编程/终端/沟通/办公/文件/浏览/学习/娱乐/其他
  2. 再次 Seed 计数不变
- 分支与异常：无
- 调用：`PcCategoryService.SeedDefaultsAsync`

#### SeedDefaultsAsync_DoesNotDuplicateExistingSameNameCategory()
- 输入：已有「终端」非内置
- 输出：无
- 副作用：Seed
- 步骤：同名仅一条
- 分支与异常：无
- 调用：同上

#### SeedDefaultsAsync_UsesExistingSameNameParentForMissingChildren()
- 输入：已有「工作」父节点
- 输出：无
- 副作用：Seed 子分类
- 步骤：新「编程」ParentId=既有工作 Id
- 分支与异常：无
- 调用：同上

#### CreateDb()
- 输入：无
- 输出：InMemory PimDbContext
- 副作用：注册 PcTracker 程序集
- 步骤：UseInMemoryDatabase
- 分支与异常：无
- 调用：`PimDbContext`

## 近逐行中文伪代码

1. 已有自定义时补齐内置且幂等
2. 同名不重复
3. 子类复用已有父 Id
4. CreateDb

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/Pim.UnitTests/Services/PcCategoryServiceTests.cs",
      "label": "PcCategoryServiceTests",
      "path": "tests/Pim.UnitTests/Services/PcCategoryServiceTests.cs",
      "doc": "docs/pseudocode/files/tests/Pim.UnitTests/Services/PcCategoryServiceTests.cs.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "tests/Pim.UnitTests/Services/PcCategoryServiceTests.cs", "to": "src/modules/Pim.Module.PcTracker/Services/PcCategoryService.cs", "type": "tests" }
  ]
}
```
