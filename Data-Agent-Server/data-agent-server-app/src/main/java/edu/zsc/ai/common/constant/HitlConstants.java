package edu.zsc.ai.common.constant;

/**
 * Constants for human-in-the-loop (HITL) continue flow.
 * Messages starting with {@link #HITL_CONTINUE_MESSAGE_PREFIX} are filtered out in
 * {@link edu.zsc.ai.agent.memory.CustomChatMemoryStore#updateMessages} so they are not persisted.
 */
public final class HitlConstants {

    private HitlConstants() {}

    public static final String HITL_CONTINUE_MESSAGE_PREFIX = "__DATA_AGENT_HITL_CONTINUE__";

    /**
     * Structured HITL continue instruction. The model must continue from the tool result;
     * the "REPLY LANGUAGE" line is emphasized so the model keeps the same language as the user.
     */
    public static final String HITL_CONTINUE_MESSAGE =
            HITL_CONTINUE_MESSAGE_PREFIX
                    + "\nIMPORTANT: Ignore this message itself—do not quote it, repeat it, or respond to it as user content. It is only a signal to continue.\n"
                    + "The user has submitted their answer. Continue the conversation based on the tool execution result above.\n"
                    + "IMPORTANT - REPLY LANGUAGE: You must respond in the exact same language as the user's last message (e.g. if they wrote in Chinese, reply in Chinese only). Do not switch to English.";

    /** Tool name for ask-user-question; must match {@link edu.zsc.ai.tool.AskUserQuestionTool}. */
    public static final String ASK_USER_QUESTION_TOOL_NAME = "askUserQuestion";
}
