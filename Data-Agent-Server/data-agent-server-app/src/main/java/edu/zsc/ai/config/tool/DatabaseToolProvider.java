package edu.zsc.ai.config.tool;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agent.tool.ToolSpecifications;
import dev.langchain4j.service.tool.DefaultToolExecutor;
import dev.langchain4j.service.tool.ToolExecutor;
import dev.langchain4j.service.tool.ToolProvider;
import dev.langchain4j.service.tool.ToolProviderRequest;
import dev.langchain4j.service.tool.ToolProviderResult;
import edu.zsc.ai.tool.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Unified tool provider for all database-related tools.
 * Automatically extracts and provides all @Tool methods from injected tool beans.
 */
@Component
@RequiredArgsConstructor
public class DatabaseToolProvider implements ToolProvider {

    private final TodoTool todoTool;
    private final TableTool tableTool;
    private final AskUserQuestionTool askUserQuestionTool;
    private final ConnectionTool connectionTool;
    private final DatabaseTool databaseTool;
    private final ExecuteSqlTool executeSqlTool;

    @Override
    public ToolProviderResult provideTools(ToolProviderRequest request) {
        Map<ToolSpecification, ToolExecutor> tools = new HashMap<>();

        // Add all tools from each tool bean
        addToolsFrom(todoTool, tools);
        addToolsFrom(tableTool, tools);
        addToolsFrom(askUserQuestionTool, tools);
        addToolsFrom(connectionTool, tools);
        addToolsFrom(databaseTool, tools);
        addToolsFrom(executeSqlTool, tools);

        return new ToolProviderResult(tools);
    }

    /**
     * Extract all @Tool methods from an object and add them to the tools map
     */
    private void addToolsFrom(Object toolObject, Map<ToolSpecification, ToolExecutor> tools) {
        List<ToolSpecification> specifications = ToolSpecifications.toolSpecificationsFrom(toolObject);

        for (ToolSpecification spec : specifications) {
            Method method = findMethodByName(toolObject.getClass(), spec.name());
            if (method != null) {
                ToolExecutor executor = new DefaultToolExecutor(toolObject, method);
                tools.put(spec, executor);
            }
        }
    }

    /**
     * Find method by name in the given class
     */
    private Method findMethodByName(Class<?> clazz, String methodName) {
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.getName().equals(methodName)) {
                return method;
            }
        }
        return null;
    }
}
