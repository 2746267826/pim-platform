# 浏览器插件 CI 构建 + 连接状态面板 + 多浏览器多实例 实现计划

> **面向 AI 代理的工作者：** 必需子技能：使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 逐任务实现此计划。步骤使用复选框（`- [ ]`）语法来跟踪进度。

**目标：** 实现浏览器插件多实例身份识别、心跳携带 browser/instanceId、守护程序多实例管理、服务端字段扩展、面板浏览器连接板块、CI 自动构建并打包到 Windows 安装包。

**架构：** 插件端新增 helpers.ts 提供 getBrowserType/getInstanceId，heartbeat 携带身份；守护程序 BrowserBridgeService 改 ConcurrentDictionary 按 instanceId 管理，30s 超时检测；服务端 pc_tracker_events 新增 browser/instance_id 列并贯通上传；StatusWindow 新增浏览器连接板块按实例显示；CI 新增 browser-extension 工作流并集成到 Windows 发布。

**技术栈：** TypeScript + Vite (extension), C# .NET 8 WPF (client-windows), ASP.NET Core + EF Core + PostgreSQL (server), GitHub Actions + Inno Setup

---

## 文件结构

- **插件**
  - 创建：`pim-watcher-web/src/background/helpers.ts` — getBrowserType/getInstanceId
  - 修改：`pim-watcher-web/src/background/client.ts` — HeartbeatData 新增 browser/instanceId
  - 创建：`pim-watcher-web/src/background/heartbeat.ts` — 心跳组装
  - 创建：`pim-watcher-web/src/background/main.ts` — 后台入口（alarm、tab 监听）
  - 修改：`pim-watcher-web/src/manifest.json` — 完善 manifest
  - 创建：`pim-watcher-web/vite.config.ts` — 扩展构建配置
  - 创建：`pim-watcher-web/tsconfig.json`
  - 修改：`pim-watcher-web/package.json` — build/zip 脚本

- **客户端**
  - 修改：`src/client-windows/Pim.Client.Core/Models/TrackerModels.cs` — BrowserHeartbeat 新增 Browser/InstanceId，TrackerEventForUpload 新增 Browser/InstanceId，新增 BrowserConnection 模型
  - 修改：`src/client-windows/Pim.Client.Core/Services/BrowserBridgeService.cs` — ConcurrentDictionary 多实例、BuildDisplayName、CheckConnections、定时器
  - 修改：`src/client-windows/Pim.Client.Core/Services/NativeTrackerService.cs` — 上传携带 browser/instanceId，health 携带多实例统计
  - 修改：`src/client-windows/Pim.Client.Core/Services/TrackerSessionManager.cs` — 按实例更新（可选，保留 IsBrowserMediaActive 兼容）
  - 修改：`src/client-windows/Pim.Client.App/StatusWindow.xaml` — 新增“浏览器连接”板块
  - 修改：`src/client-windows/Pim.Client.App/StatusWindow.xaml.cs` — 渲染多实例状态、测试连接按钮、FormatAgo 等
  - 修改：`src/client-windows/Pim.Client.App/App.xaml.cs` — 可选健康上报扩展

- **服务端**
  - 修改：`src/modules/Pim.Module.PcTracker/Entities/TrackerEventEntity.cs` — 新增 Browser/InstanceId
  - 修改：`src/modules/Pim.Module.PcTracker/Entities/EntityConfigurations.cs` — 索引
  - 修改：`src/modules/Pim.Module.PcTracker/Services/PcTrackerSchemaInitializer.cs` — 建表+ALTER COLUMN
  - 修改：`src/modules/Pim.Module.PcTracker/DTOs/TrackerDtos.cs` — TrackerEventDto 新增 Browser/InstanceId
  - 修改：`src/modules/Pim.Module.PcTracker/Services/PcTrackerService.Tracker.cs` — Upload 处理新字段

- **CI/安装器**
  - 创建：`.github/workflows/build-browser-extension.yml` — 扩展构建
  - 修改：`.github/workflows/ci.yml` — 新增 changes.browserExtension、build-browser-extension job、release 资产
  - 修改：`.github/workflows/build-windows.yml` — 集成扩展产物到 publish/browser-extension
  - 修改：`installer/pim-setup.iss` — 可选携带扩展说明
  - 修改：`src/client-windows/publish` 包含 `browser-extension` 目录

