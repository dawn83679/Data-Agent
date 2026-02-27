/**
 * API paths and error/label constants for chat and AI assistant.
 */

/** Default chat stream API path. */
export const CHAT_STREAM_API = '/api/chat/stream';

/** Thrown when no valid access token. */
export const NOT_AUTHENTICATED = 'Not authenticated';

/** Shown when session expired and user must login again. */
export const SESSION_EXPIRED_MESSAGE = 'Session expired, please login again';

/** ThoughtBlock label while streaming (thinking). */
export const THOUGHT_LABEL_THINKING = 'Thinking';

/** ThoughtBlock label when thought is done. */
export const THOUGHT_LABEL_THOUGHT = 'Thought';

/** ToolRunBlock: prefix when tool execution failed. */
export const TOOL_RUN_LABEL_FAILED = 'Failed ';

/** ToolRunBlock: prefix when tool ran successfully. */
export const TOOL_RUN_LABEL_RAN = 'Ran ';

/** ToolRunBlock: section title for parameters. */
export const TOOL_RUN_SECTION_PARAMETERS = 'PARAMETERS';

/** ToolRunBlock: section title for response. */
export const TOOL_RUN_SECTION_RESPONSE = 'RESPONSE';

/** Placeholder when parameters or response is empty. */
export const TOOL_RUN_EMPTY_PLACEHOLDER = '—';

/** PlanningIndicator: label shown while waiting for AI response or between blocks. */
export const PLANNING_LABEL = 'Planning...';
