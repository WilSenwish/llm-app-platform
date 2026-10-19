# 参考仓库学习指南（Reference Study Guide）

> 用途：把 `refs/` 下 9 个只读参考仓库，按本平台 **8 周落地计划** 的推进节奏，拆成"该看什么、按什么顺序看、看到什么程度"的可执行路径。
> 适用对象：本项目开发者（你）。所有参考仓库均为**只读、本地、不入库**（`refs/` 已被 `.gitignore` 忽略）。
> 最后更新：2026-10-19

---

## 0. 总原则（务必先读）

1. **只借鉴"设计与实现思路"，绝不 fork 业务代码**。本项目核心是**从零自研**的插件化内核，参考仓库仅用于技术选型与实现细节对标（这一点在面试话术里也要讲清，见 `README.md`）。
2. **学模式，不抄实现**。尤其 nexus-agent 的图谱（Neo4j/ES）RAG、各仓库的私有业务耦合部分，初期可直接跳过。
3. **注意技术栈版本差异**：参考仓库多用 Spring AI 1.x / Spring Boot 3.4 / Java 17+，本项目锁定 Spring Boot 3.3.5 + Java 17 + 自研 OTEL 栈。遇到 `micrometer-tracing-bridge-brave`/Zipkin、私有注册中心等与本平台冲突的部分，以本项目基线为准。
4. **License 合规**：ragent 等仓库 License 各异，仅个人学习参考，商用时需自行核实 License 兼容性。

---

## 1. 参考仓库一览与优先级

| 仓库 | 来源 | 技术栈 | 主价值 | 对应周次 | 学习优先级 |
| --- | --- | --- | --- | --- | --- |
| **nexus-agent** | gx729（Super-Agent） | Java / SB3 / Spring AI | 前置编排、三层执行器、双通道 RAG、证据治理 | Week1 内核 | 🔴 P0 最高 |
| **ragent** | nageoffer | Java / SB3 / Spring AI | 多模型路由网关、大厂工程化框架层（幂等/ID/统一返回/错误码/trace） | Week1 内核 + Week2 网关 | 🔴 P0 最高 |
| **damai-ai** | java-up-up | Java / SB3 | 网关逻辑参考 | Week2 网关 | 🟠 P1 |
| **ruoyi-ai** | ageerle | Java / SB3 | MCP 协议封装、模型管理、流程编排、后台权限 | Week1/5/6 | 🟠 P1 |
| **AssistantAgent-main** | Spring AI Alibaba | Java17 / SB3.4 / SA1.1 | Code-as-Action（代码生成+GraalVM 沙箱）、评估图、动态 Prompt、经验/Skill 体系 | Week5 / Week7 | 🟠 P1 |
| **deepresearch-main** | Spring AI Alibaba | Java17 / SB3.4 / SA1.0 | 基于 Graph 的 Multi-Agent；Plan&Execute、Hybrid RAG、Reflection、HITL、自进化 Memory、安全沙箱 | Week5 沙箱 / Week6 | 🟠 P1 |
| **Lynxe-main**（原 JManus） | Spring AI Alibaba | Java17 / SB3.x | 纯 Java 版 Manus，确定性探索任务、HTTP 服务调用 | Week6 | 🟡 P2 |
| **DataAgent-main** | Spring AI Alibaba | Java17 / SB3.4 | 企业级数据分析师：Text-to-SQL、Python 分析、智能报告、MCP Server | Week5 extractor | 🟡 P2 |
| **agentscope-examples** | AgentScope Java | Java17 / SB3 | 多 Agent 协作、工具调用框架示例集 | Week5/6 框架级 | 🟡 P2 |
| `langchain4j-examples` | gx729 | — | 结构化输出参考（**未拉取，401 私有**） | Week5 | ⚪ 待补 |
| `springai-samples` | gx729 | — | Spring AI 样例（**未拉取，401 私有**） | — | ⚪ 待补 |

> `springai-observability-demo` 已在 2026-10-18 评估后**移除**（Brave/Zipkin 与本项目 OTEL 栈不符，无复用价值）。

---

## 2. 阶段 0 · Week1 平台内核（🔴 立即深学）

目标：把现有 `platform-core` 的 `AppPlugin` / `PluginRegistry` / `PluginExecutor` 骨架，演进成真正的"执行策略注册中心 + 前置编排 + 多通道检索"。

### 2.1 nexus-agent（Super-Agent）—— 内核架构的"参考答案"

基础包：`org.javaup.ai.chatagent`，主模块 `super-agent-business/super-agent-business-chat`。

**推荐阅读顺序（由外到内）：**

