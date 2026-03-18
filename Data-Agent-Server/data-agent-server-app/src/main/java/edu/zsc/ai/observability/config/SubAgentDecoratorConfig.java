package edu.zsc.ai.observability.config;

import edu.zsc.ai.agent.subagent.SubAgent;
import edu.zsc.ai.agent.subagent.SubAgentRequest;
import edu.zsc.ai.agent.subagent.contract.PlannerRequest;
import edu.zsc.ai.agent.subagent.contract.SchemaSummary;
import edu.zsc.ai.agent.subagent.contract.SqlPlan;
import edu.zsc.ai.agent.subagent.explorer.ExplorerSubAgent;
import edu.zsc.ai.agent.subagent.planner.PlannerSubAgent;
import edu.zsc.ai.observability.AgentLogService;
import edu.zsc.ai.observability.decorator.LoggingSubAgentDecorator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SubAgentDecoratorConfig {

    @Bean("loggingExplorerSubAgent")
    public SubAgent<SubAgentRequest, SchemaSummary> loggingExplorerSubAgent(
            ExplorerSubAgent delegate,
            AgentLogService agentLogService) {
        @SuppressWarnings("unchecked")
        SubAgent<SubAgentRequest, SchemaSummary> decorated =
                (SubAgent<SubAgentRequest, SchemaSummary>) LoggingSubAgentDecorator.explorer(delegate, agentLogService);
        return decorated;
    }

    @Bean("loggingPlannerSubAgent")
    public SubAgent<PlannerRequest, SqlPlan> loggingPlannerSubAgent(
            PlannerSubAgent delegate,
            AgentLogService agentLogService) {
        @SuppressWarnings("unchecked")
        SubAgent<PlannerRequest, SqlPlan> decorated =
                (SubAgent<PlannerRequest, SqlPlan>) LoggingSubAgentDecorator.planner(delegate, agentLogService);
        return decorated;
    }
}
