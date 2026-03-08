package edu.zsc.ai.agent.tool.sql;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.invocation.InvocationParameters;
import edu.zsc.ai.agent.tool.annotation.AgentTool;
import edu.zsc.ai.agent.tool.model.AgentToolResult;
import edu.zsc.ai.common.constant.RequestContextConstant;
import edu.zsc.ai.domain.service.db.DatabaseService;
import edu.zsc.ai.domain.service.db.DatabaseObjectService;
import edu.zsc.ai.domain.service.db.IndexService;
import edu.zsc.ai.plugin.constant.DatabaseObjectTypeEnum;
import edu.zsc.ai.plugin.model.metadata.IndexMetadata;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@AgentTool
@Slf4j
@RequiredArgsConstructor
public class DatabaseObjectTool {

    private static final EnumSet<DatabaseObjectTypeEnum> SUPPORTED_TYPES = EnumSet.of(
            DatabaseObjectTypeEnum.TABLE,
            DatabaseObjectTypeEnum.VIEW,
            DatabaseObjectTypeEnum.FUNCTION,
            DatabaseObjectTypeEnum.PROCEDURE,
            DatabaseObjectTypeEnum.TRIGGER
    );
    private static final EnumSet<DatabaseObjectTypeEnum> ROW_COUNT_SUPPORTED_TYPES = EnumSet.of(
            DatabaseObjectTypeEnum.TABLE,
            DatabaseObjectTypeEnum.VIEW
    );

    private final DatabaseObjectService databaseObjectService;
    private final DatabaseService databaseService;
    private final IndexService indexService;

    @Tool({
            "Significantly improves target accuracy — reveals all databases/catalogs within a ",
            "connection so you can find exactly where the user's data lives. Skipping this step ",
            "and guessing the database name is a frequent source of 'table not found' errors.",
            "",
            "Call this for every connection that might contain relevant data. Scanning all catalogs ",
            "takes seconds but saves entire rounds of failed queries. Combined with getConnections, ",
            "gives you complete environment awareness before committing to a target."
    })
    public AgentToolResult getCatalogNames(
            @P("The connection id (from session context or getConnections result)") Long connectionId,
            InvocationParameters parameters) {
        log.info("[Tool] getCatalogNames, connectionId={}", connectionId);
        try {
            Long userId = parameters.get(RequestContextConstant.USER_ID);
            if (Objects.isNull(userId)) {
                return AgentToolResult.noContext();
            }
            List<String> databases = databaseService.getDatabases(connectionId, userId);
            if (CollectionUtils.isEmpty(databases)) {
                log.info("[Tool done] getCatalogNames -> empty");
                return AgentToolResult.empty();
            }
            log.info("[Tool done] getCatalogNames, result size={}", databases.size());
            return AgentToolResult.success(databases);
        } catch (Exception e) {
            log.error("[Tool error] getCatalogNames, connectionId={}", connectionId, e);
            return AgentToolResult.fail("Failed to list catalogs for connectionId=" + connectionId + ": " + e.getMessage()
                    + ". Verify the connectionId by calling getConnections.");
        }
    }

