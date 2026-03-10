# Data-Agent 多 Agent Mode 设计与调研

## 1. 背景与目标

`Data-Agent` 当前已经具备两种 Agent 执行模式：

- `agent`：单 Agent 直接理解用户请求、调用工具、生成 SQL、执行并返回结果。
- `plan`：单 Agent 只做分析与规划，不直接执行。

当前后端主链路已经比较清晰：

- 请求入口：`ChatRequest`
- 会话准备：`ChatSessionFactory`
- Agent 调用：`ReActAgent`
- 流式桥接：`ChatStreamBridge`
- 输出方式：SSE `ChatResponseBlock`

这套架构已经有几个很重要的基础能力：

- Plan Mode 切换
- 工具调用流式展示
- 写操作确认机制
- 对话记忆与上下文注入

因此，我们的目标不是另起一套“外置多 Agent 平台”，而是在现有聊天链路内新增第三种模式：

- `multi-agent`：主 Agent 负责规划、拆解、派工、汇总，多个子 Agent 负责执行具体子任务并回报结果。

这个目标的核心不是“让多个 Agent 自由对话”，而是让复杂数据库任务从单体 ReAct 升级为受控协作式执行。

## 2. 当前系统现状

### 2.1 当前架构事实

基于仓库现状，当前 AI 侧实现有以下关键事实：

- `AgentModeEnum` 目前只有 `AGENT("agent")` 与 `PLAN("plan")` 两种模式。
- `ChatRequest.agentType` 已经承担模式入口语义。
- `AgentManager` 通过 `model + language + mode` 缓存和创建 `ReActAgent`。
- `AgentToolConfig` 目前按 mode 过滤工具，尚未按角色过滤工具。
- `ChatServiceImpl` 目前支持 Agent Mode 执行中自动切入 Plan Mode。
- `ChatResponseBlock` 已支持 `TEXT`、`THOUGHT`、`TOOL_CALL`、`TOOL_RESULT`、`STATUS`、`done` 这些流式块。

### 2.2 当前可复用的能力

这对多 Agent 设计非常关键，因为我们不是从零开始：

- 已有统一聊天入口，不需要新开一套 API。
- 已有会话上下文机制，可扩展 `runId`、`taskId`、`agentRole`。
- 已有流式工具展示能力，可扩展为任务级事件流。
- 已有用户确认能力，适合接入写操作审批。
- 已有 Plan Mode，可作为主 Agent 在复杂任务前的思考能力基础。

### 2.3 当前不足

现有实现仍然是“单 Agent + 工具集”模型，离稳定的多 Agent 体系还有明显缺口：

- Prompt 仍按语言切换，没有按 `mode + role` 切换。
- Tool 权限只有 mode 维度，没有角色维度。
- Context 只有 conversation 级，没有 run/task 级。
- SSE 事件类型不足以表达“派工、回报、等待审批、任务状态”。
- 主 Agent 和子 Agent 没有结构化任务协议。
- 当前 Plan Mode 仍然是单 Agent 分析，并不是可执行编排层。

结论很明确：多 Agent 不能只靠加一个 prompt 解决，必须把它正式定义成新的 `Agent Mode`。

## 3. 为什么把多 Agent 做成新的 Agent Mode

### 3.1 这不是“更多工具”，而是新的运行语义

如果只是增加几个 tool，本质上还是单 Agent 自己做所有事情。这样会有几个问题：

- 规划权、执行权、总结权混在一个上下文里。
- 高风险数据库任务的权限边界不清晰。
- 子任务之间缺少明确的输入输出协议。
- 用户看不到“是谁在做什么”。

而 `multi-agent` 代表的是新的执行模式：

- 用户请求先进入主 Agent
- 主 Agent 决定是否拆解为多个任务
- 子 Agent 各自处理受控范围内的任务
- 主 Agent 对结果做汇总、校验、追问、重派、审批控制

### 3.2 三种 Mode 的边界

建议将 `AgentModeEnum` 扩展为：

- `agent`：单 Agent 直接执行
- `plan`：单 Agent 只规划不执行
- `multi-agent`：主 Agent 编排多个子 Agent 协作执行

三者关系不是替代，而是并存：

