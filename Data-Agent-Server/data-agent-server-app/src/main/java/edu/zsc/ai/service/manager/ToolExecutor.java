package edu.zsc.ai.service.manager;

import java.util.Map;

@FunctionalInterface
public interface ToolExecutor {
    Object execute(Map<String, Object> parameters);
}
