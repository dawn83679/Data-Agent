package edu.zsc.ai.agent.tool.sql.model;

import edu.zsc.ai.plugin.model.metadata.IndexMetadata;

import java.util.List;

public record NamedObjectDetail(
        String objectName,
        String objectType,
        Long connectionId,
        String databaseName,
        String schemaName,
        boolean success,
        String error,
        String ddl,
        Long rowCount,
        List<IndexMetadata> indexes
) {

    public NamedObjectDetail {
        if (indexes == null) indexes = List.of();
    }
}
