package edu.zsc.ai.agent.subagent.contract;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Contract DTO for Explorer SubAgent request.
 */
@Data
@Builder
public class ExplorerRequest {
    private String instruction;
    private List<Long> connectionIds;
    private String context;
}
