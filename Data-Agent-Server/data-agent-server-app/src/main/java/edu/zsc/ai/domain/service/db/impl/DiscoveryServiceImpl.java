package edu.zsc.ai.domain.service.db.impl;

import edu.zsc.ai.agent.tool.sql.model.NamedObjectDetail;
import edu.zsc.ai.agent.tool.sql.model.ObjectDetail;
import edu.zsc.ai.agent.tool.sql.model.ObjectQueryItem;
import edu.zsc.ai.agent.tool.sql.model.ObjectSearchResponse;
import edu.zsc.ai.agent.tool.sql.model.ObjectSearchResult;
import edu.zsc.ai.agent.guard.ExplorerConnectionScopeGuard;
import edu.zsc.ai.context.AgentRequestContext;
import edu.zsc.ai.context.AgentRequestContextInfo;
import edu.zsc.ai.context.RequestContext;
import edu.zsc.ai.context.RequestContextInfo;
import edu.zsc.ai.util.ConnectionIdUtil;
import edu.zsc.ai.domain.model.context.DbContext;
import edu.zsc.ai.domain.model.dto.response.db.ConnectionResponse;
import edu.zsc.ai.domain.service.db.ConnectionAccessService;
import edu.zsc.ai.domain.service.db.DatabaseObjectService;
import edu.zsc.ai.domain.service.db.DatabaseService;
import edu.zsc.ai.domain.service.db.DbConnectionService;
import edu.zsc.ai.domain.service.db.DiscoveryService;
import edu.zsc.ai.domain.service.db.IndexService;
import edu.zsc.ai.domain.service.db.SchemaService;
import edu.zsc.ai.plugin.constant.DatabaseObjectTypeEnum;
import edu.zsc.ai.plugin.model.metadata.IndexMetadata;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.regex.Pattern;

import static edu.zsc.ai.config.ExecutorConfig.SHARED_EXECUTOR_BEAN_NAME;

@Slf4j
@Service
public class DiscoveryServiceImpl implements DiscoveryService {

    // TODO OOM risk: SEARCH_RESULT_LIMIT only truncates the final result count returned to Tool,
    //  but databaseObjectService.searchObjects() may load a large dataset into memory at once.
    //  Need to add LIMIT in plugin-layer SQL queries to control memory usage at source.
    private static final int SEARCH_RESULT_LIMIT = 100;

    private static final EnumSet<DatabaseObjectTypeEnum> ROW_COUNT_TYPES = EnumSet.of(
            DatabaseObjectTypeEnum.TABLE,
            DatabaseObjectTypeEnum.VIEW
    );

    private static final List<DatabaseObjectTypeEnum> DEFAULT_SEARCH_TYPES = List.of(
            DatabaseObjectTypeEnum.TABLE,
            DatabaseObjectTypeEnum.VIEW
    );

    private final Executor sharedExecutor;
    private final DbConnectionService dbConnectionService;
    private final DatabaseService databaseService;
    private final SchemaService schemaService;
    private final DatabaseObjectService databaseObjectService;
    private final IndexService indexService;
    private final ConnectionAccessService connectionAccessService;

    public DiscoveryServiceImpl(
            @Qualifier(SHARED_EXECUTOR_BEAN_NAME) Executor sharedExecutor,
            DbConnectionService dbConnectionService,
            DatabaseService databaseService,
            SchemaService schemaService,
            DatabaseObjectService databaseObjectService,
            IndexService indexService,
            ConnectionAccessService connectionAccessService) {
        this.sharedExecutor = sharedExecutor;
        this.dbConnectionService = dbConnectionService;
        this.databaseService = databaseService;
        this.schemaService = schemaService;
        this.databaseObjectService = databaseObjectService;
        this.indexService = indexService;
        this.connectionAccessService = connectionAccessService;
    }

    // ==================== searchObjects ====================

    @Override
    public ObjectSearchResponse searchObjects(String pattern, DatabaseObjectTypeEnum type,
                                              Long connectionId, String databaseNamePattern,
                                              String schemaNamePattern) {
        List<ConnectionResponse> connections = resolveConnections(connectionId);
        List<DatabaseObjectTypeEnum> typesToSearch = Objects.nonNull(type) ? List.of(type) : DEFAULT_SEARCH_TYPES;

        if (connections.isEmpty()) {
            return new ObjectSearchResponse(List.of(), 0, false, null);
        }
        if (connections.size() == 1) {
            return searchConnectionAcrossDatabases(connections.get(0), pattern, typesToSearch,
                    databaseNamePattern, schemaNamePattern);
        }

        RequestContextInfo requestContextSnapshot = RequestContext.snapshot();
        AgentRequestContextInfo agentRequestContextSnapshot = AgentRequestContext.snapshot();
        List<CompletableFuture<ObjectSearchResponse>> futures = connections.stream()
                .map(conn -> CompletableFuture.supplyAsync(() -> {
                    applyContextSnapshots(requestContextSnapshot, agentRequestContextSnapshot);
                    try {
                        return searchConnectionAcrossDatabases(conn, pattern, typesToSearch,
                                databaseNamePattern, schemaNamePattern);
                    } finally {
                        clearContextSnapshots();
                    }
                }, sharedExecutor))
                .toList();
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        List<ObjectSearchResult> merged = new ArrayList<>();
        List<String> allErrors = new ArrayList<>();
        boolean truncated = false;
        for (CompletableFuture<ObjectSearchResponse> f : futures) {
            if (merged.size() >= SEARCH_RESULT_LIMIT) {
                truncated = true;
                break;
            }
            ObjectSearchResponse r = f.join();
            merged.addAll(r.results());
            if (r.errors() != null) {
                allErrors.addAll(r.errors());
            }
            if (r.truncated()) {
                truncated = true;
            }
            if (merged.size() > SEARCH_RESULT_LIMIT) {
                merged = new ArrayList<>(merged.subList(0, SEARCH_RESULT_LIMIT));
                truncated = true;
            }
        }
        return new ObjectSearchResponse(merged, merged.size(), truncated,
                allErrors.isEmpty() ? null : allErrors);
    }

