# llm-app-platform · 插件化 LLM 应用平台

> 自研通用 LLM 应用运行平台。**RAG 仅作为可插拔插件**，平台统一承载：通用对话、RAG 问答、MCP 工具 Agent、CodeAgent、结构化抽取、多 Agent 编排、离线批量任务。

## 核心差异化亮点（简历 / 面试重点）

- **独立 AI 模型网关**：多模型路由、健康探测、故障自动降级、令牌桶限流、SSE 流式透传
- **全链路 OpenTelemetry 可观测 Harness**：AOP 埋点 + OTEL Collector + Prometheus + Loki + Grafana + AlertManager
- **限流 / 熔断 / 高可用多实例**：Resilience4j 熔断舱壁隔离（按插件 / 任务类型故障隔离）
- **压测 & 故障注入演练**：Gatling 混合流量压测 + Chaos 故障注入手册

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
git clone --depth 1 https://gitee.com/gx729/damai-ai.git              refs/damai-ai
git clone --depth 1 https://gitee.com/gx729/springai-observability-demo.git refs/springai-observability-demo
git clone --depth 1 https://gitee.com/ageerle/ruoyi-ai.git            refs/ruoyi-ai
git clone --depth 1 https://gitee.com/gx729/nexus-agent.git           refs/nexus-agent

# 2. 编译并运行单元测试（JDK 17）
export JAVA_HOME=<你的 zulu-17 JAVA_HOME>
mvn -B clean test

# 3. 启动观测栈（week4 接入；需 OrbStack）
cd compose && docker compose -f observability-stack.yml up -d
```

## 参考仓库（仅只读，不提交主仓库）

**P0（已规划拉取）**
- `refs/ragent` — nageoffer/ragent
- `refs/damai-ai` — gx729/damai-ai（网关逻辑参考）
- `refs/springai-observability-demo` — gx729（OTEL 埋点样板）
- `refs/ruoyi-ai` — ageerle/ruoyi-ai（MCP、模型管理、流程编排）
- `refs/nexus-agent` — gx729/nexus-agent（Super-Agent：前置编排、三层执行器、双通道 RAG、证据治理）

**P1 / P2（按需）**
- `refs/langchain4j-examples`（P1）
- `refs/springai-samples`（P2）

## 8 周落地计划概览

| 周次 | 重点 |
| --- | --- |
| 第0周 | 目录初始化、拉取 P0 参考仓库、工程骨架、Cursor 上下文 ✅ |
| 第1周 | 平台内核 + SPI 插件体系 + plugin-rag |
| 第2周 | 独立 AI 模型网关微服务（gateway-service） |
| 第3周 | 高可用底座改造 + 异步任务队列（Redis / RabbitMQ / Resilience4j） |
| 第4周 | 全链路可观测 Harness（OTEL + Prometheus + Loki + Grafana + AlertManager） |
| 第5周 | plugin-code-agent + plugin-extractor |
| 第6周 | Workflow 编排引擎 + plugin-multi-agent |
| 第7周 | 统一评估平台 + 压测 Harness + 故障注入演练 |
| 第8周 | 安全加固、文档、面试题库、简历项目描述、Open-WebUI 面试官 Agent |

## 面试关键区分话术

> Q：项目是不是基于 RuoYi-AI / Super-Agent 改的？
> A：没有 fork 任何现成平台作为底座，是从零自研的 `llm-app-platform`。
> - 参考 RuoYi-AI 学习 MCP 协议封装、模型管理、流程编排、后台权限工程实现；
> - 参考 Nexus-Agent（Super-Agent）吸收企业级 Agent 架构：前置编排、三层执行器、双通道 RAG、证据预算与幻觉治理思想；
> - 在参考资源之外，独立自研补齐了整套生产底座：独立 AI 模型网关、OpenTelemetry 全链路可观测、Prometheus + Grafana 告警 Harness、多实例高可用、异步任务队列、熔断舱壁隔离。

---

当前进度：**第0周（开工前置）** —— 多模块骨架已就绪并通过编译与单元测试；P0 参考仓库拉取与观测栈编排文件生成后完成初始化提交。