- 简单任务仍走 `agent`
- 高风险但尚不需要协作的任务可走 `plan`
- 多步骤、跨对象、需要明确分工的复杂任务走 `multi-agent`

### 3.3 对当前工程的直接影响

如果把多 Agent 做成新的 Mode，那么它会真实影响这些位置：

- `AgentModeEnum`：新增 `MULTI_AGENT`
- `ChatRequest.agentType`：支持 `multi-agent`
- `AgentManager`：缓存 key 和 agent 构造逻辑要识别新 mode
- `AgentToolConfig`：从 `mode` 过滤升级为 `mode + role` 过滤
- `PromptConfig` / prompt 资源：从“按语言”升级为“按语言 + mode + role”
- `ChatServiceImpl`：从单次 Agent 调用升级为主从编排流程
- `ChatResponseBlock`：新增任务级事件块

## 4. 主流多 Agent 实现盘点

这一节只看主流、具有代表性的实现模式，不追求把所有框架都罗列一遍。

### 4.1 Supervisor / Manager 模式

#### 架构模式

一个主 Agent 负责用户交互、任务拆解、选择专家 Agent、汇总结果。子 Agent 通常不直接面对用户，而是作为工具或受控子流程存在。

#### 代表实现

- OpenAI Agents SDK 的 `Manager (agents as tools)` 与 `handoffs`
- LangGraph Supervisor
- CrewAI Hierarchical Process
- LangChain4j `supervisorBuilder()`

#### 技术实现特点

- 主 Agent 负责 routing 与 orchestration
- 子 Agent 暴露为 handoff target 或 sub-agent
- 常带有共享上下文、trace、guardrails、streaming
- 更容易做权限治理、成本治理和结果审计

#### 优点

- 控制强，适合数据库类高风险任务
- 权限边界容易切分
- 用户入口单一，交互体验稳定
- 容易加审批、重试、超时、限流

#### 缺点

- 主 Agent 容易成为瓶颈
- 如果上下文裁剪做不好，成本会变高
- 主 Agent 设计不清楚时会退化成“大一统 Agent”

#### 对 Data-Agent 的适配评价

这是最适合 `Data-Agent` 的主模式。数据库任务天然需要：

- 明确责任边界
- 严格权限分层
- 主入口统一审批
- 可解释的执行链路

### 4.2 Workflow / Graph 模式

#### 架构模式

把整个 agent 系统视为状态机或图。每个节点可能是一个 Agent、工具调用器、人工审批节点或路由节点。系统不强调“聊天”，而强调“状态流转”。

#### 代表实现

- LangGraph
- 部分 LangChain workflow 设计
- 一些基于状态图的企业编排实现

#### 技术实现特点

- 显式状态图
- checkpoint / resume
- branch / loop / retry
- 人工介入节点
- 状态持久化与任务恢复

#### 优点

- 状态最清楚
- 适合复杂业务流
- 可恢复、可审计、可治理
- 适合后续平台化

#### 缺点

- 建模成本高
- 首版实现偏重
- 对现有代码侵入更强

#### 对 Data-Agent 的适配评价

它非常适合作为 `Data-Agent` 的中期演进方向，但不适合作为 v1 直接重构目标。更合理的方式是：

- v1 先做 `Supervisor + Structured Delegation`
- v2 再把运行过程显式工作流化

### 4.3 Team / Group Chat / Selector 模式

#### 架构模式

多个 Agent 像群聊一样轮流说话，或者通过 selector 决定下一个发言 Agent。系统更像“多人讨论”，而不是“主从编排”。

#### 代表实现

- AutoGen `Teams`
- AutoGen `RoundRobinGroupChat`
- AutoGen `SelectorGroupChat`
- 一些 swarm / group chat 风格框架

#### 技术实现特点

- 多 Agent 共用消息流
- termination condition
- speaker selection
- team stream
- 常见于研究、写作、审稿、头脑风暴

#### 优点

- 形式灵活
- 发散能力强
- 适合研究、辩论、创意任务

#### 缺点

- 难以控制
- 消息膨胀严重
- 权限隔离困难
- 对数据库类任务不友好

#### 对 Data-Agent 的适配评价

这不是 `Data-Agent` v1 应采用的默认模式。数据库任务不适合多人自由群聊式决策，因为：