    private ObjectSearchResponse searchConnectionAcrossDatabases(ConnectionResponse conn,
                                                                 String pattern,
                                                                 List<DatabaseObjectTypeEnum> typesToSearch,
                                                                 String databaseNamePattern,
                                                                 String schemaNamePattern) {
        try {
            return searchConnectionAcrossDatabasesOrThrow(conn, pattern, typesToSearch,
                    databaseNamePattern, schemaNamePattern);
        } catch (Exception e) {
            log.warn("Search failed for connection {} ({}): {}", conn.getName(), conn.getId(), e.getMessage());
            String msg = StringUtils.defaultIfBlank(e.getMessage(), e.getClass().getSimpleName());
            return new ObjectSearchResponse(
                    List.of(), 0, false,
                    List.of("Connection '" + conn.getName() + "' (id=" + conn.getId() + "): " + msg));
        }
    }

    private ObjectSearchResponse searchConnectionAcrossDatabasesOrThrow(ConnectionResponse conn,
                                                                        String pattern,
                                                                        List<DatabaseObjectTypeEnum> typesToSearch,
                                                                        String databaseNamePattern,
                                                                        String schemaNamePattern) {
        List<ObjectSearchResult> results = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        for (String db : resolveDatabases(conn, databaseNamePattern)) {
            for (String s : resolveSchemas(conn.getId(), db, schemaNamePattern)) {
                try {
                    collectSearchResults(conn, db, s, pattern, typesToSearch, results);
                } catch (Exception e) {
                    log.warn("Search failed for {} {}/{}: {}", conn.getName(), db, s, e.getMessage());
                    errors.add("[" + conn.getName() + "] " + db + "/" + s + ": " + StringUtils.defaultIfBlank(e.getMessage(), e.getClass().getSimpleName()));
                }
                if (results.size() >= SEARCH_RESULT_LIMIT) {
                    return new ObjectSearchResponse(
                            results.subList(0, SEARCH_RESULT_LIMIT), SEARCH_RESULT_LIMIT, true,
                            errors.isEmpty() ? null : errors);
                }
            }
        }
        return new ObjectSearchResponse(results, results.size(), false, errors.isEmpty() ? null : errors);
    }

    private List<ConnectionResponse> resolveConnections(Long connectionId) {
        if (AgentRequestContext.isExplorerScope()) {
            List<Long> allowedConnectionIds = AgentRequestContext.requireAllowedConnectionIds();
            if (Objects.nonNull(connectionId)) {
                ExplorerConnectionScopeGuard.validateConnectionAllowed(connectionId);
                return List.of(dbConnectionService.getConnectionById(connectionId));
            }
            return allowedConnectionIds.stream()
                    .distinct()
                    .map(dbConnectionService::getConnectionById)
                    .toList();
        }
        if (Objects.nonNull(connectionId)) {
            return List.of(dbConnectionService.getConnectionById(connectionId));
        }
        List<ConnectionResponse> all = dbConnectionService.getAllConnections();
        return CollectionUtils.isEmpty(all) ? Collections.emptyList() : all;
    }

    private List<String> resolveDatabases(ConnectionResponse conn, String databaseNamePattern) {
        List<String> dbs = databaseService.getDatabases(conn.getId());
        if (CollectionUtils.isEmpty(dbs)) {
            return Collections.emptyList();
        }
        return filterNamesBySqlPattern(dbs, databaseNamePattern);
    }

    private List<String> resolveSchemas(Long connectionId, String database, String schemaNamePattern) {
        List<String> schemas = getSchemas(connectionId, database);
        if (CollectionUtils.isEmpty(schemas)) {
            // No schemas (e.g. MySQL) → use null as placeholder to still search the database
            return StringUtils.isBlank(schemaNamePattern) ? Collections.singletonList(null) : Collections.emptyList();
        }
        return filterNamesBySqlPattern(schemas, schemaNamePattern);
    }

