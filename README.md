# llm-app-platform · 插件化 LLM 应用平台

> 自研通用 LLM 应用运行平台。**RAG 仅作为可插拔插件**，平台统一承载：通用对话、RAG 问答、MCP 工具 Agent、CodeAgent、结构化抽取、多 Agent 编排、离线批量任务。

## 项目亮点（简历 / 面试重点）

- **插件化内核，RAG 只是其中一种能力插件**：基于自研 SPI 插件体系 + 插件注册中心，将通用对话、RAG 问答、MCP 工具 Agent、CodeAgent、结构化抽取、多 Agent 编排、离线批量任务统一承载在同一平台，可热插拔、可独立迭代。
- **独立 AI 模型网关（自研）**：统一多模型路由、健康探测、故障自动降级、令牌桶限流与 SSE 流式透传，与业务插件彻底解耦。
- **生产级可观测性（自研 Harness）**：以 Micrometer Observation 为埋点底座，经 OpenTelemetry Collector 统一汇聚至 Prometheus（指标）/ Loki（日志）/ Grafana（大盘）+ AlertManager（告警）。
- **稳定性工程**：Resilience4j 熔断 + 舱壁隔离（按插件 / 任务类型做故障隔离），支持多实例高可用与异步任务队列削峰。
- **质量保障闭环**：Gatling 混合流量压测 + Chaos 故障注入演练，形成「压测 → 演练 → 告警 → 复盘」的可靠性闭环。

## 技术栈

| 维度 | 选型 |
| --- | --- |
| 语言 / 构建 | Java 17、Maven 3.9（多模块） |
| 框架 | Spring Boot 3.3、Spring AI（week2 接入） |
| 插件化 | SPI + 插件注册中心（自研） |
| 可观测 | Micrometer Observation → OpenTelemetry → Prometheus / Loki / Grafana |
| 中间件 | MySQL、Redis、Milvus、RabbitMQ（OrbStack 编排） |
| 稳定性 | Resilience4j（熔断 / 舱壁 / 限流） |

## 模块结构

```
llm-app-platform/
├── platform-core/      平台内核：SPI 插件规范、插件注册中心、前置编排与三层执行器（纯底座）
├── plugin-rag/         RAG 问答插件（可插拔，独立模块）
├── gateway-service/    （week2 规划）独立 AI 模型网关微服务
├── refs/               只读参考仓库（.gitignore 忽略，不提交主仓库）
├── docs/               架构图、trade-off 决策、故障手册
├── compose/            OrbStack docker-compose 编排（中间件 + 观测栈）
├── grafana/            Grafana 大盘 JSON、AlertManager 告警规则
├── harness/            Gatling 压测脚本、评测数据集、故障注入脚本
└── src/                模块源码根目录占位（见 src/README.md）
```

## 快速开始

> 构建要求 **JDK 17**（本机通过 mise 锁定 `zulu-17`）。Maven 使用国内 Aliyun 镜像。

```bash
# 1. 克隆 P0 参考仓库（仅只读参考，不提交）
mkdir -p refs
git clone --depth 1 https://gitee.com/nageoffer/ragent.git            refs/ragent
git clone --depth 1 https://gitee.com/java-up-up/damai-ai.git         refs/damai-ai
# git clone --depth 1 https://github.com/913165/springai-Observability-demo.git refs/springai-observability-demo  # 已评估，与本项目 OTEL 栈不符，未保留
git clone --depth 1 https://gitee.com/ageerle/ruoyi-ai.git            refs/ruoyi-ai
git clone --depth 1 https://gitee.com/gx729/nexus-agent.git           refs/nexus-agent

# 2. 编译并运行单元测试（JDK 17）
export JAVA_HOME=<你的 zulu-17 JAVA_HOME>
mvn -B clean test

# 3. 启动观测栈（week4 接入；需 OrbStack）
cd compose && docker compose -f observability-stack.yml up -d
```

## 参考仓库（仅只读，不提交主仓库）

> 各仓库的**学习优先级与逐周阅读路径**见 **[docs/reference-study-guide.md](docs/reference-study-guide.md)**。

**P0（已规划拉取）**
- `refs/ragent` — nageoffer/ragent
- `refs/damai-ai` — java-up-up/damai-ai（网关逻辑参考）
- `refs/springai-observability-demo` — 913165/springai-Observability-demo（已评估：仅验证 Spring AI 原生 Observation，但采用 Brave/Zipkin，与本项目 OTEL 栈不符，未保留本地副本）
- `refs/ruoyi-ai` — ageerle/ruoyi-ai（MCP、模型管理、流程编排）
- `refs/nexus-agent` — gx729/nexus-agent（Super-Agent：前置编排、三层执行器、双通道 RAG、证据治理）

**P1 / P2（按需）**
- `refs/langchain4j-examples`（P1）
- `refs/springai-samples`（P2）

