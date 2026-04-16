package edu.zsc.ai.agent;

/**
 * Provides the appropriate ReActAgent for a given model name
 * (e.g. qwen3.6-plus, qwen3-max-2026-01-23, qwen3-max-thinking).
 */
public interface ReActAgentProvider {

    /**
     * Returns the ReActAgent for the given model name, prompt language, and agent mode.
     *
     * @param modelName model name (must be validated against the configured AI model catalog beforehand)
     * @param language  prompt language (e.g. en, zh). Unknown values should fallback to default.
     * @param agentMode agent mode code (e.g. "agent", "plan"). Null/blank defaults to agent.
     * @return the prepared agent and the exact rendered system prompt used for this chat
     */
    PreparedReActAgent getAgent(String modelName, String language, String agentMode);
}
