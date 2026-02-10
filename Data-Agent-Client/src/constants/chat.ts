/**
 * API paths and error/label constants for chat and AI assistant.
 */

/** Default chat stream API path. */
export const CHAT_STREAM_API = '/api/chat/stream';

/** Submit tool answer (e.g. askUserQuestion) API path. */
export const SUBMIT_TOOL_ANSWER_API = '/api/chat/submit-tool-answer';

/** Thrown when no valid access token. */
export const NOT_AUTHENTICATED = 'Not authenticated';

/** Shown when session expired and user must login again. */
export const SESSION_EXPIRED_MESSAGE = 'Session expired, please login again';

/** Shown when submitToolAnswer is called without conversationId. */
export const CONVERSATION_ID_REQUIRED_FOR_TOOL_ANSWER =
  'conversationId is required for submit-tool-answer';

/** ThoughtBlock label while streaming (thinking). */
export const THOUGHT_LABEL_THINKING = 'Thinking';

/** ThoughtBlock label when thought is done. */
export const THOUGHT_LABEL_THOUGHT = 'Thought';

/** Shown when streaming but no backend data received for a while. */
export const PLANNING_LABEL = 'planning...';

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