- **测试**
  - 修改：`tests/Pim.UnitTests/ClientWindows/AwBucketSelectionTests.cs` — 补充 BrowserHeartbeat 新字段测试
  - 创建：`tests/Pim.UnitTests/ClientWindows/BrowserBridgeMultiInstanceTests.cs`
  - 创建：`tests/Pim.UnitTests/Services/PcTrackerBrowserFieldsTests.cs`
  - 修改/创建：`pim-watcher-web` 前端单元测试（可选 vitest）

---

### 任务 1：插件多实例支持

**文件：**
- 创建：`pim-watcher-web/src/background/helpers.ts`
- 修改：`pim-watcher-web/src/background/client.ts`
- 创建：`pim-watcher-web/src/background/heartbeat.ts`
- 修改：`pim-watcher-web/src/manifest.json`
- 创建：`pim-watcher-web/vite.config.ts`
- 创建：`pim-watcher-web/tsconfig.json`

- [ ] **步骤 1：创建 helpers.ts 实现 getBrowserType/getInstanceId**

```typescript
export function getBrowserType(): string {
    const ua = navigator.userAgent
    if (ua.includes('Chrome') && !ua.includes('Edg')) return 'chrome'
    if (ua.includes('Edg')) return 'edge'
    if (ua.includes('Firefox')) return 'firefox'
    if (ua.includes('Safari') && !ua.includes('Chrome')) return 'safari'
    return 'other'
}
export async function getInstanceId(): Promise<string> {
    const anyBrowser: any = (globalThis as any).browser ?? (globalThis as any).chrome
    if (anyBrowser?.runtime?.id) return anyBrowser.runtime.id
    const stored: any = await anyBrowser?.storage?.local?.get?.('instanceId') ?? {}
    if (stored?.instanceId) return stored.instanceId
    const uuid = crypto.randomUUID()
    await anyBrowser?.storage?.local?.set?.({ instanceId: uuid })
    return uuid
}
```

- [ ] **步骤 2：扩展 HeartbeatData 并携带身份**

```typescript
export interface HeartbeatData {
    url: string; title: string; audible: boolean; incognito: boolean; tabCount: number
    browser: string; instanceId: string
}
```

修改 sendHeartbeat 保持兼容，timestamp 自动附加。

- [ ] **步骤 3：创建 heartbeat.ts 组装并发送**

```typescript
import { getBrowserType, getInstanceId } from './helpers'
import { sendHeartbeat } from './client'
export async function heartbeat(tab: chrome.tabs.Tab, tabCount: number){
  const browserType = getBrowserType()
  const instanceId = await getInstanceId()
  await sendHeartbeat({
    url: decodeURL(tab.url ?? ''), title: tab.title ?? '',
    audible: tab.audible ?? false, incognito: tab.incognito ?? false,
    tabCount, browser: browserType, instanceId
  })
}
function decodeURL(u:string){ try{return decodeURIComponent(u)}catch{return u}}
```

- [ ] **步骤 4：补齐 vite.config.ts / tsconfig.json / manifest.json / main.ts**

vite.config.ts:
```typescript
import { defineConfig } from 'vite'
import { resolve } from 'path'
export default defineConfig({
  build:{
    outDir:'dist',
    lib:{ entry: resolve(__dirname,'src/background/main.ts'), name:'background', formats:['es'], fileName:()=>'background/main.js'},
    rollupOptions:{ output:{ entryFileNames:'background/main.js'}}
  }
})
```
main.ts 监听 alarms/tabs：每 30s heartbeat，启动 waitForPimClient。

manifest.json 保持现有并确保 host_permissions 含 localhost:15601。

- [ ] **步骤 5：本地验证构建**

运行：`npm --prefix pim-watcher-web install && npm --prefix pim-watcher-web run build`

预期：生成 dist/background/main.js 且 dist/manifest.json 存在。

- [ ] **步骤 6：Commit**

```bash
git add pim-watcher-web/src/background/helpers.ts pim-watcher-web/src/background/client.ts pim-watcher-web/src/background/heartbeat.ts pim-watcher-web/src/background/main.ts pim-watcher-web/vite.config.ts pim-watcher-web/tsconfig.json pim-watcher-web/package.json pim-watcher-web/src/manifest.json
git commit -m "feat(browser): add multi-instance helpers and heartbeat identity"
```

