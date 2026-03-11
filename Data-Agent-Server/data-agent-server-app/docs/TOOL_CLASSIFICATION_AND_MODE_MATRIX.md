# 工具分类与模式 × 工具矩阵

本文档是 data-agent-server-app 工具体系的单一事实来源：工具分类、工具名与模式过滤矩阵，与 `AgentToolConfig` 及系统提示词中的 `<tool-mastery>` 保持一致。

## 1. 工具分类表

| 分类 | 工具名（方法名） | 说明 |
|------|------------------|------|
| **发现** | getEnvironmentOverview | 一次获取所有连接、数据库(catalog)、schema 的全景；环境未知时首先调用。 |
| **发现** | searchObjects | 按模式全局搜索表/视图/函数/存储过程/触发器；需带 objectNamePattern（如 `%order%`）。 |
| **发现** | getObjectDetail | 批量获取一个或多个对象的 DDL、行数、索引；定位后探查结构时使用。 |
| **执行** | executeSelectSql | 执行只读 SQL（SELECT），可传多条语句一次调用。 |
| **执行** | executeNonSelectSql | 执行写操作（INSERT/UPDATE/DELETE/DDL）；必须先通过 askUserConfirm 获得确认。 |
| **用户交互** | askUserQuestion | 向用户提问并给出选项（如多候选时让用户选择目标）。 |
| **用户交互** | askUserConfirm | 写操作前的强制确认；返回 confirmationToken 供 executeNonSelectSql 使用。 |
| **推理与任务** | thinking | 理解目标、拆解任务、产出检查清单与下一步建议；复杂或不确定时优先调用。 |
| **推理与任务** | todoWrite | 多步任务（3+ 步）时追踪进度；CREATE/UPDATE/DELETE 任务列表。 |
| **计划** | enterPlanMode | 进入 Plan 模式，只分析规划不执行；复杂/写操作/多表时建议进入。 |
| **计划** | exitPlanMode | 退出 Plan 模式并交付结构化计划（title + steps）。仅 Plan 模式下可用。 |
| **记忆** | searchMemories | 按自然语言查询用户已确认的记忆（偏好、业务规则、术语等）。 |
| **记忆** | listCandidateMemories | 列出当前会话中待用户确认的记忆候选。 |
| **记忆** | createCandidateMemory | 提交一条待用户确认的记忆候选。 |
| **记忆** | deleteCandidateMemory | 删除一条记忆候选。 |
| **呈现** | renderChart | 根据数据渲染图表（LINE/BAR/PIE/SCATTER/AREA）；图表即终局，无需再复述数据。 |
| **技能** | activateSkill | 按任务类型加载专家规则（如 chart）；首次使用某能力前调用。 |

## 2. 模式 × 工具过滤矩阵

Agent 运行模式由 `AgentModeEnum` 定义：**AGENT**（默认，可执行 SQL 与写操作）、**PLAN**（仅分析与规划，不执行）。

过滤逻辑见 `AgentToolConfig.filterTools(agentTools, mode)`：按**工具类**禁用，而非按单个方法名。

### 2.1 AGENT 模式

- **可用**：除 exitPlanMode 外的所有工具。
- **禁用**：exitPlanMode（在 Agent 模式下无需“退出计划”，因未进入 Plan 模式）。

### 2.2 PLAN 模式

- **可用**：getEnvironmentOverview, searchObjects, getObjectDetail, askUserQuestion, thinking, todoWrite, enterPlanMode, **exitPlanMode**, searchMemories, listCandidateMemories, createCandidateMemory, deleteCandidateMemory, activateSkill。
- **禁用**（整类移除）：
  - ExecuteSqlTool：executeSelectSql, executeNonSelectSql
  - ChartTool：renderChart
  - AskUserConfirmTool：askUserConfirm

### 2.3 矩阵速查

| 工具名 | AGENT | PLAN |
|--------|-------|------|
| getEnvironmentOverview | ✓ | ✓ |
| searchObjects | ✓ | ✓ |
| getObjectDetail | ✓ | ✓ |
| executeSelectSql | ✓ | ✗ |
| executeNonSelectSql | ✓ | ✗ |
| askUserQuestion | ✓ | ✓ |
| askUserConfirm | ✓ | ✗ |
| thinking | ✓ | ✓ |
| todoWrite | ✓ | ✓ |
| enterPlanMode | ✓ | ✓ |
| exitPlanMode | ✗ | ✓ |
| renderChart | ✓ | ✗ |
| searchMemories | ✓ | ✓ |
| listCandidateMemories | ✓ | ✓ |
| createCandidateMemory | ✓ | ✓ |
| deleteCandidateMemory | ✓ | ✓ |
| activateSkill | ✓ | ✓ |

## 3. 与代码的对应关系

- **工具名**：与 LangChain4j 暴露给模型的方法名一致，见 `ToolNameEnum`。
- **按模式过滤**：`AgentToolConfig.PLAN_MODE_DISABLED`（ExecuteSqlTool, ChartTool, AskUserConfirmTool）、`AgentToolConfig.AGENT_MODE_DISABLED`（ExitPlanModeTool）。
- **系统提示词**：`<tool-mastery>` / `<tool-usage>` 应按上述分类组织子块，并与本矩阵一致。

更新本文档时请同步更新 `AgentToolConfig` 与 prompt 中的工具相关块。