- 风险高
- 需要稳定结构化输入输出
- 需要明确审批边界
- 需要能快速知道“谁做错了”

### 4.4 Planner / Peer / Critic 循环模式

#### 架构模式

Planner 负责规划，多个 peer agents 负责生成、批判、验证、评分，再进入循环优化。

#### 代表实现

- LangChain4j `plannerBuilder()`
- LangChain4j agentic patterns 中的研究型样例
- 各类 `critic / validator / scorer` 结构

#### 技术实现特点

- plan -> act -> critique -> validate -> score -> loop
- 循环退出条件
- 多轮质量优化
- 更偏研究和 hypothesis 生成

#### 优点

- 适合复杂推理
- 适合质量优化和多轮反思
- 能增强结果可靠性

#### 缺点

- 容易无限循环
- 成本高
- 延迟高
- 需要额外治理最大调用次数

#### 对 Data-Agent 的适配评价

可以局部借鉴，但不应作为默认主架构。更适合用于：

- 复杂 SQL 方案复核
- 高风险写操作前的 validator/critic 流
- 多候选 SQL 的评分与筛选

### 4.5 A2A / 分布式 Agent 协议模式

#### 架构模式

子 Agent 不一定和主系统在同一进程内，而是通过标准协议远程通信。主 Agent 更像 client agent，子 Agent 是 remote agent。

#### 代表实现

- Google A2A 协议方向
- LangChain4j `a2aBuilder()`
- 未来组织级 Agent 平台常见的远程 Agent 通信模式

#### 技术实现特点

- capability discovery
- agent card
- task-oriented protocol
- artifact exchange
- SSE / HTTP / JSON-RPC 等标准之上实现
- 支持长任务和状态同步

#### 优点

- 可跨团队、跨服务、跨框架协作
- 适合平台化和企业级治理
- 长期扩展性最好

#### 缺点

- 对现阶段实现过重
- 协议治理与状态治理复杂
- 调试链路更长

#### 对 Data-Agent 的适配评价

这是长期方向，不是 v1 路线。当前更合理的做法是：

- 内部先把任务协议对象设计好
- 保持可序列化
- 为未来远程 Agent 留扩展位

## 5. 我们当前的妥协方案

这部分必须写清楚，因为我们当前不是在做终局架构，而是在做可落地的第一阶段方案。

### 5.1 架构妥协

- v1 只做单服务内多 Agent，不做真正分布式 Agent。
- v1 不引入完整 workflow engine。
- v1 不做 A2A 远程通信。
- v1 复用当前 `AiServices + Tool + SSE` 主链路，不立即迁移到 `langchain4j-agentic`。

### 5.2 交互妥协

- 用户入口仍然只有一个聊天窗口。
- 子 Agent 不直接与用户对话。
- 所有任务状态通过主 Agent 汇总展示。
- 前端先做任务事件流展示，不先做完整编排看板。

### 5.3 能力妥协

- v1 只聚焦数据库任务。
- 子 Agent 角色固定，不先做动态 agent registry。
- 不做开放式 peer-to-peer agent 协作。
- 不支持任意子 Agent 再委派其他子 Agent。

### 5.4 状态妥协

- v1 先不做完整持久化 run/task 编排表。
- 可先以内存态或轻量仓储维护 `run/task`。
- 先把 tracing 和结构化日志打通，再考虑持久化恢复。

## 6. 我们当前的不足与风险

### 6.1 技术不足

- 当前 prompt 资源没有主 Agent 与子 Agent 分治设计。
- 当前 tool 过滤机制过粗，无法表达“谁能规划、谁能执行、谁能只读”。
- 当前上下文模型不足以承载子任务生命周期。
- 当前 SSE 事件模型没有任务级协议。
- 当前没有统一的任务输入输出结构。

### 6.2 产品风险

- 如果多 Agent 只是多 prompt 拼接，系统会很快失控。
- 如果主 Agent 同时掌握所有高权限工具，系统会回到单 Agent 老路。
- 如果没有任务级可视化，用户不会真正信任“多 Agent 在做什么”。
- 如果没有最大调用次数和重试治理，系统容易进入无限循环。

### 6.3 工程风险

- 直接重构到 `langchain4j-agentic` 风险过高，因为现有链路已深度绑定 SSE、工具流与上下文。
- 过早做远程 A2A 会拉高状态治理和调试成本。
- 过早做平台化会分散当前数据库智能体主线能力。

