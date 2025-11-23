package edu.zsc.ai.service.ai.manager;

import edu.zsc.ai.model.dto.request.ai.ToolCallResult;

import java.util.Map;


public interface ToolExecutor {
    /**
     * Get tool name
     */
    String getToolName();

    /**
     * Get tool description
     */
    String getToolDescription();

    /**
     * Execute tool
     * New unified method, automatically gets context from ThreadLocal
     *
     * @param params tool parameters
     * @return execution result
     */
    ToolCallResult execute(Map<String, Object> params);

    /**
     * Validate tool parameters
     *
     * @param params tool parameters
     * @return validation result, null means validation passed, otherwise returns error message
     */
    default String validateParams(Map<String, Object> params) {
        return null; // Default validation passed
    }

    /**
     * Whether user confirmation is needed
     *
     * @param params tool parameters
     * @return true means user confirmation is needed, false means automatic execution is allowed
     */
    default boolean needUserConfirmation(Map<String, Object> params) {
        return true; // Default requires user confirmation
    }

    /**
     * Get tool parameter definitions
     *
     * @return parameter definition Map, key is parameter name, value is parameter description
     */
    default Map<String, String> getParameterDefinitions() {
        return Map.of(); // Default no parameter definitions
    }
}
