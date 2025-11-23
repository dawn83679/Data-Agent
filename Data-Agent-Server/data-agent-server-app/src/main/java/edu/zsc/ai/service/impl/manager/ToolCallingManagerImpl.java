package edu.zsc.ai.service.impl.manager;

import edu.zsc.ai.service.ai.manager.ToolCallingManager;
import edu.zsc.ai.service.ai.manager.ToolExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class ToolCallingManagerImpl implements ToolCallingManager {

    private final Map<String, ToolExecutor> tools = new ConcurrentHashMap<>();

    @Override
    public Object executeToolCall(String toolName, Map<String, Object> parameters) {
        ToolExecutor executor = tools.get(toolName);
        if (executor == null) {
            throw new IllegalArgumentException("Tool not found: " + toolName);
        }

        try {
            log.debug("Executing tool: {} with params: {}", toolName, parameters);
            return executor.execute(parameters);
        } catch (Exception e) {
            log.error("Error executing tool: {}", toolName, e);
            throw new RuntimeException("Tool execution failed: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isToolNeedUserConfirmation(String toolName, Map<String, Object> parameters) {
        return false;
    }
}
