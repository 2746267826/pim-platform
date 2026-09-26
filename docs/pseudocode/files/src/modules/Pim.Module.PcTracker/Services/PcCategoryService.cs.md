# src/modules/Pim.Module.PcTracker/Services/PcCategoryService.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.PcTracker
- 职责：PC 分类树 CRUD、重排与内置种子数据幂等初始化；输出 `CategoryTreeNode`。
- 主要依赖：`PimDbContext`、`PcCategoryEntity`、`AppSignatureEntity`、PcTracker DTOs
- 被谁使用：`PcTrackerModule`（`/api/v1/pc/categories*` 与 Initialize 种子）

## 函数级结构化伪代码

### PcCategoryService
#### 构造 `PcCategoryService(db)`
- 输入：DbContext
- 输出：实例
- 副作用：保存 `_db`
- 步骤：注入
- 分支与异常：无
- 调用：无

#### `GetTreeAsync(ct)`
- 输入：取消令牌
- 输出：根到叶 `List<CategoryTreeNode>`
- 副作用：只读查询
- 步骤：全表按 SortOrder、Name 排序 → `BuildTree(all, null)`
- 分支与异常：无
- 调用：`BuildTree`

#### `BuildTree(all, parentId)`
- 输入：全部分类、父 Id
- 输出：该父下节点列表（递归 Children）
- 副作用：无
- 步骤：筛选 ParentId==parentId；映射字段并递归 `BuildTree(all, c.Id)`
- 分支与异常：无
- 调用：自身递归

#### `SaveAsync(req, ct)`
- 输入：`CategorySaveRequest`（可选 Id）
- 输出：单节点 `CategoryTreeNode`（Children 空列表）
- 副作用：Insert 或 Update + SaveChanges
- 步骤：
  1. 有 Id：Find 否则 KeyNotFoundException「分类不存在」；更新 Name/Color/Icon/Productivity/ParentId/SortOrder/UpdatedAt
  2. 无 Id：新建 Guid、IsBuiltin=false、时间戳，Add
  3. SaveChanges；`MapToNode(entity, [])`
- 分支与异常：找不到抛 KeyNotFoundException
- 调用：`MapToNode`

#### `DeleteAsync(id, ct)`
- 输入：分类 Id
- 输出：bool（false=不存在或内置）
- 副作用：删除行 + SaveChanges
- 步骤：
  1. Include Children 查实体
  2. null 或 IsBuiltin → false
  3. 有子分类 → InvalidOperationException 要求先删子
  4. 查询 AppSignature CategoryPath 是否 Contains 名称（结果 hasRefs 未用于阻断）
  5. Remove + SaveChanges → true
- 分支与异常：有子抛冲突类异常
- 调用：EF

#### `ReorderAsync(req, ct)`
- 输入：`ReorderCategoriesRequest.Items`（Id、ParentId、SortOrder）
- 输出：无
- 副作用：批量更新 ParentId/SortOrder/UpdatedAt
- 步骤：按 Id 列表加载；逐项匹配更新；SaveChanges
- 分支与异常：缺失 Id 跳过
- 调用：EF

#### `SeedDefaultsAsync(ct)`
- 输入：取消令牌
- 输出：无
- 副作用：仅插入缺失的内置分类
- 步骤：
  1. 硬编码娱乐/工作/学习/沟通/其他树（固定 Guid、IsBuiltin=true）
  2. 读现有 Id 与按名分组
  3. 对每个种子：Id 已存在或同名已存在则 resolved 映射；否则解析 ParentId 经 resolvedIds，加入 missing
  4. missing 空则 return；否则 AddRange + SaveChanges
- 分支与异常：无抛
- 调用：EF

#### `MapToNode(entity, children)`
- 输入：实体与子列表
- 输出：CategoryTreeNode
- 副作用：无
- 步骤：字段拷贝
- 分支与异常：无
- 调用：无

## 近逐行中文伪代码

1. 注入 PimDbContext
2. GetTreeAsync：排序全表 → 递归 BuildTree
3. SaveAsync：更新或新建用户分类，非内置
4. DeleteAsync：禁删内置；有子则抛；再删（hasRefs 查询未拦截）
5. ReorderAsync：批量改父与排序
6. SeedDefaultsAsync：固定 Guid 树；按 Id/名幂等补缺
7. MapToNode 扁平映射

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.PcTracker/Services/PcCategoryService.cs",
      "label": "PcCategoryService",
      "path": "src/modules/Pim.Module.PcTracker/Services/PcCategoryService.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.PcTracker/Services/PcCategoryService.cs.md",
      "layer": "module.pctracker",
      "kind": "service"
    }
  ],
  "edges": [
    { "from": "src/modules/Pim.Module.PcTracker/Services/PcCategoryService.cs", "to": "src/Pim.Infrastructure/Data/PimDbContext.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.PcTracker/Services/PcCategoryService.cs", "to": "src/modules/Pim.Module.PcTracker/Entities/PcCategoryEntity.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.PcTracker/Services/PcCategoryService.cs", "to": "src/modules/Pim.Module.PcTracker/Entities/AppSignatureEntity.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.PcTracker/Services/PcCategoryService.cs", "to": "src/modules/Pim.Module.PcTracker/DTOs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.PcTracker/PcTrackerModule.cs", "to": "src/modules/Pim.Module.PcTracker/Services/PcCategoryService.cs", "type": "calls" }
  ]
}
```
