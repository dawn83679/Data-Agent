package edu.zsc.ai.agent.subagent.contract;

import lombok.Builder;
import lombok.Data;

/**
 * Request for Planner SubAgent.
 * Optimization context (existing SQL, DDLs, indexes) should be included in instruction by MainAgent.
 */
@Data
@Builder
public class PlannerRequest {
    private String instruction;
    private SchemaSummary schemaSummary;
    private Long timeoutSeconds;
}
