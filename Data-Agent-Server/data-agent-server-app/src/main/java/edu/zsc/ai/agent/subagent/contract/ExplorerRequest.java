package edu.zsc.ai.agent.subagent.contract;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ExplorerRequest {
    private String instruction;
    private List<Long> connectionIds;
    private String context;
}
