package edu.zsc.ai.domain.service.db;

import edu.zsc.ai.agent.tool.sql.model.ConnectionOverview;
import edu.zsc.ai.agent.tool.sql.model.NamedObjectDetail;
import edu.zsc.ai.agent.tool.sql.model.ObjectDetail;
import edu.zsc.ai.agent.tool.sql.model.ObjectQueryItem;
import edu.zsc.ai.agent.tool.sql.model.ObjectSearchResponse;
import edu.zsc.ai.plugin.constant.DatabaseObjectTypeEnum;

import java.util.List;

public interface DiscoveryService {

    List<ConnectionOverview> getEnvironmentOverview(Long userId);

    ObjectSearchResponse searchObjects(String pattern, DatabaseObjectTypeEnum type,
                                       Long connectionId, String databaseName,
                                       String schemaName, Long userId);

    ObjectDetail getObjectDetail(DatabaseObjectTypeEnum type, String objectName,
                                 Long connectionId, String databaseName,
                                 String schemaName, Long userId);

    List<NamedObjectDetail> getObjectDetails(List<ObjectQueryItem> items, Long userId);
}
