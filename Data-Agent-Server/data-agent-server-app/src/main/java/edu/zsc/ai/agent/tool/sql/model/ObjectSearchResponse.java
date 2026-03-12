package edu.zsc.ai.agent.tool.sql.model;

import java.util.List;

/**
 * Search result wrapper with truncation info and optional per-connection or per-scope errors.
 * When some connections or schemas failed (e.g. connection refused), {@code errors} is set so the model can see failures.
 */
public record ObjectSearchResponse(
        List<ObjectSearchResult> results,
        int totalCount,
        boolean truncated,
        List<String> errors) {

    public ObjectSearchResponse(List<ObjectSearchResult> results, int totalCount, boolean truncated) {
        this(results, totalCount, truncated, null);
    }
}
