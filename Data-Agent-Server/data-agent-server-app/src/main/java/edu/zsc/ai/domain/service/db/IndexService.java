package edu.zsc.ai.domain.service.db;

import edu.zsc.ai.domain.model.context.DbContext;
import edu.zsc.ai.plugin.model.metadata.IndexMetadata;

import java.util.List;

public interface IndexService {

    List<IndexMetadata> getIndexes(DbContext db, String tableName);
}
