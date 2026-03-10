package edu.zsc.ai.config.ai;

import edu.zsc.ai.agent.annotation.AgentTool;
import edu.zsc.ai.agent.tool.ask.AskUserConfirmTool;
import edu.zsc.ai.agent.tool.ask.AskUserQuestionTool;
import edu.zsc.ai.agent.tool.chart.ChartTool;
import edu.zsc.ai.agent.tool.memory.MemoryTool;
import edu.zsc.ai.agent.tool.multi.MultiAgentDelegationTool;
import edu.zsc.ai.agent.tool.plan.ExitPlanModeTool;
import edu.zsc.ai.agent.tool.skill.ActivateSkillTool;
import edu.zsc.ai.agent.tool.sql.DiscoveryTool;
import edu.zsc.ai.agent.tool.sql.ExecuteSqlTool;
import edu.zsc.ai.agent.tool.think.ThinkingTool;
import edu.zsc.ai.agent.tool.todo.TodoTool;
import edu.zsc.ai.common.enums.ai.AgentModeEnum;
import edu.zsc.ai.common.enums.ai.AgentRoleEnum;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Configuration
public class AgentToolConfig {

    private static final Set<Class<?>> PLAN_MODE_DISABLED = Set.of(
            ExecuteSqlTool.class,
            ChartTool.class,
            AskUserConfirmTool.class
    );

    private static final Set<Class<?>> AGENT_MODE_DISABLED = Set.of(
            ExitPlanModeTool.class
    );

    private static final Set<Class<?>> MULTI_AGENT_ORCHESTRATOR_ALLOWED = Set.of(
            MultiAgentDelegationTool.class,
            ThinkingTool.class,
            AskUserQuestionTool.class,
            MemoryTool.class,
            TodoTool.class
    );

    private static final Map<AgentRoleEnum, Set<Class<?>>> ROLE_ALLOWED_TOOLS = Map.of(
            AgentRoleEnum.SCHEMA_ANALYST, Set.of(
                    DiscoveryTool.class,
                    ThinkingTool.class,
                    MemoryTool.class
            ),
            AgentRoleEnum.SQL_PLANNER, Set.of(
                    DiscoveryTool.class,
                    ThinkingTool.class,
                    MemoryTool.class,
                    TodoTool.class,
                    ActivateSkillTool.class
            ),
            AgentRoleEnum.SQL_EXECUTOR, Set.of(
                    ExecuteSqlTool.class,
                    AskUserConfirmTool.class
            ),
            AgentRoleEnum.RESULT_ANALYST, Set.of()
    );

    @Bean
    public List<Object> agentTools(ApplicationContext context) {
        return new ArrayList<>(context.getBeansWithAnnotation(AgentTool.class).values());
    }

    public List<Object> filterTools(List<Object> agentTools, AgentModeEnum mode) {
        if (mode == AgentModeEnum.MULTI_AGENT) {
            return agentTools.stream()
                    .filter(tool -> MULTI_AGENT_ORCHESTRATOR_ALLOWED.contains(tool.getClass()))
                    .toList();
        }
        Set<Class<?>> disabled = (mode == AgentModeEnum.PLAN)
                ? PLAN_MODE_DISABLED
                : AGENT_MODE_DISABLED;
        return agentTools.stream()
                .filter(tool -> !disabled.contains(tool.getClass()))
                .toList();
    }

    public List<Object> filterTools(List<Object> agentTools, AgentModeEnum mode, AgentRoleEnum role) {
        if (mode == AgentModeEnum.MULTI_AGENT && role != null) {
            Set<Class<?>> allowed = ROLE_ALLOWED_TOOLS.get(role);
            if (allowed == null) {
                return List.of();
            }
            return agentTools.stream()
                    .filter(tool -> allowed.contains(tool.getClass()))
                    .toList();
        }

        List<Object> modeFiltered = filterTools(agentTools, mode);
        if (role == null) {
            return modeFiltered;
        }
        Set<Class<?>> allowed = ROLE_ALLOWED_TOOLS.get(role);
        if (allowed == null) {
            return modeFiltered;
        }
        return modeFiltered.stream()
                .filter(tool -> allowed.contains(tool.getClass()))
                .toList();
    }
}