## 7. v1 目标架构

### 7.1 新的 Mode 定义

新增：

- `MULTI_AGENT("multi-agent")`

保持三种模式并存：

- `agent`
- `plan`
- `multi-agent`

### 7.2 核心角色

#### 主 Agent

唯一对用户负责的 `OrchestratorAgent`，职责固定为：

- 理解任务目标
- 决定是否拆解
- 选择子 Agent
- 汇总结果
- 控制审批
- 决定重试、追问或结束

#### 子 Agent

v1 固定四类数据库子 Agent：

- `SchemaAnalystAgent`
- `SqlPlannerAgent`
- `SqlExecutorAgent`
- `ResultAnalystAgent`

### 7.3 任务协议

多 Agent 不采用自由文本协作，而采用结构化协议对象。建议最小对象集为：

- `AgentRun`
- `AgentTask`
- `DelegationRequest`
- `DelegationReport`
- `ArtifactRef`

这些对象即使 v1 先只在内存中存在，也要从一开始按 JSON 可序列化方式设计。

### 7.4 运行流程

数据库复杂任务的标准流程建议固定为：

1. 用户请求进入 `multi-agent`
2. `OrchestratorAgent` 评估复杂度与风险
3. 派 `SchemaAnalystAgent`
4. 派 `SqlPlannerAgent`
5. 若涉及写操作或高风险查询，进入审批
6. 审批通过后派 `SqlExecutorAgent`
7. 派 `ResultAnalystAgent`
8. `OrchestratorAgent` 汇总结论并返回用户

### 7.5 权限边界

- `SchemaAnalystAgent`：只读 schema / metadata / memory
- `SqlPlannerAgent`：只生成 SQL，不执行
- `SqlExecutorAgent`：只执行已批准 SQL
- `ResultAnalystAgent`：只解释结果
- `OrchestratorAgent`：只编排，不直接执行 SQL

这个边界是多 Agent 方案成立的关键。如果主 Agent 仍直接能执行 SQL，那么系统会退化回原来的单 Agent。

## 8. 后续架构方向

### Phase 1：Mode 化

目标：

- 把多 Agent 正式做成 `multi-agent` mode
- 打通主从协作链路
- SSE 可见主从任务过程
- 完成数据库复杂任务闭环

关键词：

- orchestrator
- worker agents
- structured delegation
- controlled execution

### Phase 2：工作流化

目标：

- 引入显式 `run/task` 状态机
- 支持任务恢复、重试、限流、超时、审计
- 增强人工介入与可恢复执行

关键词：

- workflow
- state machine
- checkpoint
- retry policy
- approval node

### Phase 3：平台化

目标：

- 评估部分子 Agent 独立服务化
- 接入远程 Agent 与能力发现
- 与 A2A / agent registry / org-level governance 接轨

关键词：

- remote agents
- capability discovery
- agent registry
- A2A
- cross-service tracing

## 9. 后续技术方向

### 9.1 Prompt 方向

从当前“按语言切 prompt”，演进到：

- 按语言
- 按 mode
- 按 role
- 按风险等级

至少要区分：

- 单 Agent prompt
- Plan Mode prompt
- Multi-Agent 主 Agent prompt
- Multi-Agent 各子 Agent prompt

### 9.2 Tool 权限方向

从当前“按 mode 过滤工具”，演进到：

- `mode + role + risk` 三维工具权限控制

工具治理目标：

- 谁能读
- 谁能写
- 谁能请求审批
- 谁能终止任务
- 谁能发起重试

### 9.3 上下文与状态方向

从 conversation 级上下文，演进到：

- `conversationId`
- `runId`
- `taskId`
- `agentRole`
- `parentTaskId`
- `delegationDepth`

### 9.4 流式协议方向

SSE 事件需要从当前文本/工具流，扩展到任务流：

- `TASK_PLAN`
- `TASK_START`
- `TASK_STATUS`
- `TASK_RESULT`
- `TASK_APPROVAL`
- `TASK_DONE`

### 9.5 可观测性方向

日志和 tracing 至少要具备：

- model
- conversationId
- runId
- taskId
- agentRole
- toolName
- toolCallId
- duration
- token usage
- retry count

