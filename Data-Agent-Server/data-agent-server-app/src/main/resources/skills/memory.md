# Memory Rules

## Purpose
Use this skill to manage durable memory that improves future turns. Good memory is stable across turns, useful for later prompt injection or explicit recall, and short enough to be understood at a glance.

## Write Only When The Information Is Durable
You may write memory when the conversation reveals one of these:
- Stable user preferences about response style, output structure, or interaction style
- Durable workflow constraints, review rules, implementation constraints, or approval rules
- Business rules or product/domain rules that will likely matter again in this workspace
- Validated knowledge that has been confirmed through repeated correction, code inspection, or verified SQL results
- Reusable high-value SQL patterns that are safe to recall later

## Do Not Write These
Never write memory for:
- One-off tasks or temporary instructions that only matter in this turn
- Raw emotional language or insults by themselves
- Guesses, ambiguous claims, or speculative conclusions
- Information that is likely to expire quickly
- Large chunks of transcript copied verbatim

## Read Before You Reach
Most turns should rely on prompt-injected memory already present in the runtime prompt.
Use explicit memory recall only when:
- The current task clearly depends on durable context that is probably not already injected
- You need to check a specific memory dimension such as a workflow rule, preference, or validated workspace fact
- The prompt-injected memory looks insufficient or ambiguous

Do not call explicit memory recall mechanically on every turn.

## Classification Workflow
Choose the narrowest valid classification.

Think in two steps before calling `writeMemory`:
1. Decide whether the conclusion is durable enough to survive beyond this turn.
2. Decide the narrowest scope and workspace level that still preserves the truth of the memory.

## Scope

### USER
Use for stable cross-conversation user preferences or durable user working habits.

Good:
- "User prefers concise bullet responses and wants final SQL wrapped in a code block."

Bad:
- "User is annoyed in this turn, so remember the frustration."

### CONVERSATION
Use for short-lived but reusable context that should help later in this conversation only.

Good:
- "For this conversation, stay read-only and do not propose production writes."

Bad:
- "User said wait a second before the next message, so persist that as a durable rule."

### WORKSPACE
Use for durable project, schema, catalog, naming, SQL, domain, or governance facts tied to the current data environment.

Good:
- "In analytics, use catalogName terminology and avoid databaseName wording."

Bad:
- "User likes concise answers, so store that as a workspace rule."

Scope selection guidance:
- Choose `USER` only when the memory is about the person, not about the current database or task
- Choose `CONVERSATION` when the rule or conclusion is useful later in this conversation but should not leak into future conversations
- Choose `WORKSPACE` when the memory is about the current data environment, naming conventions, SQL patterns, schema facts, or project rules
- If you are unsure between two scopes, prefer the narrower one

## Workspace Level
When `scope = WORKSPACE`, you must also choose the narrowest safe `workspaceLevel`.

### GLOBAL
Applies to all database work in this product context.

Good:
- "Across this product, explain storage hierarchy with catalogName instead of databaseName."

Bad:
- "Orders in analytics.public use paid_at for revenue, so store that as GLOBAL."

### CONNECTION
Applies across the current connection.

Good:
- "For connection 12, avoid write SQL unless the user explicitly asks for execution."

Bad:
- "Only analytics catalog stores money in cents, so store that as CONNECTION."

### CATALOG
Applies across the current catalog only.

Good:
- "In analytics catalog, money fields are stored in cents and must be divided by 100.0."

Bad:
- "Only analytics.public.orders uses paid_at for revenue, so store that as CATALOG."

### SCHEMA
Applies only to the current schema.

Good:
- "In analytics.public, revenue metrics are based on paid_at rather than created_at."

Bad:
- "Every schema in analytics follows this revenue rule, but still store it as SCHEMA."

Workspace level selection guidance:
- Choose `SCHEMA` when the fact or rule is tied to specific objects, tables, metrics, or conventions inside one schema
- Choose `CATALOG` when it holds across multiple schemas in the same catalog
- Choose `CONNECTION` when it holds across the whole connection regardless of catalog
- Choose `GLOBAL` only for very broad rules that truly apply everywhere in this product context
- Never widen the level just because broader recall feels convenient
- If the evidence only supports the current schema, do not write `CATALOG` or `CONNECTION`

## Memory Types

### PREFERENCE
What it is: a stable user preference about style, format, interaction, or decision framing.

Good:
- "User prefers concise answers with SQL in fenced code blocks."

Bad:
- "The current schema stores money in cents."

### BUSINESS_RULE
What it is: a durable business definition, governance rule, product rule, or safety rule.

Good:
- "Only paid orders count toward recognized revenue."

Bad:
- "Always explain your answer before giving SQL."

### KNOWLEDGE_POINT
What it is: a validated fact about architecture, domain concepts, glossary terms, or concrete objects.

Good:
- "The active_users view already excludes soft-deleted accounts."

Bad:
- "I think active_users probably excludes deleted users, but I did not verify it."

### WORKFLOW_CONSTRAINT
What it is: a durable constraint on process, approvals, implementation style, or review behavior.

Good:
- "Before any write SQL, explain the risk and wait for explicit confirmation."

Bad:
- "GMV is revenue before refunds."

### GOLDEN_SQL_CASE
What it is: a validated and reusable SQL pattern, join strategy, SQL snippet, or metric formula.

Good:
- "Monthly revenue in analytics uses date_trunc('month', paid_at) with amount_cents / 100.0."

Bad:
- "Here is an unverified draft SQL query that might work."

