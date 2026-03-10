package edu.zsc.ai.domain.service.agent.multi.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubAgentDelegationResult {

    private Long runId;
    private Long taskId;
    private String agentRole;
    private String title;
    private String status;
    private String summary;
    private String details;
    private boolean requiresApproval;
}
