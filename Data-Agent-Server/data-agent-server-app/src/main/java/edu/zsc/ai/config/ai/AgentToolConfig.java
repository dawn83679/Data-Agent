package edu.zsc.ai.config.ai;

import edu.zsc.ai.agent.tool.annotation.AgentTool;
import edu.zsc.ai.agent.tool.ask.AskUserConfirmTool;
import edu.zsc.ai.agent.tool.chart.ChartTool;
import edu.zsc.ai.agent.tool.plan.ExitPlanModeTool;
import edu.zsc.ai.agent.tool.sql.ExecuteSqlTool;
import edu.zsc.ai.common.enums.ai.AgentModeEnum;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;
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

    @Bean
    public List<Object> agentTools(ApplicationContext context) {
        return new ArrayList<>(context.getBeansWithAnnotation(AgentTool.class).values());
    }

    public List<Object> filterTools(List<Object> agentTools, AgentModeEnum mode) {
        Set<Class<?>> disabled = (mode == AgentModeEnum.PLAN)
                ? PLAN_MODE_DISABLED
                : AGENT_MODE_DISABLED;
        return agentTools.stream()
                .filter(tool -> !disabled.contains(tool.getClass()))
                .toList();
    }
}
