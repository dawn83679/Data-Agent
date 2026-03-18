package edu.zsc.ai.config.ai;

import edu.zsc.ai.agent.subagent.SubAgent;
import edu.zsc.ai.agent.subagent.SubAgentRequest;
import edu.zsc.ai.agent.subagent.contract.PlannerRequest;
import edu.zsc.ai.agent.subagent.contract.SchemaSummary;
import edu.zsc.ai.agent.subagent.contract.SqlPlan;
import edu.zsc.ai.agent.subagent.explorer.ExplorerSubAgent;
import edu.zsc.ai.agent.subagent.planner.PlannerSubAgent;
import lombok.Getter;
import org.springframework.context.annotation.Lazy;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * Aggregates SubAgent-related dependencies for orchestrator tools.
 * Reduces constructor parameter count in orchestrator tools.
 */
@Component
@Getter
public class SubAgentManager {

    private final SubAgent<SubAgentRequest, SchemaSummary> explorerSubAgent;
    private final SubAgent<PlannerRequest, SqlPlan> plannerSubAgent;
    private final SubAgentProperties properties;

    public SubAgentManager(
            @Lazy @Qualifier("loggingExplorerSubAgent") SubAgent<SubAgentRequest, SchemaSummary> explorerSubAgent,
            @Lazy @Qualifier("loggingPlannerSubAgent") SubAgent<PlannerRequest, SqlPlan> plannerSubAgent,
            SubAgentProperties properties) {
        this.explorerSubAgent = explorerSubAgent;
        this.plannerSubAgent = plannerSubAgent;
        this.properties = properties;
    }
}
