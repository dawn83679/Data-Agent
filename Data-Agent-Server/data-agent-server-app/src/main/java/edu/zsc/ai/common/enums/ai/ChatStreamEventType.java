package edu.zsc.ai.common.enums.ai;

import edu.zsc.ai.domain.model.dto.response.agent.ChatResponseBlock;

import java.util.Optional;

/**
 * Canonical event types used by SSE stream blocks and SSE log writer output.
 */
public enum ChatStreamEventType {

    TEXT,
    THOUGHT,
    TOOL_CALL,
    TOOL_RESULT,
    STATUS,
    SUB_AGENT_START,
    SUB_AGENT_PROGRESS,
    SUB_AGENT_COMPLETE,
    SUB_AGENT_ERROR,
    DONE,
    UNKNOWN;

    public static ChatStreamEventType resolve(ChatResponseBlock block) {
        if (block == null) {
            return UNKNOWN;
        }
        if (block.getType() != null && !block.getType().isBlank()) {
            return fromType(block.getType()).orElse(UNKNOWN);
        }
        return block.isDone() ? DONE : UNKNOWN;
    }

    public static Optional<ChatStreamEventType> fromType(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(ChatStreamEventType.valueOf(value.trim()));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