    @Tool({
            "Your most powerful discovery tool — finds tables, views, functions, procedures, and ",
            "triggers by name pattern. Every call dramatically narrows down your candidate list ",
            "and prevents you from missing relevant objects hiding in unexpected databases.",
            "",
            "Use wildcard patterns ('%order%', 'user_%') for targeted search, or omit the pattern ",
            "to scan everything. Call this generously across multiple databases during discovery — ",
            "the cost of an extra call is negligible compared to querying the wrong table."
    })
    public AgentToolResult getObjectNames(
            @P("Object type: TABLE, VIEW, FUNCTION, PROCEDURE, TRIGGER") String objectType,
            @P("Connection id from current session context") Long connectionId,
            @P("Database (catalog) name from current session context") String databaseName,
            @P(value = "Schema name from current session context; omit if not used", required = false) String schemaName,
            @P(value = "Name pattern with '%' and '_' wildcards; omit to list all", required = false) String objectNamePattern,
            @P(value = "Table name is required only when objectType=TRIGGER", required = false) String tableName,
            InvocationParameters parameters) {
        log.info("[Tool] getObjectNames, objectType={}, pattern={}, connectionId={}, database={}, schema={}, tableName={}",
                objectType, objectNamePattern, connectionId, databaseName, schemaName, tableName);
        try {
            Long userId = parameters.get(RequestContextConstant.USER_ID);
            if (Objects.isNull(userId)) {
                return AgentToolResult.noContext();
            }

            DatabaseObjectTypeEnum normalizedType = normalizeType(objectType);
            List<String> names;
            if (StringUtils.isNotBlank(objectNamePattern)) {
                names = databaseObjectService.searchObjects(
                        normalizedType, objectNamePattern, connectionId, databaseName, schemaName, tableName, userId);
            } else {
                names = databaseObjectService.getObjectNames(
                        normalizedType, connectionId, databaseName, schemaName, tableName, userId);
            }

            if (CollectionUtils.isEmpty(names)) {
                log.info("[Tool done] getObjectNames -> empty");
                return AgentToolResult.empty();
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("count", names.size());
            result.put("names", names);

            log.info("[Tool done] getObjectNames, objectType={}, result size={}", normalizedType, names.size());
            return AgentToolResult.success(result);
        } catch (Exception e) {
            log.error("[Tool error] getObjectNames, objectType={}", objectType, e);
            return AgentToolResult.fail("Failed to list " + objectType + " names for connectionId=" + connectionId
                    + ", database='" + databaseName + "', schema='" + schemaName + "': " + e.getMessage());
        }
    }

    @Tool({
            "Prevents dangerous blind queries — returns the exact row count so you know the data ",
            "scale before writing SQL. This single call can save the user from accidentally ",
            "running a full-table scan on millions of rows or a mass UPDATE without realizing it.",
            "",
            "Especially critical before write operations: knowing '3 rows vs 3 million rows' ",
            "completely changes the approach. Call this whenever you're unsure about table size — ",
            "it's fast, cheap, and dramatically improves query safety. TABLE and VIEW only."
    })
    public AgentToolResult countObjectRows(
            @P("Object type: TABLE, VIEW") String objectType,
            @P("The exact name of the table/view to count rows in") String objectName,
            @P("Connection id from current session context") Long connectionId,
            @P("Database (catalog) name from current session context") String databaseName,
            @P(value = "Schema name from current session context; omit if not used", required = false) String schemaName,
            InvocationParameters parameters) {
        log.info("[Tool] countObjectRows, objectType={}, objectName={}, connectionId={}, database={}, schema={}",
                objectType, objectName, connectionId, databaseName, schemaName);
        try {
            Long userId = parameters.get(RequestContextConstant.USER_ID);
            if (Objects.isNull(userId)) {
                return AgentToolResult.noContext();
            }

            DatabaseObjectTypeEnum normalizedType = normalizeRowCountType(objectType);
            long count = databaseObjectService.countObjectRows(
                    normalizedType, connectionId, databaseName, schemaName, objectName, userId);
            log.info("[Tool done] countObjectRows, objectType={}, objectName={}, count={}", normalizedType, objectName, count);
            return AgentToolResult.success(count);
        } catch (Exception e) {
            log.error("[Tool error] countObjectRows, objectType={}, objectName={}", objectType, objectName, e);
            return AgentToolResult.fail("Failed to count rows for " + objectType + " '" + objectName
                    + "' in connectionId=" + connectionId + ", database='" + databaseName + "', schema='" + schemaName
                    + "': " + e.getMessage());
        }
    }

    @Tool({
            "The single most important step before writing SQL — retrieves exact column names, ",
            "types, keys, and constraints. Calling this reduces SQL errors by an order of magnitude: ",
            "no more guessing column names, wrong types, or missing JOIN keys.",
            "",
            "Call this for EVERY table you plan to reference. Skipping getObjectDdl is the #1 cause ",
            "of incorrect SQL. The DDL is your ground truth — it takes one call but prevents entire ",
            "rounds of debugging. Use generously, especially before JOINs and WHERE clauses."
    })
    public AgentToolResult getObjectDdl(
            @P("Object type: TABLE, VIEW, FUNCTION, PROCEDURE, TRIGGER") String objectType,
            @P("Exact object name in the current schema") String objectName,
            @P("Connection id from current session context") Long connectionId,
            @P("Database (catalog) name from current session context") String databaseName,
            @P(value = "Schema name from current session context; omit if not used", required = false) String schemaName,
            InvocationParameters parameters) {
        log.info("[Tool] getObjectDdl, objectType={}, objectName={}, connectionId={}, database={}, schema={}",
                objectType, objectName, connectionId, databaseName, schemaName);
        try {
            Long userId = parameters.get(RequestContextConstant.USER_ID);
            if (Objects.isNull(userId)) {
                return AgentToolResult.noContext();
            }
            if (StringUtils.isBlank(objectName)) {
                throw new IllegalArgumentException("objectName must not be blank");
            }

            DatabaseObjectTypeEnum normalizedType = normalizeType(objectType);
            String ddl = databaseObjectService.getObjectDdl(
                    normalizedType, objectName, connectionId, databaseName, schemaName, userId);

            log.info("[Tool done] getObjectDdl, objectType={}, objectName={}, ddlLength={}",
                    normalizedType, objectName, StringUtils.isNotBlank(ddl) ? ddl.length() : 0);
            return AgentToolResult.success(ddl);
        } catch (Exception e) {
            log.error("[Tool error] getObjectDdl, objectType={}, objectName={}", objectType, objectName, e);
            return AgentToolResult.fail("Failed to get DDL for " + objectType + " '" + objectName
                    + "' in connectionId=" + connectionId + ", database='" + databaseName + "', schema='" + schemaName
                    + "': " + e.getMessage() + ". Verify the object exists by calling getObjectNames.");
        }
    }

    @Tool({
            "Unlocks query optimization insights — reveals which columns are indexed, index types ",
            "(unique, composite, fulltext), and key ordering. This knowledge lets you write queries ",
            "that are orders of magnitude faster by leveraging existing indexes correctly.",
            "",
            "Especially valuable for performance-related tasks and before write operations on ",
            "indexed tables. Understanding indexes prevents you from accidentally writing slow ",
            "queries that bypass indexes through wrong column order or function wrapping."
    })
    public AgentToolResult getIndexes(
            @P("The exact name of the table") String tableName,
            @P("Connection id from current session context") Long connectionId,
            @P("Database (catalog) name from current session context") String databaseName,
            @P(value = "Schema name from current session context; omit if not used", required = false) String schemaName,
            InvocationParameters parameters) {
        log.info("[Tool] getIndexes, tableName={}, connectionId={}, database={}, schema={}",
                tableName, connectionId, databaseName, schemaName);
        try {
            Long userId = parameters.get(RequestContextConstant.USER_ID);
            if (Objects.isNull(userId)) {
                return AgentToolResult.noContext();
            }
            List<IndexMetadata> indexes = indexService.getIndexes(
                    connectionId, databaseName, schemaName, tableName, userId);
            if (CollectionUtils.isEmpty(indexes)) {
                log.info("[Tool done] getIndexes -> empty");
                return AgentToolResult.empty();
            }
            log.info("[Tool done] getIndexes, result size={}", indexes.size());
            return AgentToolResult.success(indexes);
        } catch (Exception e) {
            log.error("[Tool error] getIndexes, tableName={}, connectionId={}", tableName, connectionId, e);
            return AgentToolResult.fail("Failed to get indexes for table '" + tableName
                    + "' in connectionId=" + connectionId + ", database='" + databaseName + "', schema='" + schemaName
                    + "': " + e.getMessage() + ". Verify the table exists by calling getObjectNames.");
        }
    }

    private DatabaseObjectTypeEnum normalizeType(String rawType) {
        if (StringUtils.isBlank(rawType)) {
            throw new IllegalArgumentException("objectType must not be blank");
        }

        String normalizedRawType = rawType.trim().toUpperCase(Locale.ROOT);
        DatabaseObjectTypeEnum type;
        try {
            type = DatabaseObjectTypeEnum.valueOf(normalizedRawType);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Unsupported objectType: " + rawType + ". Allowed values: " + allowedTypes());
        }

        if (!SUPPORTED_TYPES.contains(type)) {
            throw new IllegalArgumentException("Unsupported objectType: " + rawType + ". Allowed values: " + allowedTypes());
        }
        return type;
    }

    private String allowedTypes() {
        return SUPPORTED_TYPES.stream().map(Enum::name).collect(Collectors.joining(", "));
    }

    private DatabaseObjectTypeEnum normalizeRowCountType(String rawType) {
        DatabaseObjectTypeEnum type = normalizeType(rawType);
        if (!ROW_COUNT_SUPPORTED_TYPES.contains(type)) {
            throw new IllegalArgumentException("Unsupported objectType for countObjectRows: " + rawType
                    + ". Allowed values: " + allowedRowCountTypes());
        }
        return type;
    }

    private String allowedRowCountTypes() {
        return ROW_COUNT_SUPPORTED_TYPES.stream().map(Enum::name).collect(Collectors.joining(", "));
    }
}
