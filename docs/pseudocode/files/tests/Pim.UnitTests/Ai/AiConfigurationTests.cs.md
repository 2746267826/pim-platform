# tests/Pim.UnitTests/Ai/AiConfigurationTests.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.UnitTests
- 职责：校验 appsettings/docker-compose/.env.example 中 LiteLLM 默认与密钥分离。
- 主要依赖：仓库根配置文件
- 被谁使用：dotnet test

## 函数级结构化伪代码

### Appsettings / Development 默认
- Enabled=false；Provider=litellm；BaseUrl 分别为 litellm:4000 / 127.0.0.1:4000；Token/尝试次数等

### DockerCompose_AddsLiteLlmServiceAndApiEnvironment
- pim-api Ai__* 环境；litellm 镜像/config/DB；litellm-db-init 建库

### EnvExample_SeparatesVirtualAndMasterLiteLlmKeys
- VIRTUAL ≠ MASTER；注释约束

## 近逐行中文伪代码

1. [L8-22] appsettings
2. [L24-62] compose 片段
3. [L64-75] env.example
4. [L77-119] 读文件/ExtractService/ReadEnv

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/Pim.UnitTests/Ai/AiConfigurationTests.cs",
      "label": "AiConfigurationTests",
      "path": "tests/Pim.UnitTests/Ai/AiConfigurationTests.cs",
      "doc": "docs/pseudocode/files/tests/Pim.UnitTests/Ai/AiConfigurationTests.cs.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "tests/Pim.UnitTests/Ai/AiConfigurationTests.cs", "to": "src/Pim.Api/appsettings.json", "type": "tests" },
    { "from": "tests/Pim.UnitTests/Ai/AiConfigurationTests.cs", "to": "docker-compose.yml", "type": "tests" }
  ]
}
```
