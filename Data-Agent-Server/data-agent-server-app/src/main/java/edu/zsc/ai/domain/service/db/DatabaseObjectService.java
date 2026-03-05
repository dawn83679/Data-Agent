package edu.zsc.ai.domain.service.db;

import edu.zsc.ai.plugin.constant.DatabaseObjectTypeEnum;

import java.util.List;

public interface DatabaseObjectService {

    List<String> getObjectNames(DatabaseObjectTypeEnum objectType,
                                Long connectionId,
                                String catalog,
                                String schema,
                                String tableName,
                                Long userId);

    List<String> searchObjects(DatabaseObjectTypeEnum objectType,
                               String namePattern,
                               Long connectionId,
                               String catalog,
                               String schema,
                               String tableName,
                               Long userId);

    String getObjectDdl(DatabaseObjectTypeEnum objectType,
                        String objectName,
                        Long connectionId,
                        String catalog,
                        String schema,
                        Long userId);
}
