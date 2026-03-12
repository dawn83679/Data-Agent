# 发现阶段优化与错误回传（本次提交改动梳理）

## 一、后端：发现服务并行化

**文件：** `Data-Agent-Server/data-agent-server-app/src/main/java/edu/zsc/ai/domain/service/db/impl/DiscoveryServiceImpl.java`

- **getEnvironmentOverview**：多连接时按 connection 并行执行 `buildConnectionOverview`，再按顺序合并，缩短墙钟时间。
- **searchObjects**：单连接保持顺序；多连接时按连接并行执行 `searchConnectionAcrossDatabases`，再合并结果并截断至 100 条。
- **可读性**：抽取 `searchConnectionAcrossDatabases` / `searchConnectionAcrossDatabasesOrThrow`，减少主流程嵌套；空/单连接提前 return。

## 二、错误回传到 Tool（模型可见）

**模型层：**

- **ConnectionOverview**（`agent/tool/sql/model/ConnectionOverview.java`）：新增可空字段 `error`。连接不可达时填入错误信息，Tool 仍返回 success，模型从 result 中看到该连接的 `error`。
- **ObjectSearchResponse**（`agent/tool/sql/model/ObjectSearchResponse.java`）：新增可空字段 `errors`（List<String>）。部分连接或 db/schema 失败时写入错误列表，与 `results` 一并返回。

**DiscoveryServiceImpl：**

- **buildConnectionOverview**：异常时返回带 `error` 的 ConnectionOverview，不再吞掉异常。
- **searchConnectionAcrossDatabases**：最外层 catch 整连接失败并返回带 `errors` 的 ObjectSearchResponse；内层按 db/schema catch，收集部分 results + errors。
- **resolveDatabases / getSchemas / collectSearchResults**：不再内部吞异常，让调用方或上层 catch 并写入 error/errors。

## 三、异步任务中恢复 RequestContext

**文件：** `DiscoveryServiceImpl.java`

- 并行任务在 `sharedExecutor` 线程池中执行，ThreadLocal 的 RequestContext 不会继承。
- 在提交任务前快照 `RequestContextInfo contextSnapshot = RequestContext.get()`，在每个 async 任务内先 `RequestContext.set(contextSnapshot)`，finally 中 `RequestContext.clear()`，避免 "No userId available in RequestContext"。

## 四、全局通用线程池

**新增：** `Data-Agent-Server/data-agent-server-app/src/main/java/edu/zsc/ai/config/ExecutorConfig.java`

- 定义全局共享线程池 Bean：`sharedExecutor`（`ExecutorConfig.SHARED_EXECUTOR_BEAN_NAME`）。
- 配置项：`app.executor.pool-size`（默认 20），线程名前缀 `app-`，Spring 管理生命周期（优雅关闭）。
- DiscoveryServiceImpl 注入并使用该 Executor，不再使用静态 `Executors.newFixedThreadPool`。

**配置：** `application.yml`

- 新增 `app.executor.pool-size: 20`。

## 五、提示词：发现结果即唯一事实

**文件：** `prompt/system_agent_zh.xml`、`prompt/system_agent_en.xml`

- 在 `<discovery>` 段增加规则：以最近一次 getEnvironmentOverview/searchObjects 的工具返回为唯一依据；在基于该结果完成目标选择或执行决策前不得再次调用发现类工具；仅当用户明确切换环境或要求重新扫描时可再调；超过 3 轮未基于最近发现结果做决策时的处理方式。

---

**涉及文件列表：**

- 修改：ConnectionOverview.java, ObjectSearchResponse.java, DiscoveryServiceImpl.java, application.yml, system_agent_zh.xml, system_agent_en.xml
- 新增：ExecutorConfig.java
