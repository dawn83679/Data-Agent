package edu.zsc.ai.domain.service.ai.manager;

import java.util.Map;

public interface ToolCallingManager {

    Object executeToolCall(String toolName, Map<String, Object> parameters);

    boolean isToolNeedUserConfirmation(String toolName, Map<String, Object> parameters);
}
