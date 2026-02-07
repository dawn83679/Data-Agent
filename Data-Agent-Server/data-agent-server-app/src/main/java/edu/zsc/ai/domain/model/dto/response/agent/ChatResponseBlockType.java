package edu.zsc.ai.domain.model.dto.response.agent;

/**
 * Chat Response Block Type
 */
public enum ChatResponseBlockType {
    TEXT,           // Regular text response
    THOUGHT,        // Reasoning/thinking
    TOOL_CALL,      // Tool invocation
    TOOL_RESULT,    // Tool execution result
    DONE            // Stream completed
}
