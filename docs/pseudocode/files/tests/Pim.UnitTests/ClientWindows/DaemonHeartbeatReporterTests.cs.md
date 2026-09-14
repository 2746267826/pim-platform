# tests/Pim.UnitTests/ClientWindows/DaemonHeartbeatReporterTests.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.UnitTests
- 职责：心跳默认 127.0.0.1:5858；StatusJson 含机器/进程；源状态；JSON 反序列化 API 契约。
- 主要依赖：DaemonHeartbeatReporter
- 被谁使用：dotnet test

## 函数级结构化伪代码

### BuildHeartbeat_UsesIpv4LoopbackDefaultServerUrl / UsesDefaultWhenBlank
### BuildHeartbeat_StatusJsonIncludesMachineAndProcess
### BuildHeartbeat_UsesProvidedSourceStatesAndStatusDetails
### ClientHeartbeatJson_DeserializesIntoApiRequestContract

## 近逐行中文伪代码

1. 默认 URL
2. StatusJson 字段
3. 源状态透传
4. 反序列化契约

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/Pim.UnitTests/ClientWindows/DaemonHeartbeatReporterTests.cs",
      "label": "DaemonHeartbeatReporterTests.cs",
      "path": "tests/Pim.UnitTests/ClientWindows/DaemonHeartbeatReporterTests.cs",
      "doc": "docs/pseudocode/files/tests/Pim.UnitTests/ClientWindows/DaemonHeartbeatReporterTests.cs.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [{"from":"tests/Pim.UnitTests/ClientWindows/DaemonHeartbeatReporterTests.cs","to":"src/client-windows/Pim.Client.Core/Services/DaemonHeartbeatReporter.cs","type":"tests"}]
}
```