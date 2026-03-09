package edu.zsc.ai.domain.service.db;

import edu.zsc.ai.domain.model.context.DbContext;
import edu.zsc.ai.plugin.model.metadata.PrimaryKeyMetadata;

import java.util.List;

public interface PrimaryKeyService {

    List<PrimaryKeyMetadata> listPrimaryKeys(DbContext db, String tableName);
}
