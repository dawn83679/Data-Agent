package edu.zsc.ai.agent.subagent.contract;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PlannerRequest {
    private String instruction;
    private SchemaSummary schemaSummary;
    private Long timeoutSeconds;
}
