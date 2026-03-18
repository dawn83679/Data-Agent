package edu.zsc.ai.agent.tool.sql.model;

import edu.zsc.ai.plugin.model.metadata.ColumnMetadata;
import edu.zsc.ai.plugin.model.metadata.IndexMetadata;

import java.util.List;

/**
 * Combined object detail: DDL + row count + indexes + columns.
 * For TABLE/VIEW: columns populated from ColumnService.
 * For FUNCTION/PROCEDURE/TRIGGER: only ddl is present, columns empty.
 */
public record ObjectDetail(String ddl, Long rowCount, List<IndexMetadata> indexes, List<ColumnMetadata> columns) {

    public ObjectDetail {
        if (columns == null) columns = List.of();
    }

    /** Backward-compatible constructor (columns = empty). */
    public ObjectDetail(String ddl, Long rowCount, List<IndexMetadata> indexes) {
        this(ddl, rowCount, indexes, List.of());
    }
}
