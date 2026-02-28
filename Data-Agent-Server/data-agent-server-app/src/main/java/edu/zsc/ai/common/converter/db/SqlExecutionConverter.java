package edu.zsc.ai.common.converter.db;

import edu.zsc.ai.domain.model.dto.response.db.ExecuteSqlColumn;
import edu.zsc.ai.domain.model.dto.response.db.ExecuteSqlExecutionInfo;
import edu.zsc.ai.domain.model.dto.response.db.ExecuteSqlMessage;
import edu.zsc.ai.domain.model.dto.response.db.ExecuteSqlMessageLevel;
import edu.zsc.ai.domain.model.dto.response.db.ExecuteSqlResponse;
import edu.zsc.ai.domain.model.dto.response.db.ExecuteSqlResponseType;
import edu.zsc.ai.domain.model.dto.response.db.ExecuteSqlResultSet;
import edu.zsc.ai.domain.model.dto.response.db.ExecuteSqlSubResult;
import edu.zsc.ai.plugin.model.command.sql.SqlCommandResult;
import edu.zsc.ai.plugin.model.command.sql.SqlMessageInfo;
import edu.zsc.ai.plugin.model.command.sql.SqlMessageLevel;
import edu.zsc.ai.plugin.model.command.sql.SqlCommandSubResult;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class SqlExecutionConverter {

    private SqlExecutionConverter() {
    }

    /**
     * Convert plugin SqlCommandResult to ExecuteSqlResponse DTO.
     */
    public static ExecuteSqlResponse toResponse(SqlCommandResult r) {
        if (r == null) {
            return null;
        }
        ExecuteSqlResponseType type = resolveType(r);
        ExecuteSqlResultSet resultSet = buildResultSet(r);
        ExecuteSqlExecutionInfo executionInfo = buildExecutionInfo(r);
        List<ExecuteSqlMessage> messages = buildMessages(r);
        List<ExecuteSqlSubResult> results = buildSubResults(r);
        return ExecuteSqlResponse.builder()
                .success(r.isSuccess())
                .errorMessage(r.getErrorMessage())
                .executionTimeMs(r.getExecutionTime())
                .originalSql(r.getOriginalSql())
                .executedSql(r.getExecutedSql())
                .query(r.isQuery())
                .headers(r.getHeaders())
                .rows(r.getRows())
                .affectedRows(r.getAffectedRows())
                .type(type)
                .resultSet(resultSet)
                .executionInfo(executionInfo)
                .messages(messages)
                .results(results)
                .build();
    }

    private static ExecuteSqlResponseType resolveType(SqlCommandResult r) {
        if (!r.isSuccess()) {
            return ExecuteSqlResponseType.ERROR;
        }
        return r.isQuery() ? ExecuteSqlResponseType.QUERY : ExecuteSqlResponseType.UPDATE;
    }

    private static ExecuteSqlResultSet buildResultSet(SqlCommandResult r) {
        if (!r.isQuery()) {
            return null;
        }
        List<ExecuteSqlColumn> columns = null;
        if (r.getColumns() != null) {
            columns = r.getColumns()
                    .stream()
                    .map(col -> ExecuteSqlColumn.builder()
                            .name(col.getName())
                            .label(col.getLabel())
                            .typeName(col.getTypeName())
                            .jdbcType(col.getJdbcType())
                            .precision(col.getPrecision())
                            .scale(col.getScale())
                            .nullable(col.getNullable())
                            .tableName(col.getTableName())
                            .build())
                    .collect(Collectors.toList());
        } else if (r.getHeaders() != null) {
            columns = r.getHeaders()
                    .stream()
                    .map(header -> ExecuteSqlColumn.builder()
                            .name(header)
                            .label(header)
                            .build())
                    .collect(Collectors.toList());
        }
        Integer fetchRows = r.getRows() == null ? null : r.getRows().size();
        return ExecuteSqlResultSet.builder()
                .columns(columns)
                .rows(r.getRows())
                .fetchRows(fetchRows)
                .truncated(r.getTruncated())
                .build();
    }

    private static ExecuteSqlResultSet buildResultSet(SqlCommandSubResult r) {
        if (r == null || !r.isQuery()) {
            return null;
        }
        List<ExecuteSqlColumn> columns = null;
        if (r.getColumns() != null) {
            columns = r.getColumns()
                    .stream()
                    .map(col -> ExecuteSqlColumn.builder()
                            .name(col.getName())
                            .label(col.getLabel())
                            .typeName(col.getTypeName())
                            .jdbcType(col.getJdbcType())
                            .precision(col.getPrecision())
                            .scale(col.getScale())
                            .nullable(col.getNullable())
                            .tableName(col.getTableName())
                            .build())
                    .collect(Collectors.toList());
        } else if (r.getHeaders() != null) {
            columns = r.getHeaders()
                    .stream()
                    .map(header -> ExecuteSqlColumn.builder()
                            .name(header)
                            .label(header)
                            .build())
                    .collect(Collectors.toList());
        }
        Integer fetchRows = r.getRows() == null ? null : r.getRows().size();
        return ExecuteSqlResultSet.builder()
                .columns(columns)
                .rows(r.getRows())
                .fetchRows(fetchRows)
                .truncated(r.getTruncated())
                .build();
    }

    private static List<ExecuteSqlSubResult> buildSubResults(SqlCommandResult r) {
        if (r.getResults() == null || r.getResults().isEmpty()) {
            return Collections.emptyList();
        }
        return r.getResults()
                .stream()
                .map(SqlExecutionConverter::mapSubResult)
                .collect(Collectors.toList());
    }

    private static ExecuteSqlSubResult mapSubResult(SqlCommandSubResult sub) {
        ExecuteSqlResponseType type = sub.isQuery() ? ExecuteSqlResponseType.QUERY : ExecuteSqlResponseType.UPDATE;
        ExecuteSqlResultSet resultSet = buildResultSet(sub);
        List<ExecuteSqlMessage> messages = buildMessages(sub);
        return ExecuteSqlSubResult.builder()
                .type(type)
                .resultSet(resultSet)
                .headers(sub.getHeaders())
                .rows(sub.getRows())
                .affectedRows(sub.getAffectedRows())
                .messages(messages)
                .build();
    }

    private static ExecuteSqlExecutionInfo buildExecutionInfo(SqlCommandResult r) {
        return ExecuteSqlExecutionInfo.builder()
                .executionId(null)
                .startTime(r.getStartTime())
                .endTime(r.getEndTime())
                .durationMs(r.getExecutionTime())
                .executionMs(r.getExecutionMs())
                .fetchingMs(r.getFetchingMs())
                .affectedRows(r.getAffectedRows())
                .fetchRows(r.getFetchRows())
                .truncated(r.getTruncated())
                .limitApplied(r.getLimitApplied())
                .build();
    }

    private static List<ExecuteSqlMessage> buildMessages(SqlCommandResult r) {
        List<ExecuteSqlMessage> messages;
        if (r.getMessages() != null && !r.getMessages().isEmpty()) {
            messages = r.getMessages()
                    .stream()
                    .map(SqlExecutionConverter::mapMessage)
                    .collect(Collectors.toList());
        } else if (r.getErrorMessage() == null || r.getErrorMessage().isBlank()) {
            messages = new java.util.ArrayList<>();
        } else {
            ExecuteSqlMessage message = ExecuteSqlMessage.builder()
                    .level(ExecuteSqlMessageLevel.ERROR)
                    .code(r.getErrorCode() == null ? null : String.valueOf(r.getErrorCode()))
                    .sqlState(r.getSqlState())
                    .message(r.getErrorMessage())
                    .detail(r.getErrorDetail())
                    .build();
            messages = new java.util.ArrayList<>(Collections.singletonList(message));
        }
        if (r.isSuccess()) {
            ExecuteSqlMessage summary = buildSummaryMessage(r);
            if (summary != null) {
                messages.add(summary);
            }
        }
        return messages;
    }

    private static List<ExecuteSqlMessage> buildMessages(SqlCommandSubResult r) {
        if (r == null || r.getMessages() == null || r.getMessages().isEmpty()) {
            return Collections.emptyList();
        }
        return r.getMessages()
                .stream()
                .map(SqlExecutionConverter::mapMessage)
                .collect(Collectors.toList());
    }

    private static ExecuteSqlMessage mapMessage(SqlMessageInfo info) {
        return ExecuteSqlMessage.builder()
                .level(mapLevel(info.getLevel()))
                .code(info.getCode())
                .sqlState(info.getSqlState())
                .message(info.getMessage())
                .detail(info.getDetail())
                .build();
    }

    private static ExecuteSqlMessageLevel mapLevel(SqlMessageLevel level) {
        if (level == null) {
            return ExecuteSqlMessageLevel.INFO;
        }
        if (level == SqlMessageLevel.WARN) {
            return ExecuteSqlMessageLevel.WARN;
        }
        if (level == SqlMessageLevel.ERROR) {
            return ExecuteSqlMessageLevel.ERROR;
        }
        return ExecuteSqlMessageLevel.INFO;
    }

    private static ExecuteSqlMessage buildSummaryMessage(SqlCommandResult r) {
        ExecuteSqlResponseType type = resolveType(r);
        String message;
        if (type == ExecuteSqlResponseType.QUERY) {
            Integer fetchRows = r.getFetchRows() != null ? r.getFetchRows()
                    : (r.getRows() == null ? 0 : r.getRows().size());
            message = String.format("Query OK, fetched %d rows in %d ms", fetchRows, r.getExecutionTime());
        } else if (type == ExecuteSqlResponseType.UPDATE) {
            message = String.format("Update OK, affected %d rows in %d ms", r.getAffectedRows(),
                    r.getExecutionTime());
        } else {
            return null;
        }
        return ExecuteSqlMessage.builder()
                .level(ExecuteSqlMessageLevel.INFO)
                .message(message)
                .build();
    }
}