## SubType Quick Examples

### PREFERENCE

#### RESPONSE_STYLE
Good:
- "User prefers concise direct answers over long explanations."
Bad:
- "Orders become active only after payment."

#### OUTPUT_FORMAT
Good:
- "User wants final SQL wrapped in a fenced code block."
Bad:
- "For this one reply, add a code block."

#### LANGUAGE_PREFERENCE
Good:
- "User prefers Chinese for future interaction unless they switch languages."
Bad:
- "The user wrote one Chinese sentence in this turn, so always persist Chinese as a preference."

#### INTERACTION_STYLE
Good:
- "User prefers that you explain assumptions before taking action."
Bad:
- "In analytics catalog, cents must be divided by 100.0."

#### DECISION_STYLE
Good:
- "When offering multiple options, user wants a recommended option first."
Bad:
- "The user asked for two choices in this single message."

### BUSINESS_RULE

#### PRODUCT_RULE
Good:
- "Exports in this product use UTC timestamps by default."
Bad:
- "The user likes UTC formatting."

#### DOMAIN_RULE
Good:
- "Only paid orders count toward GMV."
Bad:
- "Always verify joins before finalizing SQL."

#### GOVERNANCE_RULE
Good:
- "Sensitive tables must not be queried without explicit justification."
Bad:
- "The orders table primary key is order_id."

#### SAFETY_RULE
Good:
- "Never execute write SQL without explicit user confirmation."
Bad:
- "The current conversation is read-only for now."

### KNOWLEDGE_POINT

#### ARCHITECTURE_KNOWLEDGE
Good:
- "This warehouse flows from ods to dwd to ads."
Bad:
- "You should always review SQL before execution."

#### DOMAIN_KNOWLEDGE
Good:
- "Money fields in analytics are stored in cents."
Bad:
- "Use concise bullet formatting."

#### GLOSSARY
Good:
- "DAU means daily active users."
Bad:
- "For this one answer, define DAU before the SQL."

#### OBJECT_KNOWLEDGE
Good:
- "The orders table primary key is order_id."
Bad:
- "I assume orders probably joins to users on user_id."

### WORKFLOW_CONSTRAINT

#### PROCESS_RULE
Good:
- "First analyze the request, then produce SQL."
Bad:
- "Paid orders define revenue."

#### APPROVAL_RULE
Good:
- "Ask for confirmation before any write operation."
Bad:
- "This message asks for confirmation once, so store it as a durable rule."

#### IMPLEMENTATION_CONSTRAINT
Good:
- "Use catalogName terminology and avoid databaseName wording."
Bad:
- "order_id is the primary key of orders."

#### REVIEW_CONSTRAINT
Good:
- "Every final SQL must include a brief risk review of filters and joins."
Bad:
- "The analytics catalog stores money in cents."

### GOLDEN_SQL_CASE

#### QUERY_PATTERN
Good:
- "Monthly revenue groups by date_trunc('month', paid_at) and sums amount_cents / 100.0."
Bad:
- "Here is a one-off SQL draft for this turn only."

#### JOIN_STRATEGY
Good:
- "Join orders to users on user_id and keep a left join when preserving unmatched orders."
Bad:
- "Users prefer left joins."

#### VALIDATED_SQL
Good:
- "This validated SQL returns active paid users correctly in analytics.public."
Bad:
- "This SQL looks plausible, but it was never checked."

#### METRIC_CALCULATION
Good:
- "ARPU equals revenue divided by paid_users."
Bad:
- "The user wants the ARPU formula explained first."

## How To Write Good Memory
Write concise, reusable conclusions.
- Remove turn-specific wording such as "this time", "for the current task", or transcript-only details
- Prefer one clear sentence over a paragraph
- State the lasting rule, preference, or fact directly
- Use `title` as a short label and `content` as the authoritative wording
- Use `reason` to explain why this should be remembered, not to restate the content
- Prefer stable conclusions over procedural chatter; write what should be remembered, not how the conversation unfolded

## Update Behavior
If a new instruction corrects or replaces an older preference or rule, write the newer memory so the system can overwrite the old one. Do not preserve conflicting parallel versions on purpose.

When you revise memory:
- Keep the same conceptual rule or preference if the user is clearly correcting wording or tightening the rule
- Write the newest authoritative phrasing instead of preserving both versions
- Do not create parallel memories that disagree unless the disagreement itself is the durable fact

## Tool Usage
When the current turn establishes a durable preference, reusable workflow rule, validated workspace fact, or reusable SQL pattern, you should actively consider writing memory instead of just replying and moving on.

When durable context is missing, use `readMemory` with a focused intent and optional filters.
- Prefer a short intent that states what durable fact you need
- Add `scope`, `memoryType`, or `subType` only when that narrows the recall correctly
- Do not call explicit recall every turn

When the memory is worth storing, call `writeMemory` directly.
- Do not ask the user for confirmation before writing
- Do not announce memory creation as part of the normal user-facing answer
- Do not call `writeMemory` for weak or low-confidence candidates
- When `scope = WORKSPACE`, pass the explicit binding fields required by that level:
  - `GLOBAL`: no extra binding fields
  - `CONNECTION`: `workspaceConnectionId`
  - `CATALOG`: `workspaceConnectionId` + `workspaceCatalogName`
  - `SCHEMA`: `workspaceConnectionId` + `workspaceCatalogName` + `workspaceSchemaName`
- Do not rely on hidden runtime context to decide the workspace binding
- Do not guess broader workspace scope than the evidence supports
