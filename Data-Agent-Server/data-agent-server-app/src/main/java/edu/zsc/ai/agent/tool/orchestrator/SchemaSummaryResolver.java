package edu.zsc.ai.agent.tool.orchestrator;

import edu.zsc.ai.agent.subagent.contract.SchemaSummary;

/**
 * Resolves planner input into a normalized {@link SchemaSummary}.
 */
public interface SchemaSummaryResolver {

    SchemaSummary resolve(String schemaSummaryJson);
}
