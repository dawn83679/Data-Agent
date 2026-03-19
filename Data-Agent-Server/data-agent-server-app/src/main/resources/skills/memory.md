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

## Classification Rules
Choose the narrowest valid classification.

### Scope
- `USER`: stable cross-conversation user preferences or durable user working habits
- `WORKSPACE`: durable project/workspace rules, domain facts, architecture conventions, or validated SQL patterns
- `CONVERSATION`: short-lived but reusable context for this conversation only

### Workspace Level
When `scope = WORKSPACE`, you must also choose the narrowest safe `workspaceLevel`.
- `GLOBAL`: applies to all database work in this product context
- `CONNECTION`: applies across the current connection
- `CATALOG`: applies across the current catalog only
- `SCHEMA`: applies only to the current schema

### Memory Types and Typical SubTypes
- `PREFERENCE`
  - `RESPONSE_STYLE`
  - `OUTPUT_FORMAT`
  - `INTERACTION_STYLE`
  - `DECISION_STYLE`
- `BUSINESS_RULE`
  - `PRODUCT_RULE`
  - `DOMAIN_RULE`
  - `GOVERNANCE_RULE`
  - `SAFETY_RULE`
- `KNOWLEDGE_POINT`
  - `ARCHITECTURE_KNOWLEDGE`
  - `DOMAIN_KNOWLEDGE`
  - `GLOSSARY`
  - `OBJECT_KNOWLEDGE`
- `WORKFLOW_CONSTRAINT`
  - `PROCESS_RULE`
  - `APPROVAL_RULE`
  - `IMPLEMENTATION_CONSTRAINT`
  - `REVIEW_CONSTRAINT`
- `GOLDEN_SQL_CASE`
  - `QUERY_PATTERN`
  - `JOIN_STRATEGY`
  - `VALIDATED_SQL`
  - `METRIC_CALCULATION`

## How To Write Good Memory
Write concise, reusable conclusions.
- Remove turn-specific wording such as "this time", "for the current task", or transcript-only details
- Prefer one clear sentence over a paragraph
- State the lasting rule, preference, or fact directly
- Use `title` as a short label and `content` as the authoritative wording
- Use `reason` to explain why this should be remembered, not to restate the content

## Update Behavior
If a new instruction corrects or replaces an older preference/rule, write the newer memory so the system can overwrite the old one. Do not preserve conflicting parallel versions on purpose.

## Concrete Examples
Use these patterns to decide whether to write memory and how to classify it.

Example A - Stable user preference
- Conversation signal: "Always answer in concise bullet points and include final SQL in a code block."
- Write memory: scope=`USER`, memoryType=`PREFERENCE`, subType=`OUTPUT_FORMAT`
- Content example: "User prefers concise bullet responses and wants final SQL wrapped in a code block."

Example B - Workspace rule or constraint
- Conversation signal: "In analytics workspace, always use catalogName terminology and never databaseName in SQL explanations."
- Write memory: scope=`WORKSPACE`, memoryType=`WORKFLOW_CONSTRAINT`, subType=`IMPLEMENTATION_CONSTRAINT`
- Binding example: workspaceLevel=`CATALOG`, workspaceConnectionId=`12`, workspaceCatalogName=`analytics`
- Content example: "For analytics catalog, use catalogName terminology and avoid databaseName wording."

Example C - Reusable validated knowledge or SQL pattern
- Conversation signal: repeated verified query review confirms a safe pattern for monthly revenue aggregation.
- Write memory: scope=`WORKSPACE`, memoryType=`GOLDEN_SQL_CASE`, subType=`QUERY_PATTERN`
- Content example: "Monthly revenue in analytics is computed by grouping paid orders by date_trunc('month', paid_at) and summing amount_cents / 100.0."

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
