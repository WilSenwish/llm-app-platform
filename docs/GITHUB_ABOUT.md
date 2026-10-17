# GitHub 仓库 About & Topics

> 用于仓库 Settings → About 的「Description」与「Topics」字段。复制对应内容粘贴即可。

## 一、About 简介（Description）

**中文主版本（推荐）**
```
插件化通用 LLM 应用平台：以自研 SPI 插件内核统一承载 RAG 问答、CodeAgent、MCP 工具智能体与多 Agent 编排，内置独立 AI 模型网关、OpenTelemetry 全链路可观测与高可用底座。
```

**英文备选（提升国际可检索性）**
```
Plugin-based general-purpose LLM application platform: a self-built SPI plugin kernel hosting RAG, CodeAgent, MCP tool agents and multi-agent orchestration, with a standalone AI model gateway, full OpenTelemetry observability and a high-availability foundation.
```

**超短版（≤80 字，适合作为首屏一句话）**
```
插件化通用 LLM 平台 · SPI 插件内核 + 独立模型网关 + 全链路可观测底座
```

## 二、Topics（标签）

在 Settings → About → Topics 中逐个添加（不区分大小写，建议小写连字符；上限 20 个，已精选 16 个）：

```
llm
llm-application
rag
agent
multi-agent
mcp
plugin-architecture
spring-boot
spring-ai
java
observability
opentelemetry
resilience4j
code-agent
gateway
milvus
```

> 可按需追加（未超出 20 上限）：`vector-database`、`prompt-engineering`、`resume-project`、`high-availability`

## 三、可选补充字段

- **Website**：暂留空；若要展示在线文档，可填任意托管页（如 GitHub Pages / 腾讯文档）。
- **分类（Category）**：`Machine Learning` 或 `Developer Tool`。
- **Badge 建议**（放 README 顶部，增强观感）：
  - 构建：`Java 17` · `Spring Boot 3.3` · `Maven`
  - 能力：`SPI 插件化` · `OpenTelemetry 可观测` · `Resilience4j 高可用`

## 四、与 README 定位的对应关系

README 首句定位可直接复用作 About 文案基础：
> 自研通用 LLM 应用运行平台。RAG 仅作为可插拔插件，平台统一承载：通用对话、RAG 问答、MCP 工具 Agent、CodeAgent、结构化抽取、多 Agent 编排、离线批量任务。
