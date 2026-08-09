# src/modules/Pim.Module.PcTracker/DTOs/PcTrackerDtos.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.PcTracker
- 职责：定义 PC 追踪模块上传、摘要、明细、分类规则与 ActivityWatch 完整上传等 API/服务 DTO（均为 record，无业务方法）。
- 主要依赖：`System.Text.Json.Serialization`（部分属性名映射）
- 被谁使用：PcTracker 端点、上传服务、质量服务、分类与详情查询等

## 函数级结构化伪代码

### KeystatsUploadRequest / AppStatEntry
#### 记录字段
- 输入：设备日度键鼠统计与可选 per-app 统计
- 输出：上传请求模型
- 副作用：无
- 步骤：承载 DeviceId/Date/按键点击/鼠标滚轮峰值与 `AppStats`
- 分支与异常：无
- 调用：无

### AwEventsUploadRequest / AwEventEntry
#### 记录字段
- 输入：设备 Id 与事件列表（时间戳、时长、类型、应用、标题、AFK）
- 输出：AW 事件上传模型
- 副作用：无
- 步骤：列表承载原始 AW 事件条目
- 分支与异常：无
- 调用：无

### PcSummaryResponse 及相关
#### KeystatsSummary / HeatmapBucket / AppRankingItem / TimelineItem / WorkSessionItem / DerivedMetrics / CategorySummary
- 输入：服务层组装的摘要数据
- 输出：`PcSummaryResponse` 聚合：键鼠摘要、热力、应用排名、时间线、会话、派生指标、分类汇总
- 副作用：无
- 步骤：各 record 描述展示字段（时长、分类色、置信度、切换频率等）
- 分支与异常：无
- 调用：无

### AppCategoryRule / SaveCategoryRequest
#### 记录字段
- 输入：规则 Id/模式/分类/颜色/优先级/是否内置；或保存请求四元组
- 输出：分类规则读写 DTO
- 副作用：无
- 步骤：纯数据
- 分支与异常：无
- 调用：无

### DetailQueryParams / DetailQueryResponse / PcDetailRecord / TypedDetailQueryResponse
#### 记录字段
- 输入：日期/维度/设备/应用/分类/键名/事件类型/排序/分页/域名标题 URL/视图等；明细行为字典或强类型 `PcDetailRecord`
- 输出：分页明细查询参数与响应
- 副作用：无
- 步骤：`PcDetailRecord` 扩展浏览器/分类/桶/RecordKey 元数据字段
- 分支与异常：无
- 调用：无

### HeatmapGridResponse
#### 记录字段
- 输入：二维热力桶、维度、最大键计数
- 输出：热力图网格响应
- 副作用：无
- 步骤：纯数据
- 分支与异常：无
- 调用：无

### AwInfoDto / AwBucketDto / CompleteAwEventEntry / CompleteAwUploadRequest
#### 记录字段
- 输入：AW 主机信息、桶元数据、带 SourceEventId 的完整事件、PimDeviceId 绑定
- 输出：完整 AW 上传协议 DTO
- 副作用：无
- 步骤：`device_id`/`last_updated` 用 `JsonPropertyName` 对齐 AW JSON
- 分支与异常：无
- 调用：无

### KeystatsSampleUploadRequest
#### 记录字段
- 输入：采样时刻与累计键鼠计数（含 peakKPS/peakCPS 等 JSON 别名）
- 输出：KeyStats 样本上传模型
- 副作用：无
- 步骤：字段对齐守护程序样本 JSON
- 分支与异常：无
- 调用：无

## 近逐行中文伪代码

1. 引入 Json.Serialization
2. 命名空间 `Pim.Module.PcTracker.DTOs`
3. 定义 Keystats 日上传统计与 AppStatEntry
4. 定义 AwEventsUploadRequest/AwEventEntry
5. 定义 PcSummaryResponse 及 KeystatsSummary、Heatmap、Ranking、Timeline、Session、Metrics、Category
6. 定义 AppCategoryRule、DetailQueryParams/Response、PcDetailRecord、TypedDetailQueryResponse
7. 定义 SaveCategoryRequest、HeatmapGridResponse
8. 定义 AwInfoDto/AwBucketDto/CompleteAw* 完整上传协议
9. 定义 KeystatsSampleUploadRequest（JSON 属性别名）
10. （全文件无方法体，仅 record 类型声明）

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.PcTracker/DTOs/PcTrackerDtos.cs",
      "label": "PcTrackerDtos",
      "path": "src/modules/Pim.Module.PcTracker/DTOs/PcTrackerDtos.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.PcTracker/DTOs/PcTrackerDtos.cs.md",
      "layer": "module.pctracker",
      "kind": "dto"
    }
  ],
  "edges": [
    { "from": "src/modules/Pim.Module.PcTracker", "to": "src/modules/Pim.Module.PcTracker/DTOs/PcTrackerDtos.cs", "type": "depends_on" }
  ]
}
```
