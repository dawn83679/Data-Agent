package edu.zsc.ai.domain.service.db;

import edu.zsc.ai.domain.model.context.DbContext;
import edu.zsc.ai.plugin.constant.DatabaseObjectTypeEnum;

import java.util.List;

public interface DatabaseObjectService {

    List<String> getObjectNames(DatabaseObjectTypeEnum objectType, DbContext db, String tableName);

    List<String> searchObjects(DatabaseObjectTypeEnum objectType, String namePattern,
                               DbContext db, String tableName);

    long countObjects(DatabaseObjectTypeEnum objectType, String namePattern,
                      DbContext db, String tableName);

    long countObjectRows(DatabaseObjectTypeEnum objectType, DbContext db, String objectName);

    String getObjectDdl(DatabaseObjectTypeEnum objectType, String objectName, DbContext db);
}
