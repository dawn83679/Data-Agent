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

    /**
     * Task instruction — a single narrow exploration question with named targets where possible
     * (e.g. "list columns of public.orders and verify whether status column exists").
     * Avoid open-ended phrasing like "explore the schema"; split wide goals into multiple
     * ExplorerTask entries so they can run in parallel.
     */
    private String instruction;

    /** Optional context: conversation summary, previous errors, etc. */
    private String context;

    /** Optional timeout override for this specific exploration task. */
    private Long timeoutSeconds;
}
