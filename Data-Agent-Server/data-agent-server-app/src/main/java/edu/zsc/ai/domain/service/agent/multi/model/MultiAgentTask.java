package edu.zsc.ai.domain.service.agent.multi.model;

import edu.zsc.ai.common.enums.ai.AgentRoleEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MultiAgentTask {

    private Long taskId;
    private Long runId;
    private Long parentTaskId;
    private AgentRoleEnum agentRole;
    private String title;
    private String goal;
    private String status;
    private Integer sequence;
    private String summary;
}