**补充参考（Spring AI Alibaba 生态，对应 Week5–7：CodeAgent / 多 Agent 编排 / 评估）**
> 以下 5 个为后续补充的只读参考，均基于 Spring AI Alibaba（Java 17 / Spring Boot 3.x / Spring AI 1.x），与本项目技术栈一致，可作为 Week5–7 插件实现的样板：
- `refs/AssistantAgent-main` — Spring AI Alibaba **AssistantAgent**：Code-as-Action（代码生成 + GraalVM 沙箱执行）、多维评估图（Evaluation Graph）、动态 Prompt 构建、统一经验/Skill 体系 → 对应 plugin-code-agent、week7 评估
- `refs/deepresearch-main` — Spring AI Alibaba **DeepResearch**：基于 Spring AI Alibaba Graph 的 Multi-Agent 研究 Agent，Plan&Execute、Hybrid RAG（向量+关键词）、Reflection、HITL、自进化 Memory、MCP 分配、安全沙箱 → 对应 plugin-multi-agent、week5 沙箱
- `refs/Lynxe-main` — Spring AI Alibaba **Lynxe**（原 JManus）：纯 Java 版 Manus，确定性探索型任务、HTTP 服务调用、可嵌入现有项目 → 对应 plugin-multi-agent / Workflow 编排（week6）
- `refs/DataAgent-main` — Spring AI Alibaba **DataAgent**：企业级数据分析师，Text-to-SQL、Python 深度分析、智能报告、MCP Server、RAG 增强 → 对应 plugin-extractor / 数据类插件（week5）
- `refs/agentscope-examples` — **AgentScope Java** 框架示例集（多 Agent 协作、工具调用等核心特性 Demo）→ 框架级参考（week5/6）

## 8 周落地计划概览

> 详细路线图（逐周任务 / 交付物 / 状态 / 甘特图）见 **[docs/ROADMAP.md](docs/ROADMAP.md)**。状态：✅ 已完成 · 🔄 进行中 · ⬜ 未开始

| 周次 | 重点 | 优先级 | 状态 |
| --- | --- | --- | --- |
| 第0周 | 目录初始化、拉取 P0 参考仓库、工程骨架、Cursor 上下文 | P0 | ✅ 已完成 |
| 第1周 | 平台内核 + SPI 插件体系 + plugin-rag（前置编排/三层执行器） | P0 | 🔄 进行中 |
| 第2周 | 独立 AI 模型网关微服务（gateway-service） | P0 | ⬜ 未开始 |
| 第3周 | 高可用底座改造 + 异步任务队列（Redis / RabbitMQ / Resilience4j） | P0 | ⬜ 未开始 |
| 第4周 | 全链路可观测 Harness（OTEL + Prometheus + Loki + Grafana + AlertManager） | P0 | ⬜ 未开始 |
| 第5周 | plugin-code-agent + plugin-extractor | P1 | ⬜ 未开始 |
| 第6周 | Workflow 编排引擎 + plugin-multi-agent | P1 | ⬜ 未开始 |
| 第7周 | 统一评估平台 + 压测 Harness + 故障注入演练 | P1 | ⬜ 未开始 |
| 第8周 | 安全加固、文档、面试题库、简历项目描述、Open-WebUI 面试官 Agent | P0 | ⬜ 未开始 |

## 面试关键话术：如何讲清「自研」与「参考」的边界

> **面试官常问**：这个项目是不是基于 RuoYi-AI / Super-Agent 改的？
>
> **回答要点（结论先行）**：核心架构与工程底座是**从零自研**的，开源项目仅作为**技术选型与实现细节的参考**，没有 fork 任何现成平台作为底座。
>
> - **RuoYi-AI**：主要参考其 MCP 协议封装、模型管理、流程编排与后台权限的工程实现方式（学习「别人怎么做」）；
> - **Nexus-Agent（Super-Agent）**：吸收其企业级 Agent 架构思想——前置编排、三层执行器、双通道 RAG、证据预算与幻觉治理；
> - **自研补齐的生产底座（与其最大的差异点）**：独立 AI 模型网关、OpenTelemetry 全链路可观测、Prometheus + Grafana 告警 Harness、多实例高可用、异步任务队列、熔断舱壁隔离。
>
> **一句话总结**：这是一个以「插件化内核 + 生产级稳定性 / 可观测底座」为设计目标的通用 LLM 应用平台，RAG 等能力以可插拔插件形式接入；参考了业界优秀项目的设计与实现，但平台主体为独立自研。

---

当前进度：**第 0 周已完成，第 1 周进行中** —— 多模块骨架已就绪并通过编译与单元测试；参考仓库已就位（4 个 P0 克隆 + 5 个 Spring AI Alibaba 生态补充，共 9 个，均只读）；观测栈编排文件已生成；已推送到 `github.com/WilSenwish/llm-app-platform`。详细状态见 [docs/ROADMAP.md](docs/ROADMAP.md)。
