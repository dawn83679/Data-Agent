export interface ExplorerObjectPreview {
  catalog?: string;
  schema?: string;
  objectName: string;
  objectType?: string;
  objectDdl?: string;
  relevanceScore?: number;
}

export interface ExplorerResultPayload {
  status?: 'SUCCESS' | 'ERROR';
  summaryText?: string;
  objects: ExplorerObjectPreview[];
  errorMessage?: string;
  rawResponse?: string;
}

export interface PlannerStepPreview {
  title?: string;
  content?: string;
}

export const PLANNER_SQL_BLOCK_KIND = {
  FINAL: 'FINAL',
  CHECK: 'CHECK',
  ALTERNATIVE: 'ALTERNATIVE',
} as const;

export type PlannerSqlBlockKind = typeof PLANNER_SQL_BLOCK_KIND[keyof typeof PLANNER_SQL_BLOCK_KIND];

export interface PlannerSqlBlockPreview {
  title?: string;
  sql: string;
  kind?: PlannerSqlBlockKind;
}

export interface PlannerResultPayload {
  summaryText?: string;
  planSteps: PlannerStepPreview[];
  sqlBlocks: PlannerSqlBlockPreview[];
  rawResponse?: string;
}
