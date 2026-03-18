/**
 * Console Tab Metadata
 * Stored in Tab.metadata for console/file tabs
 * Tracks the execution context (connection, database, schema)
 */
export interface ConsoleTabMetadata {
  connectionId: number;
  connectionName: string;   // e.g., "MySQL-Dev", "PostgreSQL-Prod"
  databaseName: string | null;
  schemaName: string | null;
  catalog?: string | null;  // MySQL: database name
}

/**
 * Table Data Tab Metadata
 * Extends ConsoleTabMetadata for table/view data tabs
 */
export interface TableTabMetadata extends ConsoleTabMetadata {
  objectName: string;       // table or view name
  objectType: 'table' | 'view';
  catalog?: string;
  schema?: string;
  currentPage?: number;
  pageSize?: number;
  whereClause?: string;
  orderBy?: string;
  highlightColumn?: string;
}

export type TableDataTabMetadata = TableTabMetadata;

/**
 * Plan Tab Metadata
 * Stored in Tab.metadata for plan tabs opened from AI ExitPlanMode
 */
export interface PlanTabMetadata {
  planPayload: import('../components/ai/blocks/exitPlanModeTypes').ExitPlanPayload;
}

export interface SubAgentParamsSummary {
  userQuestion?: string;
  connectionIds?: number[];
  taskCount?: number;
  timeoutSeconds?: number;
}

/**
 * SubAgent Console Tab Metadata
 * Stores all SubAgent invocations for a conversation, displayed in the Console tab.
 */
export interface SubAgentConsoleTabMetadata {
  conversationId: number | null;
  agentType: import('../components/ai/blocks/subAgentTypes').SubAgentType;
  status: 'running' | 'complete' | 'error';
  startedAt: number;
  completedAt?: number;
  params?: SubAgentParamsSummary;
  summary?: string;
  resultJson?: string;
  invocations: SubAgentInvocation[];
}

/** Minimal info for a nested tool call (e.g. getEnvironmentOverview, searchObjects) in Explorer Console. */
export interface NestedToolCall {
  toolName: string;
  status: 'pending' | 'running' | 'complete';
  responseError?: boolean;
}

export interface SubAgentInvocation {
  id: string;
  taskLabel: string;
  agentType: import('../components/ai/blocks/subAgentTypes').SubAgentType;
  status: 'running' | 'complete' | 'error';
  errorMessage?: string;
  params: SubAgentParamsSummary;
  progressEvents: import('../components/ai/blocks/subAgentTypes').SubAgentProgressEvent[];
  /** Nested tool calls (getEnvironmentOverview, searchObjects, getObjectDetail) for real-time display. */
  nestedToolCalls?: NestedToolCall[];
  /** Full nested tool run segments for reuse in the console renderer. */
  nestedToolRuns?: import('../components/ai/messageListLib/types').Segment[];
  /** Final result summary only when the frontend has a real task-scoped result. Never use status text here. */
  resultSummary?: string;
  /** Final result JSON only when the frontend has a real task-scoped result payload. */
  resultJson?: string;
  tokenUsage?: number;
  trace?: Record<string, unknown>;
  startedAt: number;
  completedAt?: number;
}