    private List<String> filterNamesBySqlPattern(List<String> names, String sqlPattern) {
        if (CollectionUtils.isEmpty(names)) {
            return Collections.emptyList();
        }
        if (StringUtils.isBlank(sqlPattern)) {
            return names;
        }
        Pattern regex = Pattern.compile(toSqlLikeRegex(sqlPattern), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
        return names.stream()
                .filter(Objects::nonNull)
                .filter(name -> regex.matcher(name).matches())
                .toList();
    }

    private String toSqlLikeRegex(String sqlPattern) {
        StringBuilder regex = new StringBuilder("^");
        for (int i = 0; i < sqlPattern.length(); i++) {
            char current = sqlPattern.charAt(i);
            switch (current) {
                case '%' -> regex.append(".*");
                case '_' -> regex.append('.');
                default -> {
                    if ("\\.^$|?*+()[]{}".indexOf(current) >= 0) {
                        regex.append('\\');
                    }
                    regex.append(current);
                }
            }
        }
        regex.append('$');
        return regex.toString();
    }

    private void collectSearchResults(ConnectionResponse conn, String database, String schema,
                                      String pattern, List<DatabaseObjectTypeEnum> types,
                                      List<ObjectSearchResult> results) {
        DbContext db = new DbContext(conn.getId(), database, schema);
        for (DatabaseObjectTypeEnum searchType : types) {
            if (searchType == DatabaseObjectTypeEnum.TRIGGER) {
                continue;
            }
            List<String> names = databaseObjectService.searchObjects(searchType, pattern, db, null);
            for (String name : names) {
                results.add(new ObjectSearchResult(
                        conn.getId(), conn.getName(), conn.getDbType(),
                        database, schema, name, searchType.name()));
                if (results.size() >= SEARCH_RESULT_LIMIT) {
                    return;
                }
            }
        }
    }

    // ==================== getObjectDetail ====================

    @Override
    public ObjectDetail getObjectDetail(DatabaseObjectTypeEnum type, String objectName, DbContext db) {
        String ddl = databaseObjectService.getObjectDdl(type, objectName, db);

        Long rowCount = ROW_COUNT_TYPES.contains(type)
                ? databaseObjectService.countObjectRows(type, db, objectName)
                : null;

        List<IndexMetadata> indexes = (type == DatabaseObjectTypeEnum.TABLE)
                ? indexService.getIndexes(db, objectName)
                : null;

        return new ObjectDetail(ddl, rowCount, indexes);
    }

    // ==================== getObjectDetails (batch) ====================

    @Override
    public List<NamedObjectDetail> getObjectDetails(List<ObjectQueryItem> items) {
        List<NamedObjectDetail> results = new ArrayList<>(items.size());
        for (ObjectQueryItem item : items) {
            try {
                DatabaseObjectTypeEnum type = DatabaseObjectTypeEnum.parseQueryable(item.getObjectType());
                Long connId = ConnectionIdUtil.toLong(item.getConnectionId());
                if (connId != null) {
                    ExplorerConnectionScopeGuard.validateConnectionAllowed(connId);
                }
                if (connId == null) {
                    connId = RequestContext.getConnectionId();
                }
                ExplorerConnectionScopeGuard.validateConnectionAllowed(connId);
                if (connId != null) {
                    connectionAccessService.assertReadable(connId);
                }
                DbContext db = new DbContext(connId, item.getDatabaseName(), item.getSchemaName());
                ObjectDetail detail = getObjectDetail(type, item.getObjectName(), db);
                results.add(new NamedObjectDetail(
                        item.getObjectName(),
                        item.getObjectType(),
                        db.connectionId(),
                        db.catalog(),
                        db.schema(),
                        true,
                        null,
                        detail.ddl(),
                        detail.rowCount(),
                        detail.indexes()));
            } catch (Exception e) {
                log.warn("Batch getObjectDetail failed for {} '{}': {}",
                        item.getObjectType(), item.getObjectName(), e.getMessage());
                String errorMsg = StringUtils.defaultIfBlank(e.getMessage(), e.getClass().getSimpleName());
                results.add(new NamedObjectDetail(
                        item.getObjectName(),
                        item.getObjectType(),
                        item.getConnectionId(),
                        item.getDatabaseName(),
                        item.getSchemaName(),
                        false,
                        errorMsg,
                        null,
                        null,
                        List.of()));
            }
        }
        return results;
    }

    // ==================== helpers ====================

    private List<String> getSchemas(Long connectionId, String catalog) {
        List<String> schemas = schemaService.listSchemas(connectionId, catalog);
        return CollectionUtils.isEmpty(schemas) ? Collections.emptyList() : schemas;
    }

    private void applyContextSnapshots(RequestContextInfo requestContextSnapshot,
                                       AgentRequestContextInfo agentRequestContextSnapshot) {
        if (requestContextSnapshot != null) {
            RequestContext.set(requestContextSnapshot);
        } else {
            RequestContext.clear();
        }
        if (agentRequestContextSnapshot != null) {
            AgentRequestContext.set(agentRequestContextSnapshot);
        } else {
            AgentRequestContext.clear();
        }
    }

    private void clearContextSnapshots() {
        AgentRequestContext.clear();
        RequestContext.clear();
    }
}
