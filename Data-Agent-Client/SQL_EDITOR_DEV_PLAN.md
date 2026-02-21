# 中间 SQL 编辑器部分 — 开发步骤说明

参考项目：**D:\Chat2DB**（Monaco + SQL 解析与执行）  
当前项目：**Data-Agent**（已有 MonacoEditor、运行按钮和结果区壳子，缺执行 API 与结果绑定）

---

## 一、现状小结

| 模块 | 现状 |
|------|------|
| **布局** | `WorkspaceLayout` 左：DatabaseExplorer；中：`Home`（ResultsPanel + MonacoEditor + Toolbar）；右：AIAssistant |
| **编辑器** | `MonacoEditor.tsx` 已用 `@monaco-editor/react`，`language='sql'`，无执行/格式化/补全 |
| **运行** | `Home.handleRunQuery` 仅 `console.log` + 显示结果区，未调后端 |
| **结果区** | `ResultsPanel` 有「结果 / 输出」Tab 和状态栏，结果区为占位文案，未绑定真实数据 |
| **后端** | `SqlExecutionService.executeSql(ExecuteSqlRequest)` 已实现，仅被 AI 的 `ExecuteSqlTool` 使用，**无 REST 接口** |
| **执行上下文** | 无全局「当前连接 / 库 / Schema」；AI 侧有 `chatContext`（connectionId 等） |

---

## 二、开发步骤（按顺序执行）

### 阶段 1：后端 — 暴露 SQL 执行 REST 接口

1. **新增 Controller**
   - 路径建议：`data-agent-server-app/.../controller/db/SqlExecutionController.java`（或放在现有 db 包下）
   - 提供：`POST /db/sql/execute`（或 `/api/db/sql/execute`，与现有 `/api` 前缀一致）
   - 请求体：`ExecuteSqlRequest` 所需字段（`connectionId`、`databaseName`、`schemaName`、`sql`）；`userId` 从当前登录上下文（如 Sa-Token）获取，不从前端传
   - 调用：注入 `SqlExecutionService`，将请求体转为 `ExecuteSqlRequest` 并调用 `executeSql`，返回 `ExecuteSqlResponse`
   - 统一响应格式：与现有项目一致（如 `ApiResponse<ExecuteSqlResponse>`），便于前端 `http` 拦截器解包 `data`

2. **（可选）请求体 DTO**
   - 若不想直接使用 `ExecuteSqlRequest`（含 BaseRequest 的 conversationId 等），可定义 `ExecuteSqlApiRequest`，仅含 `connectionId`、`databaseName`、`schemaName`、`sql`，在 Controller 内转换为 `ExecuteSqlRequest` 并注入 `userId`

3. **验证**
   - 用 Postman/curl 调用 `POST /api/db/sql/execute`，确认返回结构与 `ExecuteSqlResponse` 一致（success、errorMessage、executionTimeMs、query、headers、rows、affectedRows）

---

### 阶段 2：前端 — 执行上下文与 API 调用

4. **执行上下文（当前连接/库/Schema）**
   - **方案 A**：在 `workspaceStore` 中增加 `sqlContext: { connectionId: number | null; databaseName: string | null; schemaName: string | null }` 及 `setSqlContext`。左侧 Explorer 在「打开控制台」或「在新 Tab 写 SQL」时设置该上下文；或从 AI 的 `chatContext` 同步（若产品要求与 AI 同源）。
   - **方案 B**：在 `Home` 或 Toolbar 上增加下拉/选择器，选择「当前连接」「当前库」「当前 Schema」，状态可放在 `workspaceStore` 或本地 state，执行时带入请求。
   - 建议先实现方案 A，并在 Explorer 双击「表」或「数据库」打开 Console 时写入 `sqlContext`，这样中间编辑器执行时就有默认上下文。

5. **前端 API**
   - 在 `src/api` 或 `src/services` 下新增 `sqlExecution.service.ts`（或 `sql.api.ts`）：
     - 方法：`executeSql(params: { connectionId: number; databaseName?: string; schemaName?: string; sql: string })`
     - 使用现有 `http`（`lib/http.ts`）发 `POST /db/sql/execute`，请求体为上述 params
     - 返回类型：与后端 `ExecuteSqlResponse` 一致（success、errorMessage、executionTimeMs、query、headers、rows、affectedRows）

6. **执行链路**
   - 在 `Home.tsx` 的 `handleRunQuery` 中：
     - 从 `workspaceStore`（或当前 state）读取 `sqlContext`（connectionId、databaseName、schemaName）
     - 从当前 `activeTab` 读取 `content` 作为 `sql`（若为 `type === 'file'`）
     - 校验：若未选连接，提示「请先选择连接」并 return
     - 调用 `sqlExecution.service.executeSql(...)`，将返回的 `ExecuteSqlResponse` 存入 state（如 `lastExecuteResult`）并传给 `ResultsPanel`
   - 可选：支持「仅执行选中 SQL」：从 Monaco 获取选中文本，有选中则执行选中部分，否则执行全部（需给 `MonacoEditor` 暴露 `getSelectionContent` 或通过 ref）

---

### 阶段 3：前端 — 结果展示与结果区改造