---

### 任务 2：客户端多实例管理

**文件：**
- 修改：`src/client-windows/Pim.Client.Core/Models/TrackerModels.cs`
- 修改：`src/client-windows/Pim.Client.Core/Services/BrowserBridgeService.cs`
- 修改：`src/client-windows/Pim.Client.Core/Services/NativeTrackerService.cs`
- 修改：`src/client-windows/Pim.Client.Core/Services/TrackerSessionManager.cs`（可选）

- [ ] **步骤 1：Models 增加 Browser/InstanceId 与 BrowserConnection**

```csharp
public sealed class BrowserHeartbeat
{
    [JsonPropertyName("browser")] public string? Browser { get; set; }
    [JsonPropertyName("instanceId")] public string? InstanceId { get; set; }
    // existing url/title/audible/incognito/tabCount/timestamp
}
public sealed class BrowserConnection
{
    public string InstanceId {get;set;}="";
    public string BrowserType {get;set;}="";
    public string DisplayName {get;set;}="";
    public bool IsConnected {get;set;}
    public DateTimeOffset LastHeartbeat {get;set;}
    public string? LastUrl {get;set;}
    public string? LastTitle {get;set;}
    public bool? LastAudible {get;set;}
    public int? LastTabCount {get;set;}
    public bool? LastIncognito {get;set;}
    public long HeartbeatCount {get;set;}
    public DateTimeOffset FirstSeen {get;set;}
}
public sealed class TrackerEventForUpload
{
    [JsonPropertyName("browser")] public string? Browser {get;set;}
    [JsonPropertyName("instanceId")] public string? InstanceId {get;set;}
}
```

- [ ] **步骤 2：重构 BrowserBridgeService 为 ConcurrentDictionary**

```csharp
class BrowserBridgeService {
  ConcurrentDictionary<string, BrowserConnection> _connections = new();
  public IReadOnlyDictionary<string, BrowserConnection> Connections => _connections;
  public BrowserHeartbeat? LastHeartbeat => _connections.Values.OrderByDescending(c=>c.LastHeartbeat).FirstOrDefault() is { } c ? new BrowserHeartbeat{ Url=c.LastUrl??"", Title=c.LastTitle??"", Audible=c.LastAudible??false, Incognito=c.LastIncognito??false, TabCount=c.LastTabCount??0, Browser=c.BrowserType, InstanceId=c.InstanceId } : _lastHeartbeatLegacy;
  public bool IsConnected => _connections.Values.Any(c=>c.IsConnected);
  void OnHeartbeat(BrowserHeartbeat hb){
    var conn = _connections.GetOrAdd(hb.InstanceId ?? "unknown", _ => new BrowserConnection{ InstanceId=hb.InstanceId??"unknown", BrowserType=hb.Browser??"other", DisplayName=BuildDisplayName(hb), FirstSeen=DateTimeOffset.UtcNow });
    conn.IsConnected=true; conn.LastHeartbeat=DateTimeOffset.UtcNow; conn.LastUrl=hb.Url; conn.LastTitle=hb.Title; conn.LastAudible=hb.Audible; conn.LastTabCount=hb.TabCount; conn.LastIncognito=hb.Incognito; conn.HeartbeatCount++;
    conn.DisplayName = BuildDisplayNameFor(conn); // 动态重算
    _channel.Writer.TryWrite(hb);
  }
  string BuildDisplayName(BrowserHeartbeat hb){ var type = hb.Browser switch {"chrome"=>"Chrome","edge"=>"Edge","firefox"=>"Firefox",_=>hb.Browser??"other"}; if(hb.Incognito) return $"{type} (无痕)"; var shortId = hb.InstanceId?.Length>4? hb.InstanceId[^4..]: hb.InstanceId??""; var sameTypeCount = _connections.Values.Count(c=>c.BrowserType==hb.Browser)+1; if(sameTypeCount<=1) return type; return $"{type} ({shortId})"; }
  void CheckConnections(){ foreach(var conn in _connections.Values){ if(conn.IsConnected && (DateTimeOffset.UtcNow-conn.LastHeartbeat).TotalSeconds>120){ conn.IsConnected=false; _logger?.Warn($"BrowserBridge {conn.DisplayName} disconnected, silent for {(DateTimeOffset.UtcNow-conn.LastHeartbeat).TotalSeconds:F0}s");}}}
}
```