| 步骤 | 看什么 | 真实路径 | 学到的模式 | 映射到本项目 |
| --- | --- | --- | --- | --- |
| 1 | 前置编排入口 | `super-agent-business-chat/.../chatagent/rag/service/ChatPreparationOrchestrator.java` | 请求进来后先做意图识别/上下文准备再分发 | 你的 `PluginExecutor` 前置阶段 |
| 2 | 执行策略注册中心 | `.../chatagent/rag/executor/ConversationExecutorRegistry.java` + 同包 `RagChatExecutor` / `ConversationExecutor` / `ReactAgentExecutor` / `GraphThenEvidenceExecutor` / `ClarificationExecutor` | 用注册表按场景选择不同执行策略（这正是 SPI 思想的成熟落地） | 你的 `PluginRegistry` + 各 `AppPlugin` 实现 |
| 3 | 双通道检索 | `.../chatagent/rag/retrieve/channel/RetrievalChannel.java` → `VectorRetrievalChannel.java` + `KeywordRetrievalChannel.java`（枚举 `super-agent-common-frame/.../enums/RetrievalChannelEnum.java`） | 向量 + 关键词多通道召回后融合 | plugin-rag 的 `Retriever` 抽象 |
| 4 | 知识路由 / 意图决策 | `.../chatagent/rag/service/DocumentQuestionRouter.java`、`GraphOnlyIntentDecision.java`、`.../manage/model/route/KnowledgeRouteDecision.java` | 按问题类型决定走图谱/向量/直答 | 你的 `PluginRouter`（Week6 编排前置） |
| 5 | 执行事件与追踪 | `.../chatagent/rag/support/ExecutorEventSupport.java`、`.../model/trace`（若有） | 执行过程事件化、可观测 | 你的 Micrometer Observation 埋点 |

**提示**：跳过 `manage/`（MySQL/ES/Neo4j/Vue 后台）、graph/Neo4j 知识图谱 specifics，只吸收"编排 → 注册 → 通道 → 路由 → 追踪"的模式骨架。

### 2.2 ragent —— 网关路由 + 大厂工程化规约

基础包：`com.nageoffer.ai.ragent`。

**两个重点方向：**

- **多模型路由网关（Week2 直接复用思路）**
  - `infra-ai/src/main/java/com/nageoffer/ai/ragent/infra/chat/RoutingLLMService.java` —— 多 `ChatClient`（Ollama / DeepSeek / SiliconFlow / BaiLian）的统一路由
  - `infra-ai/.../infra/embedding/RoutingEmbeddingService.java` —— 多 Embedding 源路由
  - 这些正是"独立 AI 模型网关"的多模型路由实现样板，Week2 重点读。
- **大厂工程化框架层（对标一线规约）**
  - `framework/src/main/java/com/nageoffer/ai/ragent/framework/` —— 幂等、分布式 ID、统一返回、错误码、trace、cache、mq 等。对照你"对标一线大厂"的要求，是最干净的参考，可在 Week3 高可用/工程化时细看。
- **RAG 链路（plugin-rag 落地时看）**
  - `infra-ai/.../infra/embedding/`、`infra/rerank/`、`rag/`（向量 + 重排链路）。

---

## 3. 阶段 1 · Week5 CodeAgent / extractor

目标：plugin-code-agent（代码生成+安全执行）、plugin-extractor（结构化抽取 / 数据类）。

### 3.1 AssistantAgent-main（Spring AI Alibaba）—— Code-as-Action 标杆
- 模块：`assistant-agent-core`（核心）、`assistant-agent-evaluation`（评估图）、`assistant-agent-prompt-builder`（动态 Prompt）、`assistant-agent-management`（经验/Skill 体系）。
- 重点学：
  - **Code-as-Action**：Agent 生成代码并在 **GraalVM polyglot 沙箱** 中执行（资源隔离）—— 对应你 plugin-code-agent 的"代码生成 + 安全沙箱"实现。
  - **Evaluation Graph（多维评估图）**：多层意图识别，指导 Agent 行为 —— Week7 统一评估平台的思路来源。
  - **统一经验/Skill 体系**：COMMON / REACT / TOOL 经验统一管理，可转 Skill —— 经验复用机制参考。
- 入口：从 `assistant-agent-start` 模块看启动装配，再到 `assistant-agent-core`。

### 3.2 deepresearch-main —— 安全沙箱 + Hybrid RAG
- 单模块项目（`src/main/java/...`），结构清晰：
  - `agents/`（Multi-Agent 初始化、MCP 分配、可观测初始化）
  - `config/`（Graph 构建）
  - `dispatcher/`（Graph EdgeAction）
- 重点学：
  - **Secure Sandbox（Docker 沙箱执行 Python 代码）** —— plugin-code-agent 沙箱实现的另一种方案（与 AssistantAgent 的 GraalVM 沙箱互补对比）。
  - **Hybrid RAG（向量 + 关键词）** —— 与 nexus-agent 双通道检索思路一致，可交叉印证。
  - **Reflection / Self-evolution Memory** —— Week7 评估与记忆机制参考。

### 3.3 DataAgent-main（Spring AI Alibaba）—— 数据类插件
- 模块：`data-agent-management`（主源码 `com` 包），含 `docker-file/`、`docs/`。
- 重点学：**Text-to-SQL**、Python 深度分析、智能报告、MCP Server —— 对应 Week5 的 plugin-extractor / 数据类插件，可直接借鉴 SQL 生成与结果渲染链路。

