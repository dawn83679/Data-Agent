package edu.zsc.ai.agent.subagent.contract;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * callingExplorerSubAgent final payload returned to MainAgent.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExplorerResultEnvelope {

    private List<ExplorerTaskResult> taskResults;
}
