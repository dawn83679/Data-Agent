package edu.zsc.ai.domain.service.db;

import edu.zsc.ai.domain.model.context.DbContext;
import edu.zsc.ai.plugin.model.metadata.ColumnMetadata;

import java.util.List;

public interface ColumnService {

    List<ColumnMetadata> listColumns(DbContext db, String tableName);
}