增加 Timer 每 30s 调用 CheckConnections，公开 GetConnectionsSnapshot()。

保持向后兼容的 LastHeartbeat/IsConnected 供 NativeTrackerService/StatusWindow/旧测试。

- [ ] **步骤 3：适配 NativeTrackerService 上传携带 browser/instanceId**

在 SessionToEvents 中，若 session.PageVisits 有 BrowserHeartbeat 关联，填充 TrackerEventForUpload.Browser/InstanceId。也从 _bridge 索引最新连接的 Browser/InstanceId 作为回退。

在 Health 报告中增加 browserConnections 统计。

- [ ] **步骤 4：添加单元测试**

创建 `tests/Pim.UnitTests/ClientWindows/BrowserBridgeMultiInstanceTests.cs`:
- 同 brand 多实例 DisplayName 逻辑
- incognito 显示“无痕”
- 超时 120s 断开
- 并发 GetOrAdd

运行：`dotnet test --filter BrowserBridgeMultiInstance`

预期：全部 PASS。

- [ ] **步骤 5：Commit**

```bash
git add src/client-windows/Pim.Client.Core/Models/TrackerModels.cs src/client-windows/Pim.Client.Core/Services/BrowserBridgeService.cs src/client-windows/Pim.Client.Core/Services/NativeTrackerService.cs tests/Pim.UnitTests/ClientWindows/BrowserBridgeMultiInstanceTests.cs
git commit -m "feat(client): multi-instance browser bridge with display name and timeout"
```

---

### 任务 3：服务端字段扩展

**文件：**
- 修改：`src/modules/Pim.Module.PcTracker/Entities/TrackerEventEntity.cs`
- 修改：`src/modules/Pim.Module.PcTracker/Entities/EntityConfigurations.cs`
- 修改：`src/modules/Pim.Module.PcTracker/Services/PcTrackerSchemaInitializer.cs`
- 修改：`src/modules/Pim.Module.PcTracker/DTOs/TrackerDtos.cs`
- 修改：`src/modules/Pim.Module.PcTracker/Services/PcTrackerService.Tracker.cs`
- 创建：`src/Pim.Infrastructure/Data/Migrations/YYYYMMDD_AddBrowserInstanceId.cs`（可选，EF migration）

- [ ] **步骤 1：实体增加字段**

```csharp
[Column("browser")][MaxLength(16)] public string? Browser {get;set;}
[Column("instance_id")][MaxLength(128)] public string? InstanceId {get;set;}
```

EntityConfigurations 增加索引 idx_tracker_events_browser_instance。

- [ ] **步骤 2：SchemaInitializer 增加 ALTER**

```sql
ALTER TABLE pc_tracker_events ADD COLUMN IF NOT EXISTS browser VARCHAR(16);
ALTER TABLE pc_tracker_events ADD COLUMN IF NOT EXISTS instance_id VARCHAR(128);
CREATE INDEX IF NOT EXISTS idx_tracker_events_browser ON pc_tracker_events(browser);
CREATE INDEX IF NOT EXISTS idx_tracker_events_instance ON pc_tracker_events(instance_id);
```

在 CREATE TABLE 定义中也加入这两列。

- [ ] **步骤 3：DTO 增加字段**

```csharp
public record TrackerEventDto(..., string? Browser, string? InstanceId);
```

客户端上传 TrackerEventForUpload 已加 browser/instanceId，服务端映射填充 entity。

- [ ] **步骤 4：Service 处理新字段**

在 UploadTrackerEventsAsync 中写入 Browser/InstanceId，验证长度。

- [ ] **步骤 5：测试**

创建 `tests/Pim.UnitTests/Services/PcTrackerBrowserFieldsTests.cs` 验证上传携带 browser/instanceId 落库。

运行：`dotnet test --filter PcTrackerBrowserFields`

预期：PASS。

- [ ] **步骤 6：Commit**

