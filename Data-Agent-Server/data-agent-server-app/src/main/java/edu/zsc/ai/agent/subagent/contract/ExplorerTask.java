package edu.zsc.ai.agent.subagent.contract;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A single exploration task within a callingExplorerSubAgent invocation.
 * Each task targets one connectionId and spawns one Explorer SubAgent.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExplorerTask {
    /** Target database connection ID (from the available connections in the runtime context). */
    private Long connectionId;

    /** Task instruction — what schema info to explore for this connection. */
    private String instruction;

    /** Optional context: conversation summary, previous errors, etc. */
    private String context;

    /** Optional timeout override for this specific exploration task. */
    private Long timeoutSeconds;
}
