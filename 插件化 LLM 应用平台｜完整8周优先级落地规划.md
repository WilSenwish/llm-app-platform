# 插件化 LLM 应用平台｜完整8周优先级落地规划
项目名称：`llm-app-platform`
项目定位：自研**通用LLM应用运行平台**，RAG仅作为可插拔插件。平台统一承载：通用对话、RAG问答、MCP工具Agent、CodeAgent、结构化抽取、多Agent编排、离线批量任务。
核心差异化亮点（面试重点）：独立AI模型网关、全链路OpenTelemetry可观测Harness、限流/熔断/高可用多实例、告警体系、压测&故障注入演练。
开发环境：Mac + OrbStack + IDEA + Cursor + Ollama + Open-WebUI
仓库策略：
- ✅ 新建**独立主仓库 llm-app-platform**（你的工程，提交到Gitee，全部自研代码）
- ✅ 所有外部参考仓库放在本地 `./refs/`，加入`.gitignore`，**仅只读参考，不提交主仓库，不fork直接作为项目底座**
- ✅ Cursor角色：阅读refs源码、生成SPI/埋点/容器配置、重构代码、写单元测试、生成文档、定位异常

## 📦 必须下载，只读参考仓库清单（P0，全部Gitee国内直连，clone到 ./refs/）
> 全部仓库clone到本地项目目录 `llm-app-platform/refs/`，仅做阅读参考，**禁止提交到你的主仓库**
1. nageoffer/ragent
    `git clone [https://gitee.com/nageoffer/ragent.git](https://gitee.com/nageoffer/ragent.git) ./refs/ragent`
2. javaup/大麦AI
    `git clone [https://gitee.com/gx729/damai-ai.git](https://gitee.com/gx729/damai-ai.git) ./refs/damai-ai`
3. springai-observability-demo
    `git clone [https://gitee.com/gx729/springai-observability-demo.git](https://gitee.com/gx729/springai-observability-demo.git) ./refs/springai-observability-demo`
4. RuoYi-AI
    `git clone [https://gitee.com/ageerle/ruoyi-ai.git](https://gitee.com/ageerle/ruoyi-ai.git) ./refs/ruoyi-ai`
5. Nexus-Agent(Super-Agent)
    `git clone [https://gitee.com/gx729/nexus-agent.git](https://gitee.com/gx729/nexus-agent.git) ./refs/nexus-agent`

### P1 推荐参考仓库（P0完成后按需拉取）
langchain4j-examples
`git clone [https://gitee.com/gx729/langchain4j-examples.git](https://gitee.com/gx729/langchain4j-examples.git) ./refs/langchain4j-examples`

### P2 可选参考仓库（时间富余再拉取）
springai-samples
`git clone [https://gitee.com/gx729/springai-samples.git](https://gitee.com/gx729/springai-samples.git) ./refs/springai-samples`

