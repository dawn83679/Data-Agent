package edu.zsc.ai.domain.service.db.impl;

import edu.zsc.ai.agent.tool.sql.model.CatalogInfo;
import edu.zsc.ai.agent.tool.sql.model.ConnectionOverview;
import edu.zsc.ai.agent.tool.sql.model.NamedObjectDetail;
import edu.zsc.ai.agent.tool.sql.model.ObjectDetail;
import edu.zsc.ai.agent.tool.sql.model.ObjectQueryItem;
import edu.zsc.ai.agent.tool.sql.model.ObjectSearchResponse;
import edu.zsc.ai.agent.tool.sql.model.ObjectSearchResult;
import edu.zsc.ai.domain.model.dto.response.db.ConnectionResponse;
import edu.zsc.ai.domain.service.db.DatabaseObjectService;
import edu.zsc.ai.domain.service.db.DatabaseService;
import edu.zsc.ai.domain.service.db.DbConnectionService;
import edu.zsc.ai.domain.service.db.DiscoveryService;
import edu.zsc.ai.domain.service.db.IndexService;
import edu.zsc.ai.domain.service.db.SchemaService;
import edu.zsc.ai.plugin.constant.DatabaseObjectTypeEnum;
import edu.zsc.ai.plugin.model.metadata.IndexMetadata;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class DiscoveryServiceImpl implements DiscoveryService {

    // TODO OOM 风险：SEARCH_RESULT_LIMIT 只截断了最终返回给 Tool 的结果数量，
    //  但底层 databaseObjectService.searchObjects() 可能一次从数据库查出大量数据加载到内存。
    //  后续需要在 plugin 层的 SQL 查询中加 LIMIT，从源头控制内存占用。
    private static final int SEARCH_RESULT_LIMIT = 100;

    private static final EnumSet<DatabaseObjectTypeEnum> ROW_COUNT_TYPES = EnumSet.of(
            DatabaseObjectTypeEnum.TABLE,
            DatabaseObjectTypeEnum.VIEW
    );

    private static final List<DatabaseObjectTypeEnum> DEFAULT_SEARCH_TYPES = List.of(
            DatabaseObjectTypeEnum.TABLE,
            DatabaseObjectTypeEnum.VIEW
    );

    private final DbConnectionService dbConnectionService;
    private final DatabaseService databaseService;
    private final SchemaService schemaService;
    private final DatabaseObjectService databaseObjectService;
    private final IndexService indexService;

    // ==================== getEnvironmentOverview ====================

    @Override
    public List<ConnectionOverview> getEnvironmentOverview(Long userId) {
        List<ConnectionResponse> connections = dbConnectionService.getAllConnections(userId);
        if (CollectionUtils.isEmpty(connections)) {
            return Collections.emptyList();
        }
        return connections.stream().map(conn -> buildConnectionOverview(conn, userId)).toList();
    }

    private ConnectionOverview buildConnectionOverview(ConnectionResponse conn, Long userId) {
        List<CatalogInfo> catalogs;
        try {
            List<String> databases = databaseService.getDatabases(conn.getId(), userId);
            catalogs = databases.stream()
                    .map(db -> new CatalogInfo(db, getSchemas(conn.getId(), db)))
                    .toList();
        } catch (Exception e) {
            log.warn("Failed to get catalogs for connection {} ({}): {}",
                    conn.getName(), conn.getId(), e.getMessage());
            catalogs = Collections.emptyList();
        }
        return new ConnectionOverview(conn.getId(), conn.getName(), conn.getDbType(), catalogs);
    }

    // ==================== searchObjects ====================

    @Override
    public ObjectSearchResponse searchObjects(String pattern, DatabaseObjectTypeEnum type,
                                              Long connectionId, String databaseName,
                                              String schemaName, Long userId) {
        List<ConnectionResponse> connections = resolveConnections(connectionId, userId);
        List<DatabaseObjectTypeEnum> typesToSearch = Objects.nonNull(type) ? List.of(type) : DEFAULT_SEARCH_TYPES;
        List<ObjectSearchResult> results = new ArrayList<>();

        for (ConnectionResponse conn : connections) {
            for (String db : resolveDatabases(conn, databaseName, userId)) {
                for (String schema : resolveSchemas(conn.getId(), db, schemaName)) {
                    collectSearchResults(conn, db, schema, pattern, typesToSearch, userId, results);
                    if (results.size() >= SEARCH_RESULT_LIMIT) {
                        List<ObjectSearchResult> truncated = results.subList(0, SEARCH_RESULT_LIMIT);
                        return new ObjectSearchResponse(truncated, truncated.size(), true);
                    }
                }
            }
        }
        return new ObjectSearchResponse(results, results.size(), false);
    }

    private List<ConnectionResponse> resolveConnections(Long connectionId, Long userId) {
        if (Objects.nonNull(connectionId)) {
            return List.of(dbConnectionService.getConnectionById(connectionId, userId));
        }
        List<ConnectionResponse> all = dbConnectionService.getAllConnections(userId);
        return CollectionUtils.isEmpty(all) ? Collections.emptyList() : all;
    }

    private List<String> resolveDatabases(ConnectionResponse conn, String databaseName, Long userId) {
        if (StringUtils.isNotBlank(databaseName)) {
            return List.of(databaseName);
        }
        try {
            List<String> dbs = databaseService.getDatabases(conn.getId(), userId);
            return CollectionUtils.isEmpty(dbs) ? Collections.emptyList() : dbs;
        } catch (Exception e) {
            log.warn("Failed to list databases for connection {} ({}): {}",
                    conn.getName(), conn.getId(), e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<String> resolveSchemas(Long connectionId, String database, String schemaName) {
        if (StringUtils.isNotBlank(schemaName)) {
            return List.of(schemaName);
        }
        List<String> schemas = getSchemas(connectionId, database);
        // No schemas (e.g. MySQL) → use null as placeholder to still search the database
        return CollectionUtils.isEmpty(schemas) ? Collections.singletonList(null) : schemas;
    }

    private void collectSearchResults(ConnectionResponse conn, String database, String schema,
                                      String pattern, List<DatabaseObjectTypeEnum> types,
                                      Long userId, List<ObjectSearchResult> results) {
        for (DatabaseObjectTypeEnum searchType : types) {
            if (searchType == DatabaseObjectTypeEnum.TRIGGER) {
                continue;
            }
            try {
                List<String> names = databaseObjectService.searchObjects(
                        searchType, pattern, conn.getId(), database, schema, null, userId);
                for (String name : names) {
                    results.add(new ObjectSearchResult(
                            conn.getId(), conn.getName(), conn.getDbType(),
                            database, schema, name, searchType.name()));
                    if (results.size() >= SEARCH_RESULT_LIMIT) {
                        return;
                    }
                }
            } catch (Exception e) {
                log.warn("Search failed for {} in {}/{}/{}: {}",
                        searchType, conn.getName(), database, schema, e.getMessage());
            }
        }
    }

    // ==================== getObjectDetail ====================

    @Override
    public ObjectDetail getObjectDetail(DatabaseObjectTypeEnum type, String objectName,
                                        Long connectionId, String databaseName,
                                        String schemaName, Long userId) {
        String ddl = databaseObjectService.getObjectDdl(type, objectName, connectionId, databaseName, schemaName, userId);

        Long rowCount = ROW_COUNT_TYPES.contains(type)
                ? databaseObjectService.countObjectRows(type, connectionId, databaseName, schemaName, objectName, userId)
                : null;

        List<IndexMetadata> indexes = (type == DatabaseObjectTypeEnum.TABLE)
                ? indexService.getIndexes(connectionId, databaseName, schemaName, objectName, userId)
                : null;

        return new ObjectDetail(ddl, rowCount, indexes);
    }

    // ==================== getObjectDetails (batch) ====================

    @Override
    public List<NamedObjectDetail> getObjectDetails(List<ObjectQueryItem> items, Long userId) {
        List<NamedObjectDetail> results = new ArrayList<>(items.size());
        for (ObjectQueryItem item : items) {
            try {
                DatabaseObjectTypeEnum type = DatabaseObjectTypeEnum.parseQueryable(item.getObjectType());
                ObjectDetail detail = getObjectDetail(
                        type, item.getObjectName(), item.getConnectionId(),
                        item.getDatabaseName(), item.getSchemaName(), userId);
                results.add(new NamedObjectDetail(
                        item.getObjectName(), item.getObjectType(), true, null, detail));
            } catch (Exception e) {
                log.warn("Batch getObjectDetail failed for {} '{}': {}",
                        item.getObjectType(), item.getObjectName(), e.getMessage());
                String errorMsg = StringUtils.defaultIfBlank(e.getMessage(), e.getClass().getSimpleName());
                results.add(new NamedObjectDetail(
                        item.getObjectName(), item.getObjectType(), false, errorMsg, null));
            }
        }
        return results;
    }

    // ==================== helpers ====================

    private List<String> getSchemas(Long connectionId, String catalog) {
        try {
            List<String> schemas = schemaService.listSchemas(connectionId, catalog);
            return CollectionUtils.isEmpty(schemas) ? Collections.emptyList() : schemas;
        } catch (Exception e) {
            log.debug("Schema listing not supported for connection {}, catalog {}: {}",
                    connectionId, catalog, e.getMessage());
            return Collections.emptyList();
        }
    }
}
