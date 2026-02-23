package edu.zsc.ai.domain.service.db;

import edu.zsc.ai.plugin.model.metadata.IndexMetadata;

import java.util.List;

public interface IndexService {

    List<IndexMetadata> getIndexes(Long connectionId, String catalog, String schema, String tableName, Long userId);
}