> Ragent 在线文档：[https://nageoffer.com/ragent/](https://nageoffer.com/ragent/)
> 大麦AI 在线文档：[https://javaup.chat/damai-ai/overview/project-intro/](https://javaup.chat/damai-ai/overview/project-intro/)
> Nexus-Agent 在线文档：[https://javaup.chat/super-agent/overview/project-intro/](https://javaup.chat/super-agent/overview/project-intro/)
> 智能体认知引擎 在线文档：[https://javaup.chat/agent-up-memory/overview/project-intro/](https://javaup.chat/agent-up-memory/overview/project-intro/)
> RuoYi-AI 在线文档：[https://doc.ruoyiai.chat/](https://doc.ruoyiai.chat/)

## 优先级定义
- **P0：必须做，项目骨架核心，简历核心亮点；不完成项目无法成立**
- **P1：重要增值模块，面试强加分；P0全部完成后再开发**
- **P2：可选扩展；编码可选，面试仅口述架构，时间充足再落地**

## 前置准备｜第0周（P0，1~2天，开工前置）
**目标：目录初始化、拉取全部P0参考仓库、搭建工程骨架、配置Cursor上下文**
### 任务（P0）
1. Gitee新建空主仓库 `llm-app-platform`，多模块SpringBoot工程初始化
2. 本地目录结构创建
```
llm-app-platform/
├── refs/                # 只读参考仓库（.gitignore忽略，不提交）
├── docs/                # 架构图、tradeoff决策文档、故障手册、设计笔记
├── compose/             # OrbStack docker-compose编排文件
├── grafana/             # Grafana大盘JSON、AlertManager告警规则
├── harness/             # Gatling压测脚本、评测数据集、故障注入脚本
└── src/                 # 主项目源码（多模块）
```
3. Clone全部 **5个P0参考仓库** 放入refs目录；Cursor加载refs目录作为代码参考上下文
4. OrbStack预拉容器镜像：MySQL、Redis、Milvus、RabbitMQ、OTEL Collector、Prometheus、Loki、Grafana、AlertManager
5. Ollama本地模型准备：`qwen3:8b`、`bge-m3`
6. Open-WebUI部署，后续导入项目文档，用来搭建面试官Agent
7. Cursor全局指令配置（直接复制给Cursor）
```
你作为 llm-app-platform 项目工程助手，遵循规则：
1. refs/目录下所有仓库仅用于阅读参考，禁止直接大批量复制代码到主工程。
2. 主工程为多模块SpringBoot项目，平台内核与业务插件解耦，SPI插件化架构。
3. 新增任何功能优先增加OTEL埋点，保证全链路可观测。
4. 编写代码附带单元测试、注释；生成Mermaid架构图、markdown设计文档。
5. 遇到问题优先检索refs下参考仓库源码，提取设计思路，独立实现，禁止整段拷贝。
6. refs/ruoyi-ai：参考MCP、模型管理、流程编排；不要复制整套若依CRUD权限后台。
7. refs/nexus-agent：重点吸收前置编排、三层执行器、双通道RAG、证据治理架构思想，仅借鉴设计，禁止直接复制源码。
8. 所有组件使用国内镜像，禁止引入需要梯子访问的依赖。
9. 每次改动输出增量清单、风险点、trade-off取舍。
```
### 交付物
✅ 主仓库初始化；refs下P0参考仓库全部就绪；Cursor项目上下文就绪；Open-WebUI基础环境

---
# 8周主体计划
## 第1周｜P0：平台内核、SPI插件架构 + 前置编排/三层执行器设计
> 核心思想取自 nexus-agent（Super-Agent）；MCP抽象参考 ruoyi-ai + ragent
> Cursor任务：读取refs源码，识别平台/业务边界、抽取Runtime逻辑、定义SPI插件接口、模块拆分
### 任务 P0
1. 主工程多模块拆分
    - `platform-core`：平台内核，**前置编排层、三层执行器调度、Agent任务运行时、MCP抽象封装、会话管理、重试、异常处理器、SPI插件注册中心**（纯底座，不含业务）
    - `plugin-rag`：RAG插件（可插拔，独立模块）
2. 定义统一插件SPI规范：插件生命周期、入参/出参、埋点钩子、元信息
3. 实现插件注册中心：动态加载、卸载插件
4. 集成验证：平台内核启动，加载plugin-rag，验证文档入库、问答、MCP调用
5. 绘制整体架构Mermaid图，记录解耦依赖问题，写入`docs/tradeoff.md`
### 任务 P1
1. 编写基础单元测试（Cursor辅助）
### 交付物
✅ 多模块平台内核 + SPI插件体系 + plugin-rag插件；架构Mermaid；tradeoff决策文档

## 第2周｜P0：独立AI模型网关微服务
> 网关逻辑参考 damai-ai；模型管理能力参考 ruoyi-ai
> Cursor任务：读取damai-ai、ruoyi-ai模型管理代码，提取路由、健康探测、限流、降级逻辑；增加OTEL埋点
### 任务 P0
1. 新建独立模块 `gateway-service`
    - 多模型路由：Ollama本地模型/远端模型端点自动切换
    - 模型健康探测、故障自动降级、超时控制、重试策略
    - 令牌桶限流：全局限流 + 按用户配额，防止模型调用风暴
    - SSE流式透传
    - 统一请求结构化日志，接入OTEL埋点
2. 端到端测试：请求 → 网关 → platform-core → plugin-rag；验证模型故障降级
### 任务 P1
1. 简易API Key鉴权
### 交付物
✅ gateway-service完整模块；网关降级测试用例；网关链路追踪验证

## 第3周｜P0：高可用底座改造 + 异步任务队列
> Cursor任务：会话状态迁移Redis；Resilience4j熔断舱壁；RabbitMQ生产者消费者；并发测试代码
### 任务 P0
1. platform-core无状态改造：任务/会话状态全部存入Redis，移除JVM内存状态
2. 引入RabbitMQ异步任务队列：承载长耗时离线任务（批量文档入库、批量评估）
3. Resilience4j熔断&舱壁隔离：**按插件/任务类型故障隔离**，RAG故障不影响CodeAgent
4. Redis多级缓存：Embedding缓存、召回结果缓存，减少向量库/LLM压力
5. 本地多实例部署，网关负载均衡转发，验证水平扩容
### 任务 P1
1. 基础Gatling压测脚本（初版）
### 交付物
✅ 无状态平台内核；异步任务队列；熔断规则；多实例扩容验证报告

## 第4周｜P0：全链路可观测Harness（平台全局底座）
> 埋点样板取自 springai-observability-demo；ruoyi/nexus-agent原生缺少完整OTEL链路追踪，本模块是项目核心加分亮点
> Cursor任务：生成AOP埋点切面、编写观测栈docker-compose、导出Grafana大盘JSON
### 任务 P0
1. OrbStack一键部署观测栈：OpenTelemetry Collector + Prometheus + Loki + Grafana + AlertManager，写入`compose/`
2. 开发全局AOP埋点切面，自动拦截所有任务入口，自动生成Span、上报指标
    - 系统指标：JVM、容器、HTTP指标
    - LLM业务指标：token消耗、任务成功率、任务耗时、插件维度指标
3. 构建3套基础Grafana大盘：基础设施大盘 / LLM全局大盘 / 插件业务大盘
4. 验证：执行RAG任务，Trace、指标、结构化日志完整在Grafana展示
### 任务 P1
1. 基础告警规则（AlertManager）：服务实例宕机、P95延迟过高、全局错误率告警
### 交付物
✅ 观测栈docker-compose.yml；AOP埋点切面；Grafana大盘JSON；大盘验证截图

## 第5周｜P1：新增插件：CodeAgent插件 + 结构化抽取插件（跳出RAG）
> MCP工具扩展参考ruoyi-ai；结构化输出参考langchain4j-examples
> Cursor任务：扩展MCP文件读写工具；开发插件模块，自动接入全局埋点
### 任务 P1
1. `plugin-code-agent`插件：扩展MCP工具，本地源码读取、代码分析、代码生成、文件读取
2. `plugin-extractor`插件：文档结构化抽取，输出固定JSON Schema
3. 两个插件注册进插件中心，自动复用平台埋点，Grafana支持按插件筛选指标
4. 测试：代码项目分析、PDF合同字段抽取；验证全链路Trace
### 任务 P2
1. MCP工具安全沙箱、命令黑名单
### 交付物
✅ plugin-code-agent + plugin-extractor；测试用例；多插件指标大盘

## 第6周｜P1：Workflow编排引擎 + MultiAgent多Agent编排插件（面试拔高亮点）
> 编排底层执行参考ruoyi-ai；任务规划、子Agent委托、证据预算参考 nexus-agent
> Cursor任务：实现串行/并行/分支/人工介入节点；子Agent嵌套Span埋点
### 任务 P1
1. platform-core内置轻量Workflow编排引擎
    - 支持串行、并行、条件分支、重试、超时、Human-in-the-loop人工介入节点
2. `plugin-multi-agent`插件：规划Agent拆解任务，委派子Agent（CodeAgent/RAG Agent），合并结果
3. 埋点增强：子Agent调用生成嵌套Span，Grafana可展示完整子Agent调用树
4. 更新Grafana大盘，新增工作流/多Agent指标面板
### 任务 P2
1. 工作流定义持久化存储
### 交付物
✅ Workflow引擎；multi-agent插件；嵌套Trace链路验证

## 第7周｜P1：统一评估平台 + 压测Harness + 故障注入演练
> Badcase、幻觉治理、证据治理思路取自 nexus-agent
> Cursor任务：批量评测脚本、混合流量Gatling脚本、故障注入测试、整理故障手册
### 任务 P1
1. 平台级统一评估模块（支持全部插件任务评估，不限于RAG）
    - 批量评测任务；评估指标：任务完成率、抽取准确率、CodeAgent正确率、RAG召回指标
    - Badcase统一管理后台，统一收集幻觉、工具调用失败、各类异常案例，支持人工标注
2. 完整Gatling压测脚本：**混合流量**，并发压测RAG、CodeAgent、多Agent编排任务
3. 故障注入演练，逐条验证，写入`docs/failure-handbook.md`
    - 模型网关降级、MCP服务不可用
    - 任务队列堆积、子Agent无限循环
    - CodeAgent IO高负载、子任务部分失败，验证重试/熔断逻辑
### 任务 P2
1. 扩充AlertManager告警规则：插件维度告警（CodeAgent失败率、任务队列堆积告警）
### 交付物
✅ 批量评估脚本；Gatling混合压测脚本；压测报告；《故障排查手册.md》

## 第8周｜P0（项目收尾，简历&面试资产准备）
> Cursor任务：生成README、架构文档、项目介绍、面试题库、简历项目描述、Open-WebUI面试官Prompt
### 任务 P0
1. 安全加固：MCP工具权限隔离（不同插件拥有独立工具白名单）、Prompt注入防护
2. 项目文档撰写（Gitee仓库README，外企风格）：架构图、技术选型、trade-off取舍、部署文档、一键启动命令、大盘截图
3. 整理项目面试题库
4. 提炼简历项目段落，量化描述，突出**插件化通用LLM平台**，不是单纯RAG项目
5. Open-WebUI导入全部文档、故障手册，构建面试官Agent，反复模拟系统设计面试
### 任务 P1
1. 全链路回归测试：所有P0/P1模块功能回归验证
### 交付物
✅ 完整Gitee项目仓库；README；简历项目描述；面试题库；Open-WebUI面试官Agent

# P2 可选扩展（时间富余，可编码实现，面试可口述）
1. 多租户权限隔离插件
2. 文档异步导入、向量冷热数据分离
3. 钉钉告警通知通道

# 面试口述关键区分话术（README+面试都要用）
> Q：项目是不是基于RuoYi-AI/Super-Agent改的？
> A：
> 没有fork任何现成平台作为底座，是从零自研的`llm-app-platform`。
> - 参考 RuoYi-AI，学习MCP协议封装、模型管理、流程编排引擎、后台权限工程实现；
> - 参考 Nexus-Agent(Super-Agent) 吸收企业级Agent架构：前置编排、三层执行器、双通道RAG、证据预算与幻觉治理思想；
> - 在参考资源之外，**独立自研补齐了整套生产底座**：独立AI模型网关、OpenTelemetry全链路可观测、Prometheus+Grafana告警Harness、多实例高可用、异步任务队列、熔断舱壁隔离。

# 仓库汇总一键克隆命令（直接复制到终端执行）
```bash
# P0 核心参考仓库
git clone [https://gitee.com/nageoffer/ragent.git](https://gitee.com/nageoffer/ragent.git) ./refs/ragent
git clone [https://gitee.com/gx729/damai-ai.git](https://gitee.com/gx729/damai-ai.git) ./refs/damai-ai
git clone [https://gitee.com/gx729/springai-observability-demo.git](https://gitee.com/gx729/springai-observability-demo.git) ./refs/springai-observability-demo
git clone [https://gitee.com/ageerle/ruoyi-ai.git](https://gitee.com/ageerle/ruoyi-ai.git) ./refs/ruoyi-ai
git clone [https://gitee.com/gx729/nexus-agent.git](https://gitee.com/gx729/nexus-agent.git) ./refs/nexus-agent

# P1 参考仓库（按需执行）
git clone [https://gitee.com/gx729/langchain4j-examples.git](https://gitee.com/gx729/langchain4j-examples.git) ./refs/langchain4j-examples

# P2 参考仓库（按需执行）
git clone [https://gitee.com/gx729/springai-samples.git](https://gitee.com/gx729/springai-samples.git) ./refs/springai-samples
```