### 9.6 前端方向

前端不应只显示“AI 说了什么”，而应逐步显示：

- 主 Agent 正在分析
- 已派发哪些子任务
- 哪个子 Agent 正在执行
- 哪个子任务需要审批
- 子任务结果与主结论的对应关系

## 10. 对官方与主流技术路线的判断

### 10.1 对 OpenAI Agents SDK 的判断

OpenAI 官方文档当前明确把多 Agent 常见模式分为两类：

- `Manager (agents as tools)`
- `Handoffs`

对 `Data-Agent` 来说，更适合借鉴 manager 模式，因为数据库任务需要主入口统一控制，而不是频繁把会话控制权交出去。

### 10.2 对 LangGraph 的判断

LangGraph 的强项不是“多几个 Agent”，而是：

- 状态图
- 层级 supervisor
- 记忆与检查点
- 长流程治理

这非常适合 `Data-Agent` 中后期演进，但不适合作为当前版本的第一刀重构。

### 10.3 对 AutoGen 的判断

AutoGen 团队式聊天很适合：

- 研究
- 写作
- 评审
- 发散式任务

但它不是数据库执行智能体的第一优先解，因为它对消息治理和权限隔离要求更高。

### 10.4 对 CrewAI 的判断

CrewAI 的 hierarchical process 对我们很有参考价值，尤其是：

- manager agent 负责 delegation
- manager agent 负责 validation
- delegation 默认不应无限开放

这与我们希望的主 Agent 角色边界高度一致。

### 10.5 对 LangChain4j 官方方向的判断

从本地官方源码观察，`langchain4j` 已经出现这些方向：

- `langchain4j-agentic`
- `supervisorBuilder()`
- `plannerBuilder()`
- `a2aBuilder()`
- `langchain4j-agentic-patterns`
- `langchain4j-agentic-a2a`

这说明官方路线也在向：

- supervisor
- planner
- workflow
- A2A

这些模式靠拢。

但对当前 `Data-Agent` 来说，更合理的策略不是立即迁移，而是：

- 先在你自己的服务层做出稳定的 `multi-agent` mode
- 接口和协议设计尽量兼容未来接入 `langchain4j-agentic`
- 在有足够收益时再渐进替换底层编排实现

## 11. 最终设计结论

### 11.1 当前建议

`Data-Agent` 应将多 Agent 正式定义为新的 `Agent Mode`，而不是临时 prompt 技巧或 tool 组合。

### 11.2 v1 推荐路线

推荐路线是：

- `Supervisor + Structured Delegation`
- 单服务内编排
- 数据库任务优先
- 权限与角色边界严格控制
- 复用现有 SSE、确认机制和上下文体系

### 11.3 不推荐路线

当前不推荐：

- 直接重构为分布式 A2A
- 直接切换到纯 workflow engine
- 默认采用群聊式多 Agent
- 让所有子 Agent 都可自由调用高权限工具

### 11.4 长期方向

长期应逐步演进为：

- 多 Agent Mode
- 显式 run/task 状态机
- 工作流化编排
- 平台级观测与治理
- 可选远程 Agent / A2A 接入

## 12. 参考资料

以下资料用于支撑本文中的主流实现判断与技术方向归纳：

- OpenAI Agents SDK Agents
  - https://openai.github.io/openai-agents-python/agents/
- OpenAI Agents SDK Handoffs
  - https://openai.github.io/openai-agents-python/handoffs/
- OpenAI API Agents SDK Guide
  - https://platform.openai.com/docs/guides/agents-sdk/
- LangGraph Reference
  - https://langchain-ai.github.io/langgraph/reference/
- LangGraph Supervisor
  - https://langchain-ai.github.io/langgraphjs/reference/modules/langgraph-supervisor.html
- LangChain Multi-agent
  - https://docs.langchain.com/oss/python/langchain/multi-agent
- AutoGen Teams
  - https://microsoft.github.io/autogen/stable/user-guide/agentchat-user-guide/tutorial/teams.html
- CrewAI Hierarchical Process
  - https://docs.crewai.com/en/learn/hierarchical-process
- Google Developers Blog: Agent2Agent Protocol
  - https://developers.googleblog.com/a2a-a-new-era-of-agent-interoperability/

