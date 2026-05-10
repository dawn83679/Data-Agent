package edu.zsc.ai.agent.subagent.contract;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExplorerTask {
    private Long connectionId;

    private String instruction;

    private String context;

    private Long timeoutSeconds;
}
