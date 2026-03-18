export type ToolCallPreviewFieldKey =
  | 'instruction'
  | 'userQuestion'
  | 'taskInstructions'
  | 'connectionIds'
  | 'databaseName'
  | 'schemaName'
  | 'sql';

export type ToolCallPreviewRenderPhase = 'streaming' | 'executing' | 'completed';

export type ToolCallPreviewFallbackMode = 'none' | 'raw';

export interface ToolCallPreviewConfig {
  fields: ToolCallPreviewFieldKey[];
  fallbackByPhase: Record<ToolCallPreviewRenderPhase, ToolCallPreviewFallbackMode>;
  fallbackLabel?: string;
}

const DEFAULT_TOOL_CALL_PREVIEW_CONFIG: ToolCallPreviewConfig = {
  fields: [],
  fallbackByPhase: {
    streaming: 'none',
    executing: 'raw',
    completed: 'raw',
  },
  fallbackLabel: 'Parameters',
};

const TOOL_CALL_PREVIEW_CONFIGS: Record<string, ToolCallPreviewConfig> = {
  callingExplorerSubAgent: {
    fields: ['taskInstructions', 'connectionIds'],
    fallbackByPhase: {
      streaming: 'none',
      executing: 'none',
      completed: 'none',
    },
  },
  callingPlannerSubAgent: {
    fields: ['instruction'],
    fallbackByPhase: {
      streaming: 'none',
      executing: 'none',
      completed: 'none',
    },
  },
  askUserConfirm: {
    fields: ['databaseName', 'schemaName', 'sql'],
    fallbackByPhase: {
      streaming: 'none',
      executing: 'raw',
      completed: 'raw',
    },
  },
};

export function getToolCallPreviewConfig(toolName: string): ToolCallPreviewConfig {
  return TOOL_CALL_PREVIEW_CONFIGS[toolName] ?? DEFAULT_TOOL_CALL_PREVIEW_CONFIG;
}
