package edu.zsc.ai.agent.subagent;

import java.util.List;

/**
 * Request for Explorer SubAgent.
 *
 * @param instruction  task instruction from MainAgent to Explorer
 * @param connectionIds target database connection IDs (1-N)
 * @param context      optional context (conversation summary, previous errors, etc.)
 */
public record SubAgentRequest(
        String instruction,
        List<Long> connectionIds,
        String context,
        Long timeoutSeconds
) {
    public SubAgentRequest(String instruction, List<Long> connectionIds, String context) {
        this(instruction, connectionIds, context, null);
    }
}