```bash
git add src/modules/Pim.Module.PcTracker/Entities/TrackerEventEntity.cs src/modules/Pim.Module.PcTracker/Entities/EntityConfigurations.cs src/modules/Pim.Module.PcTracker/Services/PcTrackerSchemaInitializer.cs src/modules/Pim.Module.PcTracker/DTOs/TrackerDtos.cs src/modules/Pim.Module.PcTracker/Services/PcTrackerService.Tracker.cs
git commit -m "feat(server): add browser and instance_id to pc_tracker_events"
```

---

### 任务 4：面板浏览器连接板块

**文件：**
- 修改：`src/client-windows/Pim.Client.App/StatusWindow.xaml`
- 修改：`src/client-windows/Pim.Client.App/StatusWindow.xaml.cs`

- [ ] **步骤 1：XAML 新增浏览器连接板块**

在 TabControl 前或“概览”内增加或新增 Tab “浏览器”：

```xml
<TabItem Header="浏览器">
  <ScrollViewer Padding="16">
    <StackPanel>
      <TextBlock Text="浏览器连接" FontWeight="SemiBold"/>
      <ItemsControl x:Name="BrowserConnectionsList">
        <ItemsControl.ItemTemplate>
          <DataTemplate>
            <Border BorderThickness="1" CornerRadius="8" Padding="12" Margin="0,8,0,0">
              <StackPanel>
                <StackPanel Orientation="Horizontal">
                  <TextBlock Text="{Binding DisplayName}" FontWeight="SemiBold" Width="140"/>
                  <TextBlock Text="{Binding StatusText}" Margin="8,0,0,0"/>
                  <TextBlock Text="{Binding HeartbeatAgo}" Margin="8,0,0,0" Foreground="{StaticResource PimMutedTextBrush}"/>
                </StackPanel>
                <TextBlock Text="{Binding Url}" Foreground="{StaticResource PimMutedTextBrush}" TextTrimming="CharacterEllipsis"/>
                <TextBlock Text="{Binding Meta}" Foreground="{StaticResource PimMutedTextBrush}"/>
              </StackPanel>
            </Border>
          </DataTemplate>
        </ItemsControl.ItemTemplate>
      </ItemsControl>
      <TextBlock x:Name="BrowserEmptyText" Text="暂无浏览器连接" Visibility="Collapsed"/>
      <Button Content="🔌 测试连接" Style="{StaticResource PimSecondaryButton}" Margin="0,12,0,0" Click="OnTestBrowserConnection" HorizontalAlignment="Left"/>
    </StackPanel>
  </ScrollViewer>
</TabItem>
```

或在“数据源”板块下方内嵌（按工单图）。

- [ ] **步骤 2：Code-behind 渲染**

```csharp
private void RefreshBrowserConnections(){
  var conns = _bridge?.Connections.Values.OrderBy(c=>c.BrowserType).ThenBy(c=>c.InstanceId).ToList() ?? _tracker?.GetBrowserConnections() ?? new();
  if(conns.Count==0){ BrowserEmptyText.Visibility=Visibility.Visible; BrowserConnectionsList.ItemsSource=null; return; }
  BrowserEmptyText.Visibility=Visibility.Collapsed;
  var vms = conns.Select(c=>new {
    DisplayName=c.DisplayName,
    StatusText=c.IsConnected?"✅ 已连接":"❌ 未连接",
    HeartbeatAgo=c.IsConnected? $"{(int)(DateTimeOffset.UtcNow-c.LastHeartbeat).TotalSeconds}秒前" : "—",
    Url=c.LastUrl ?? "—",
    Meta=$"标签页: {c.LastTabCount?.ToString()??"—"} | 音频: {(c.LastAudible==true?"是 🔊":"否")} | 心跳累计: {c.HeartbeatCount} 次"
  }).ToList();
  BrowserConnectionsList.ItemsSource=vms;
}
private async void OnTestBrowserConnection(object s, RoutedEventArgs e){
  var url = $"http://localhost:{DaemonConfig.Load().Tracker.BrowserBridgePort}/browser/ping";
  var (ok,_,msg)=await ProbeEndpointAsync(url);
  MessageBox.Show(ok? $"连接正常：{msg}" : $"连接失败：{msg}", "PIM");
}
```

在 RefreshStatusAsync 末尾调用 RefreshBrowserConnections()。

增加 FormatAgo helper。

- [ ] **步骤 3：手工验证**

