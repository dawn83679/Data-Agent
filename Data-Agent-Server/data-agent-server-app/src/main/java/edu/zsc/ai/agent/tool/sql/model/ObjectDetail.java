package edu.zsc.ai.agent.tool.sql.model;

import edu.zsc.ai.plugin.model.metadata.IndexMetadata;

import java.util.List;

/**
 * Combined object detail: DDL + row count + indexes.
 * For TABLE/VIEW: row count and indexes may be present.
 * For FUNCTION/PROCEDURE/TRIGGER: only ddl is typically present.
 */
public record ObjectDetail(String ddl, Long rowCount, List<IndexMetadata> indexes) {

    public ObjectDetail {
        if (indexes == null) indexes = List.of();
    }
}
