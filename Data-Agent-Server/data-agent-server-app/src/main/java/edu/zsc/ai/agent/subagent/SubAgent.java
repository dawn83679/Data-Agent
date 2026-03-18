package edu.zsc.ai.agent.subagent;

import edu.zsc.ai.common.enums.ai.AgentTypeEnum;

/**
 * Generic sub-agent interface for the multi-agent architecture.
 * Each sub-agent defines its own input/output types:
 * <ul>
 *   <li>Explorer: SubAgentRequest → SchemaSummary</li>
 *   <li>Planner: PlannerRequest → SqlPlan</li>
 * </ul>
 *
 * @param <I> the request type this sub-agent accepts
 * @param <O> the result type this sub-agent produces
 */
public interface SubAgent<I, O> {

    /**
     * The type of this sub-agent.
     */
    AgentTypeEnum getAgentType();

    /**
     * Invoke the sub-agent with the given request and return a typed result.
     * Each invocation uses temporary memory (no state leaks to MainAgent history).
     */
    O invoke(I request);
}
