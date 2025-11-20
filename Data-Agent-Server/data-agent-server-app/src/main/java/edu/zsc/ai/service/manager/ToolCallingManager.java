package edu.zsc.ai.service.manager;

import java.util.Map;

public interface ToolCallingManager {

    void registerTool(String toolName, ToolExecutor executor);

    Object executeToolCall(String toolName, Map<String, Object> parameters);
}
