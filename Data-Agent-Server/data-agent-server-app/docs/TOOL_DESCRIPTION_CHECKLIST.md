# 工具描述检查清单

与《系统提示词与工具设计方法论》中的工具描述范式对齐。每个工具在 @Tool 描述及本清单中应满足以下项。

## 检查项

- **能力句**：首句一句话说清「这个工具做什么」。
- **When to Use**：何时应调用（1～2 句）。
- **When NOT to Use**：何时不要用 / 禁用场景（1～2 句），减少误用。
- **与其它工具关系**：优先顺序或替代关系（如「先 X 再本工具」「不要用本工具替代 Y」）。
- **参数约定**：必填/可选、路径/范围（如 connectionId → databaseName → schemaName、objectNamePattern 的 `%`）、默认与上限。
- **示例**（可选）：易误用工具可带 1 个 Good/BAD 示例或简短 reasoning。

## 与 prompt 的交叉对照

- `<tool-mastery>` / `<tool-usage>` 中的决策路径、使用顺序、禁忌应与各工具的 When/When NOT 及关系一致。
- 参数层级约定（connectionId → databaseName → schemaName、objectNamePattern）在 prompt 的 `<tool-usage>` 或 `<tool-mastery>` 中统一写一次，工具描述中可引用。

## 工具逐项对照表

| 工具名 | 能力句 | When to Use | When NOT to Use | 与它工具关系 | 参数约定 |
|--------|--------|-------------|-----------------|--------------|----------|
| getEnvironmentOverview | ✓ | 环境未知、新请求开始时 | 本轮对话已获取过且无变更 | 发现入口；先于 searchObjects | 无参数 |
| searchObjects | ✓ | 按名称/模式找表或对象 | 已有明确目标表时直接用 getObjectDetail | 在 getEnvironmentOverview 之后缩小范围；先于 getObjectDetail | objectNamePattern 用 %；层级 connectionId→databaseName→schemaName |
| getObjectDetail | ✓ | 写 SQL 前获取 DDL/行数/索引 | 仅需对象名时用 searchObjects 即可 | 在 searchObjects 或确认目标后；多对象一次传入 | objects 列表；每项含 connectionId,databaseName,schemaName,objectName,objectType |
| executeSelectSql | ✓ | 执行只读 SQL | 写操作用 askUserConfirm→executeNonSelectSql | 先 getObjectDetail 再写 SQL；多语句一次传 | connectionId,databaseName,schemaName,sqls |
| executeNonSelectSql | ✓ | 执行写 SQL（已确认） | 未先调用 askUserConfirm 不得调用 | 必须且仅在接受 askUserConfirm 之后 | 同上 + 需用户确认 |
| askUserQuestion | ✓ | 多候选需用户选择时 | 目标已明确或仅一个候选 | 在 searchObjects 发现多候选后 | questions 列表，每项 2～3 选项 |
| askUserConfirm | ✓ | 任何写操作执行前 | 只读操作不需要 | 写操作前必须调用；之后才能 executeNonSelectSql | sql, connectionId, databaseName, schemaName, explanation |
| thinking | ✓ | 请求开始、写前、结果异常时 | 简单单步且目标已明确可跳过 | 复杂任务先 thinking 再发现/执行 | goal, analysis, isWrite, candidates |
| todoWrite | ✓ | 多步任务（3+ 步） | 单步或两步任务不必 | 与 workflow 进度同步 | action, todoId, items |
| enterPlanMode | ✓ | 复杂/写/多表需规划时 | 简单单表查询不必 | 规划阶段用；之后 exitPlanMode 交付 | reason, triggerSignal |
| exitPlanMode | ✓ | 规划完成交付计划时 | 仅在 Plan 模式下可用 | 与 enterPlanMode 配对 | title, steps |
| renderChart | ✓ | 数据就绪需可视化时 | 无数据或仅需表格时 | 在 executeSelectSql 有结果之后；图表即终局 | chartType, optionJson, description |
| searchMemories | ✓ | 需用户偏好/业务规则时 | 无相关记忆可跳过 | 可选，在 thinking 或执行前 | queryText, limit |
| listCandidateMemories | ✓ | 避免重复提交记忆候选时 | 无候选时不必 | 与 createCandidateMemory 配合 | conversationId, limit |
| createCandidateMemory | ✓ | 发现可复用知识时 | 与已有候选重复则不提交 | 先 listCandidateMemories 查重 | conversationId, candidateType, candidateContent, reason |
| deleteCandidateMemory | ✓ | 删除错误/冗余候选时 | 无删除需求不调用 | 维护 list 质量 | candidateId |
| activateSkill | ✓ | 某能力首次使用前加载规则 | 已加载过可跳过 | 如 chart 制图前 | skillName（如 chart） |

更新 @Tool 描述或 prompt 工具块时，请同步更新本表。