7. **结果区绑定真实数据**
   - `ResultsPanel` 增加 props：`executeResult: ExecuteSqlResponse | null`（及可选 `isRunning?: boolean`）
   - 当 `executeResult !== null` 且 `success === true`：
     - 若 `query === true`：用 `headers` + `rows` 渲染表格（可参考 Chat2DB 的 TableBox：表头 + 分页）；并在状态栏显示「共 x 行」「耗时 x ms」
     - 若 `query === false`：显示「影响行数：affectedRows」+ 耗时
   - 当 `success === false`：在结果区或输出 Tab 显示 `errorMessage`
   - 状态栏：根据 `executeResult` 和 `isRunning` 显示「执行中 / 已就绪 / 错误」及行数、耗时

8. **多结果集（可选，与 Chat2DB 一致）**
   - 若后端后续支持多结果集（返回 `List<ExecuteSqlResponse>`），则结果区可做成多 Tab，每个 Tab 一个表格；当前可先单结果集，接口保持可扩展。

---

### 阶段 4：Monaco 增强（SQL 解析与体验）

9. **SQL 格式化**
   - 安装依赖：`sql-formatter`（与 Chat2DB 一致）
   - 在 Toolbar 增加「格式化」按钮，或在编辑器右键菜单增加「格式化 SQL」
   - 格式化逻辑：调用 `sql-formatter` 的 `format(sql, { language: 'mysql' })` 等（可按当前连接 dbType 选 language），将结果写回当前 Tab 的 `content`（通过 `updateTabContent`）

10. **执行选中 / 全部**
    - 给 `MonacoEditor` 增加 ref 或回调：`getSelectionOrAllContent(): string`
    - 在 `handleRunQuery` 中优先使用选中内容，无选中则用全部内容作为 `sql`

11. **快捷键**
    - 保持现有 Ctrl/Cmd+Enter 执行；可选增加 Ctrl/Cmd+Shift+F 格式化（与现有 `handleRunQuery` 同层注册即可）

---

### 阶段 5：智能补全（IntelliSense，可选）

12. **表/字段补全**
    - 参考 Chat2DB：`monaco.languages.registerCompletionItemProvider('sql', { provideCompletionItems })`
    - 根据当前 `sqlContext`（connectionId、databaseName、schemaName）调用现有接口：表列表、字段列表（若后端有类似 `getAllFieldByTable` 的接口）
    - 在 `MonacoEditor` 的 `onMount` 或上层在 context 变化时注册/更新 provider，返回 `CompletionItem[]`（表名、列名等）
    - 关键字补全：可维护一份 SQL 关键字数组，在 provider 中一并返回（参考 Chat2DB 的 `constants/IntelliSense`）

13. **依赖**
    - 仅用 Monaco 内置 `sql` 语言 + 自定义 CompletionItemProvider，无需引入 syntax-parser（除非后续要做更复杂解析与错误提示）

---

## 三、与 Chat2DB 的对照

| 功能 | Chat2DB 位置 | Data-Agent 对应 |
|------|--------------|-----------------|
| Monaco 封装 | `MonacoEditor/index.tsx` + `monacoEditorConfig.ts` | 已有 `MonacoEditor.tsx`，可补 ref（getValue/getSelection）与配置 |
| 执行入口 | `ConsoleEditor` → `onExecuteSQL` → `SearchResult.handleExecuteSQL` | `Home.handleRunQuery` → 新 `sqlExecution.service` → `ResultsPanel` |
| 执行 API | `sqlServer.executeSql` → `POST /api/rdb/dml/execute` | 新 `POST /api/db/sql/execute` → `SqlExecutionService.executeSql` |
| 结果展示 | `SearchResult` + `TableBox`（多 Tab） | `ResultsPanel` + 新「结果表格」组件 + 状态栏 |
| 格式化 | `utils/sql.ts` + `sql-formatter` + 后端兜底 | 新增 `utils/sql.ts` 或 Toolbar 内 formatSql + `sql-formatter` |
| 补全 | `utils/IntelliSense` + registerCompletionItemProvider | 可选：同上，基于 sqlContext 调表/字段接口 |

---

## 四、建议实现顺序（清单）

1. [ ] 后端：新增 `SqlExecutionController`，`POST /api/db/sql/execute`，调 `SqlExecutionService.executeSql`
2. [ ] 前端：`workspaceStore` 增加 `sqlContext` + `setSqlContext`；Explorer 打开 Console 时设置 sqlContext
3. [ ] 前端：新增 `sqlExecution.service.ts`，调用 `/api/db/sql/execute`
4. [ ] 前端：`Home.handleRunQuery` 中读取 sqlContext + 当前 SQL，调用执行 API，将结果传入 ResultsPanel
5. [ ] 前端：ResultsPanel 接收 `executeResult`，渲染表格/影响行数/错误信息及状态栏
6. [ ] 前端：MonacoEditor 暴露 `getSelectionOrAllContent`，执行选中或全部
7. [ ] 前端：安装 `sql-formatter`，Toolbar 或右键「格式化 SQL」
8. [ ] （可选）智能补全：registerCompletionItemProvider + 表/字段接口

按上述顺序可实现「中间 SQL 编辑器 + 解析（格式化）+ 执行」的闭环，并与左侧数据库、右侧 AI 形成一致的工作区体验。
