package edu.zsc.ai.agent.tool;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.invocation.InvocationParameters;
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
import java.util.List;
import java.util.Locale;
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

    private final DatabaseObjectService databaseObjectService;
    private final DatabaseService databaseService;
    private final IndexService indexService;

    @Tool({
            "[WHAT] List all database names (catalogs) available on a given connection.",
            "[WHEN] Use when the user has not specified a database, or when exploring what databases exist on a connection. Pass connectionId from session context or getMyConnections."
    })
    public AgentToolResult getCatologNames(
            @P("The connection id (from session context or getMyConnections result)") Long connectionId,
            InvocationParameters parameters) {
        log.info("[Tool] getCatologNames, connectionId={}", connectionId);
        try {
            Long userId = parameters.get(RequestContextConstant.USER_ID);
            if (Objects.isNull(userId)) {
                return AgentToolResult.noContext();
            }
            List<String> databases = databaseService.listDatabases(connectionId, userId);
            if (CollectionUtils.isEmpty(databases)) {
                log.info("[Tool done] getCatologNames -> empty");
                return AgentToolResult.empty();
            }
            log.info("[Tool done] getCatologNames, result size={}", databases.size());
            return AgentToolResult.success(databases);
        } catch (Exception e) {
            log.error("[Tool error] getCatologNames, connectionId={}", connectionId, e);
            return AgentToolResult.fail(e);
        }
    }

    @Tool({
            "[WHAT] List object names by object type in the current database/schema.",
            "[WHEN] Use this to list names for TABLE/VIEW/FUNCTION/PROCEDURE/TRIGGER.",
            "IMPORTANT — For objectType=TRIGGER, tableName is required."
    })
    public AgentToolResult getObjectNames(
            @P("Object type: TABLE, VIEW, FUNCTION, PROCEDURE, TRIGGER") String objectType,
            @P("Connection id from current session context") Long connectionId,
            @P("Database (catalog) name from current session context") String databaseName,
            @P(value = "Schema name from current session context; omit if not used", required = false) String schemaName,
            @P(value = "Table name is required only when objectType=TRIGGER", required = false) String tableName,
            InvocationParameters parameters) {
        log.info("[Tool] getObjectNames, objectType={}, connectionId={}, database={}, schema={}, tableName={}",
                objectType, connectionId, databaseName, schemaName, tableName);
        try {
            Long userId = parameters.get(RequestContextConstant.USER_ID);
            if (Objects.isNull(userId)) {
                return AgentToolResult.noContext();
            }

            DatabaseObjectTypeEnum normalizedType = normalizeType(objectType);
            List<String> names = databaseObjectService.getObjectNames(
                    normalizedType, connectionId, databaseName, schemaName, tableName, userId);
            if (CollectionUtils.isEmpty(names)) {
                log.info("[Tool done] getObjectNames -> empty");
                return AgentToolResult.empty();
            }

            log.info("[Tool done] getObjectNames, objectType={}, result size={}", normalizedType, names.size());
            return AgentToolResult.success(names);
        } catch (Exception e) {
            log.error("[Tool error] getObjectNames, objectType={}", objectType, e);
            return AgentToolResult.fail(e);
        }
    }

    @Tool({
            "[WHAT] Search database objects by JDBC-style name pattern and object type.",
            "[WHEN] Use this to search TABLE/VIEW/FUNCTION/PROCEDURE/TRIGGER names.",
            "[HOW] Supports '%' (any sequence) and '_' (single char).",
            "IMPORTANT — For objectType=TRIGGER, tableName is required."
    })
    public AgentToolResult searchObjects(
            @P("Object type: TABLE, VIEW, FUNCTION, PROCEDURE, TRIGGER") String objectType,
            @P(value = "Name pattern. Supports '%' and '_' wildcards. Pass null or '%' to list all.", required = false)
            String objectNamePattern,
            @P("Connection id from current session context") Long connectionId,
            @P("Database (catalog) name from current session context") String databaseName,
            @P(value = "Schema name from current session context; omit if not used", required = false) String schemaName,
            @P(value = "Table name is required only when objectType=TRIGGER", required = false) String tableName,
            InvocationParameters parameters) {
        log.info("[Tool] searchObjects, objectType={}, pattern={}, connectionId={}, database={}, schema={}, tableName={}",
                objectType, objectNamePattern, connectionId, databaseName, schemaName, tableName);
        try {
            Long userId = parameters.get(RequestContextConstant.USER_ID);
            if (Objects.isNull(userId)) {
                return AgentToolResult.noContext();
            }

            DatabaseObjectTypeEnum normalizedType = normalizeType(objectType);
            List<String> names = databaseObjectService.searchObjects(
                    normalizedType, objectNamePattern, connectionId, databaseName, schemaName, tableName, userId);
            if (CollectionUtils.isEmpty(names)) {
                log.info("[Tool done] searchObjects -> empty");
                return AgentToolResult.empty();
            }

            log.info("[Tool done] searchObjects, objectType={}, result size={}", normalizedType, names.size());
            return AgentToolResult.success(names);
        } catch (Exception e) {
            log.error("[Tool error] searchObjects, objectType={}", objectType, e);
            return AgentToolResult.fail(e);
        }
    }

    @Tool({
            "[WHAT] Get DDL for an object by type and name.",
            "[WHEN] Use this to inspect TABLE/VIEW/FUNCTION/PROCEDURE/TRIGGER before writing SQL.",
            "IMPORTANT — objectName must be exact in the target schema."
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
            return AgentToolResult.fail(e);
        }
    }

    @Tool({
            "[WHAT] List all indexes defined on a specific table.",
            "[WHEN] Use when the user asks about indexes, or when diagnosing query performance issues. Pass connectionId, databaseName, schemaName from current session context."
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
            log.error("[Tool error] getIndexes", e);
            return AgentToolResult.fail(e);
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
}
