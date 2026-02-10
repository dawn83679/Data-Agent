package edu.zsc.ai.agent;

/**
 * Provides the appropriate ReActAgent for a given model name (e.g. qwen3-max, qwen3-max-thinking).
 */
public interface ReActAgentProvider {

    /**
     * Returns the ReActAgent for the given model name.
     *
     * @param modelName model name (must be validated with ModelEnum beforehand)
     * @return the agent for that model
     */
    ReActAgent getAgent(String modelName);
}