运行：`dotnet build src/client-windows/Pim.Client.App/Pim.Client.App.csproj`

预期：XAML 编译通过。

- [ ] **步骤 4：Commit**

```bash
git add src/client-windows/Pim.Client.App/StatusWindow.xaml src/client-windows/Pim.Client.App/StatusWindow.xaml.cs
git commit -m "feat(status): add browser connections panel with multi-instance display"
```

---

### 任务 5：CI + 安装器

**文件：**
- 创建：`.github/workflows/build-browser-extension.yml`
- 修改：`.github/workflows/ci.yml`
- 修改：`.github/workflows/build-windows.yml`
- 修改：`pim-watcher-web/package.json`
- （可选）修改：`installer/pim-setup.iss`

- [ ] **步骤 1：创建 build-browser-extension.yml**

```yaml
name: Build Browser Extension
on:
  workflow_call:
    inputs: { version:{required:true,type:string}, artifact_slug:{required:true,type:string}, git_sha_short:{required:true,type:string}}
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:{ node-version:'22', cache:'npm', cache-dependency-path:pim-watcher-web/package-lock.json}
      - run: npm ci
        working-directory: pim-watcher-web
      - run: npm run build
        working-directory: pim-watcher-web
      - run: npm run zip # 打包 zip
        working-directory: pim-watcher-web
      - uses: actions/upload-artifact@v7
        with:{ name: pim-browser-extension-v${{inputs.artifact_slug}}, path: pim-watcher-web/dist/*.zip }
```

pim-watcher-web/package.json 新增 `"zip": "vite build && cd dist && zip -r ../pim-browser-extension.zip ."` 或 node 脚本。

- [ ] **步骤 2：修改 ci.yml 增加 changes 与 jobs**

在 changes filters 增加：
```yaml
browserExtension:
  - 'pim-watcher-web/**'
  - '.github/workflows/build-browser-extension.yml'
```

flags 中输出 browserExtension。

新增 job build-browser-extension 依赖 resolve-version/changes，condition 为 all||browserExtension。

release 中增加下载与填充：

```bash
fill_if_skipped "${{ needs.build-browser-extension.result }}" 'pim-browser-extension-*.zip'
```

并上传 release-assets/pim-browser-extension-*.zip。

- [ ] **步骤 3：修改 build-windows.yml 集成扩展**

在 Publish Daemon 之后新增步骤：

```yaml
- name: Build and include Browser Extension
  shell: pwsh
  run: |
    cd ../../pim-watcher-web
    npm ci
    npm run build
    $dest = "../src/client-windows/publish/browser-extension"
    New-Item -ItemType Directory -Force -Path $dest | Out-Null
    Copy-Item -Recurse dist/* $dest -Force
```

确保 publish/browser-extension 随 zip/安装包一起打包。

在 Windows build 成功时也上传制品 `pim-browser-extension-v...zip`（或复用 browser-extension job 的产物，仅集成目录即可满足“打包到 Windows 安装包”）。

- [ ] **步骤 4：验证 CI**

运行：`act -j build-browser-extension` 或本地模拟 `npm run build`。

或：`gh workflow view` 检查语法。

运行：`dotnet build` 验证未破坏。

- [ ] **步骤 5：Commit**

```bash
git add .github/workflows/build-browser-extension.yml .github/workflows/ci.yml .github/workflows/build-windows.yml pim-watcher-web/package.json pim-watcher-web/vite.config.ts
git commit -m "ci(browser): build extension and pack into windows installer"
```

---

## 自检

**规格覆盖度：**
- 身份标识 browserType+instanceId -> 任务1
- 心跳协议 browser/instanceId -> 任务1+2
- 显示名称 Chrome (a3f2)/无痕/单实例简化 -> 任务2
- 多实例管理 ConcurrentDictionary/超时 -> 任务2
- 面板显示浏览器连接板块/动态命名/测试连接 -> 任务4
- 插件多实例支持 helpers -> 任务1
- 服务端字段扩展 browser/instance_id -> 任务3
- CI 自动构建+打包到安装包 -> 任务5
全部覆盖。

**占位符扫描：** 已替换为具体代码块与命令。

**类型一致性：** BrowserHeartbeat.Browser/InstanceId 在 Models/DTOs/Entity 保持 string?，长度 16/128，DisplayName 计算一致。

