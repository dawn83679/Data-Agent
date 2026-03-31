package edu.zsc.ai.domain.service.db;

import edu.zsc.ai.agent.tool.sql.model.NamedObjectDetail;
import edu.zsc.ai.agent.tool.sql.model.ObjectDetail;
import edu.zsc.ai.agent.tool.sql.model.ObjectQueryItem;
import edu.zsc.ai.agent.tool.sql.model.ObjectSearchResponse;
import edu.zsc.ai.domain.model.context.DbContext;
import edu.zsc.ai.plugin.constant.DatabaseObjectTypeEnum;

import java.util.List;

public interface DiscoveryService {

    ObjectSearchResponse searchObjects(String pattern, DatabaseObjectTypeEnum type,
                                       Long connectionId, String databaseNamePattern,
                                       String schemaNamePattern);

    ObjectDetail getObjectDetail(DatabaseObjectTypeEnum type, String objectName, DbContext db);

    List<NamedObjectDetail> getObjectDetails(List<ObjectQueryItem> items);
}
