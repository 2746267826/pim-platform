# Layer: infrastructure

节点总数（本层）: 85（图中展示前 40 个）

```mermaid
flowchart TB
  subgraph layer_infrastructure [infrastructure]
    n1["AiChatClientFactory.cs"]
    n2["AiGateway.cs"]
    n3["AiOptions.cs"]
    n4["AiProviderHealthService.cs"]
    n5["AiRedactor.cs"]
    n6["AiRequestLogWriter.cs"]
    n7["AiSchemaRegistry.cs"]
    n8["AiSchemaValidator.cs"]
    n9["AiUsageService.cs"]
    n10["DisabledAiGateway.cs"]
    n11["InMemoryAiSchemaRegistry.cs"]
    n12["AuditVersionEntity.cs"]
    n13["AuditVersionService.cs"]
    n14["CurrentUserService.cs"]
    n15["JwtService.cs"]
    n16["PasswordHasher.cs"]
    n17["AiProviderSettingEntity.cs"]
    n18["AiRequestLogEntity.cs"]
    n19["AuditLogEntity.cs"]
    n20["DaemonHeartbeatEntity.cs"]
    n21["LoginAttemptEntity.cs"]
    n22["OperationConfirmationEntity.cs"]
    n23["RefreshTokenEntity.cs"]
    n24["UserEntity.cs"]
    n25["20260524000000_BaselineExistingSchema.cs"]
    n26["20260524000000_BaselineExistingSchema..."]
    n27["20260524170037_Stage0OperationsTables.cs"]
    n28["20260524170037_Stage0OperationsTables..."]
    n29["20260525194000_AddPcActivityClassific..."]
    n30["20260525194000_AddPcActivityClassific..."]
    n31["20260526045819_AddQuickNotes.cs"]
    n32["20260526045819_AddQuickNotes.Designer.cs"]
    n33["20260526144517_Stage5CalendarTaskLoop.cs"]
    n34["20260526144517_Stage5CalendarTaskLoop..."]
    n35["20260527025542_AddAiGateway.cs"]
    n36["20260527025542_AddAiGateway.Designer.cs"]
    n37["20260527042125_AddFilesModule.cs"]
    n38["20260527042125_AddFilesModule.Designe..."]
    n39["20260705122322_AddPcRoute3Classificat..."]
    n40["20260705122322_AddPcRoute3Classificat..."]
  end
```

全量连接见 [交互图](../interactive/index.html)（按 layer 过滤 $layer）。