---

## 4. 阶段 2 · Week6 多 Agent 编排 / Workflow

目标：Workflow 编排引擎 + plugin-multi-agent。

### 4.1 Lynxe-main（原 JManus，Spring AI Alibaba）
- 单模块（`src/main/java/...`），含 `design.md` / `design1.md`（架构设计文档，优先读！）、`deploy/`、`tools/`、`knowledge/`。
- 重点学：**纯 Java 版 Manus**，处理"需要确定性的探索型任务"（从海量数据找数 → 入库、日志分析 → 告警）。对照你 Week6 的 plugin-multi-agent，学其任务拆解与工具调用编排；`design.md` 是理解其架构的最佳入口。

### 4.2 deepresearch-main（再读 Graph 部分）
- 重点学：`config/` 下的 **Spring AI Alibaba Graph** 构建、Plan&Execute 动态规划、Multi-Agent 协作（Researcher / Coder 角色）、MCP 分配 —— 这是你 Workflow 编排引擎最贴近的 Graph 实现样板。

### 4.3 agentscope-examples（AgentScope Java）
- 模块：`agents/agentscope-paw`、`agentscope-builder`、`agentscope-codingagent`、`agentscope-dataagent`，加 `documentation/`。
- 重点学：多 Agent 协作、工具调用的**最小可运行 Demo**——适合在动手写 plugin-multi-agent 前，用这些 example 快速建立框架心智模型（`codingagent` / `dataagent` 子模块尤其相关）。

---

## 5. 阶段 3 · Week7 统一评估平台

目标：统一评估 + 压测 + 故障注入。

- **AssistantAgent-main / `assistant-agent-evaluation`**：Evaluation Graph、多层意图识别、经验评分 —— 评估指标体系与自动化评估流程参考。
- **deepresearch-main**：Reflection（自我反思优化输出）、Self-evolution Memory（基于交互反馈的自我进化记忆）—— 评估闭环与持续改进机制参考。
- 压测 / 故障注入：本项目自研 `harness/gatling/`、`harness/chaos/`（已建目录骨架），无现成参考，按 `ROADMAP.md` Week7 计划自研。

---

## 6. 参考类 → 本项目组件 映射速查

| 参考仓库里的概念 | 本项目对应组件 | 备注 |
| --- | --- | --- |
| nexus `ConversationExecutorRegistry` | `platform-core` 的 `PluginRegistry` | 执行策略注册中心 |
| nexus 各 `*Executor` | 各 `AppPlugin` 实现（plugin-rag 等） | SPI 插件即"执行策略" |
| nexus `ChatPreparationOrchestrator` | `PluginExecutor` 前置阶段 / 未来 `PluginRouter` | 前置编排 |
| nexus `RetrievalChannel` / `VectorRetrievalChannel` / `KeywordRetrievalChannel` | plugin-rag 的 `Retriever` 抽象 | 双通道检索 |
| nexus `KnowledgeRouteDecision` / `DocumentQuestionRouter` | 未来编排引擎的路由决策 | Week6 |
| ragent `RoutingLLMService` / `RoutingEmbeddingService` | `gateway-service` 多模型路由 | Week2 |
| ragent `framework/*` | 工程化规约（统一返回/错误码/幂等/ID/trace） | Week3 对标 |
| AssistantAgent `Code-as-Action` + 沙箱 | plugin-code-agent | Week5 |
| AssistantAgent `Evaluation Graph` | 统一评估平台 | Week7 |
| deepresearch Graph / Plan&Execute | Workflow 编排引擎 | Week6 |
| Lynxe（Manus 式） | plugin-multi-agent | Week6 |

---

## 7. 避坑与边界

- ❌ **不要**把 nexus-agent 的 Neo4j/ES 图谱 RAG、各仓库的私有后台（Vue/MySQL 权限）搬进本项目。
- ❌ **不要**引入 `micrometer-tracing-bridge-brave`/Zipkin——本项目锁定 OTEL Collector 栈（见 `compose/`）。
- ⚠️ 参考仓库用了 Spring AI 1.0-M2 / 1.1.0 等里程碑/新版本，部分 API 与本项目 Spring AI 版本可能有差异，以本项目可编译为准。
- ⚠️ `refs/` 是解压/克隆的本地副本，**不进主仓库**，重装环境后可按 `README.md`「快速开始」重新获取（其中 5 个补充仓库需重新下载解压）。

---

## 8. 一句话结论

> **先啃 nexus-agent + ragent 把 Week1 内核打牢；Week2 网关回头看 ragent 的 Routing 与 damai-ai；Week5/6/7 再分别调取 AssistantAgent / deepresearch / Lynxe / DataAgent / agentscope-examples 做对应插件与编排的样板对标。所有仓库只学设计、不抄代码。**
