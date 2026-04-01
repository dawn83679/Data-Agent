package edu.zsc.ai.config.ai;

import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ReturnBehavior;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agent.tool.ToolSpecifications;
import dev.langchain4j.service.tool.DefaultToolExecutor;
import dev.langchain4j.service.tool.ToolExecutor;
import edu.zsc.ai.agent.annotation.AgentTool;
import edu.zsc.ai.agent.tool.ask.AskUserQuestionTool;
import edu.zsc.ai.agent.tool.chart.ChartTool;
import edu.zsc.ai.agent.tool.export.ExportFileTool;
import edu.zsc.ai.agent.tool.orchestrator.CallingExplorerTool;
import edu.zsc.ai.agent.tool.orchestrator.CallingPlannerTool;
import edu.zsc.ai.agent.tool.plan.ExitPlanModeTool;
import edu.zsc.ai.agent.tool.skill.ActivateSkillTool;
import edu.zsc.ai.agent.tool.sql.GetObjectDetailTool;
import edu.zsc.ai.agent.tool.sql.GetDatabasesTool;
import edu.zsc.ai.agent.tool.sql.GetSchemasTool;
import edu.zsc.ai.agent.tool.sql.SearchObjectsTool;
import edu.zsc.ai.agent.tool.sql.ExecuteSqlTool;
import edu.zsc.ai.agent.tool.todo.TodoTool;
import edu.zsc.ai.common.enums.ai.AgentModeEnum;
import edu.zsc.ai.common.enums.ai.AgentTypeEnum;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Configuration
public class AgentToolConfig {

    private static final Map<ToolScope, Set<Class<?>>> TOOL_SCOPE_ALLOWLISTS = Map.of(
            ToolScope.MAIN_AGENT, Set.of(
                    GetDatabasesTool.class,
                    GetSchemasTool.class,
                    SearchObjectsTool.class,
                    GetObjectDetailTool.class,
                    ExecuteSqlTool.class,
                    CallingExplorerTool.class,
                    CallingPlannerTool.class,
                    ChartTool.class,
                    AskUserQuestionTool.class,
                    TodoTool.class,
                    ActivateSkillTool.class,
                    ExportFileTool.class
            ),
            ToolScope.MAIN_PLAN, Set.of(
                    GetDatabasesTool.class,
                    GetSchemasTool.class,
                    CallingExplorerTool.class,
                    CallingPlannerTool.class,
                    AskUserQuestionTool.class,
                    TodoTool.class,
                    ExitPlanModeTool.class
            ),
            ToolScope.EXPLORER, Set.of(
                    TodoTool.class,
                    SearchObjectsTool.class,
                    GetObjectDetailTool.class,
                    ExecuteSqlTool.class
            ),
            ToolScope.PLANNER, Set.of(
                    TodoTool.class,
                    GetObjectDetailTool.class,
                    ExecuteSqlTool.class
            )
    );

    @Bean
    public List<Object> agentTools(ApplicationContext context) {
        return new ArrayList<>(context.getBeansWithAnnotation(AgentTool.class).values());
    }

    public ToolBundle buildToolBundle(List<Object> agentTools) {
        if (CollectionUtils.isEmpty(agentTools)) {
            return new ToolBundle(Map.of(), Set.of());
        }

        List<ToolRegistration> registrations = agentTools.stream()
                .flatMap(tool -> resolveToolRegistrations(tool).stream())
                .toList();

        ToolSpecifications.validateSpecifications(registrations.stream()
                .map(ToolRegistration::specification)
                .collect(Collectors.toList()));

        Map<ToolSpecification, ToolExecutor> executors = new LinkedHashMap<>();
        Set<String> immediateReturnToolNames = new LinkedHashSet<>();
        for (ToolRegistration registration : registrations) {
            executors.put(
                    registration.specification(),
                    new DefaultToolExecutor(
                            registration.toolBean(),
                            registration.originalMethod(),
                            registration.invocableMethod()
                    )
            );
            if (registration.returnBehavior() == ReturnBehavior.IMMEDIATE) {
                immediateReturnToolNames.add(registration.specification().name());
            }
        }
        return new ToolBundle(executors, Set.copyOf(immediateReturnToolNames));
    }

    /**
     * Resolve the exact tool set exposed to the Main Agent.
     * enterPlanMode is not exposed. exitPlanMode is exposed only in direct Plan mode.
     */
    public List<Object> resolveMainTools(List<Object> agentTools, AgentModeEnum mode) {
        ToolScope scope = mode == AgentModeEnum.PLAN ? ToolScope.MAIN_PLAN : ToolScope.MAIN_AGENT;
        return resolveTools(agentTools, scope);
    }

    /**
     * Resolve the exact tool set exposed to a sub-agent.
     */
    public List<Object> resolveSubAgentTools(List<Object> agentTools, AgentTypeEnum agentType) {
        ToolScope scope = switch (agentType) {
            case EXPLORER -> ToolScope.EXPLORER;
            case PLANNER -> ToolScope.PLANNER;
            case MAIN -> throw new IllegalArgumentException("MAIN is not a sub-agent");
        };
        return resolveTools(agentTools, scope);
    }

    /**
     * Check if tool is an instance of any class in the set.
     * Uses instanceof semantics so it works with subclasses and proxies.
     */
    private static boolean matchesAny(Object tool, Set<Class<?>> classes) {
        for (Class<?> clazz : classes) {
            if (clazz.isInstance(tool)) {
                return true;
            }
        }
        return false;
    }

    private List<Object> resolveTools(List<Object> agentTools, ToolScope scope) {
        if (CollectionUtils.isEmpty(agentTools)) {
            return List.of();
        }
        Set<Class<?>> allowlist = TOOL_SCOPE_ALLOWLISTS.get(scope);
        if (allowlist == null) {
            throw new IllegalArgumentException("Unknown tool scope: " + scope);
        }
        return agentTools.stream()
                .filter(tool -> matchesAny(tool, allowlist))
                .toList();
    }

    private List<ToolRegistration> resolveToolRegistrations(Object toolBean) {
        Class<?> targetClass = AopUtils.getTargetClass(toolBean);
        if (targetClass == null) {
            return List.of();
        }

        List<ToolRegistration> registrations = new ArrayList<>();
        for (Method method : targetClass.getDeclaredMethods()) {
            if (!method.isAnnotationPresent(Tool.class)) {
                continue;
            }
            Method invocableMethod = AopUtils.selectInvocableMethod(method, toolBean.getClass());
            registrations.add(new ToolRegistration(
                    toolBean,
                    ToolSpecifications.toolSpecificationFrom(method),
                    method,
                    invocableMethod,
                    method.getAnnotation(Tool.class).returnBehavior()
            ));
        }
        return registrations;
    }

    private record ToolRegistration(
            Object toolBean,
            ToolSpecification specification,
            Method originalMethod,
            Method invocableMethod,
            ReturnBehavior returnBehavior
    ) {
    }

    public record ToolBundle(
            Map<ToolSpecification, ToolExecutor> executors,
            Set<String> immediateReturnToolNames
    ) {
    }

    private enum ToolScope {
        MAIN_AGENT,
        MAIN_PLAN,
        EXPLORER,
        PLANNER
    }
}
