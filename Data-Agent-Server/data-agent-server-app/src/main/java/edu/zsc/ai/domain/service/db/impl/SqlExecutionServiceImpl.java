package edu.zsc.ai.domain.service.db.impl;

import edu.zsc.ai.common.converter.db.SqlExecutionConverter;
import edu.zsc.ai.domain.model.context.DbContext;
import edu.zsc.ai.domain.model.dto.request.db.AgentExecuteSqlRequest;
import edu.zsc.ai.domain.model.dto.response.db.ExecuteSqlResponse;
import edu.zsc.ai.domain.service.db.ConnectionService;
import edu.zsc.ai.domain.service.db.SqlExecutionService;
import edu.zsc.ai.plugin.capability.CommandExecutor;
import edu.zsc.ai.plugin.manager.DefaultPluginManager;
import edu.zsc.ai.plugin.model.command.sql.SqlCommandRequest;
import edu.zsc.ai.plugin.model.command.sql.SqlCommandResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SqlExecutionServiceImpl implements SqlExecutionService {

    private final ConnectionService connectionService;

    @Override
    public ExecuteSqlResponse executeSql(AgentExecuteSqlRequest request) {
        DbContext db = DbContext.from(request);
        String sql = request.getSql();

        connectionService.openConnection(db);

        ConnectionManager.ActiveConnection active = ConnectionManager.getOwnedConnection(db);

        CommandExecutor<SqlCommandRequest, SqlCommandResult> executor = DefaultPluginManager.getInstance()
                .getSqlCommandExecutorByPluginId(active.pluginId());

        SqlCommandRequest pluginRequest = new SqlCommandRequest();
        pluginRequest.setConnection(active.connection());
        pluginRequest.setOriginalSql(sql);
        pluginRequest.setExecuteSql(sql);
        pluginRequest.setDatabase(db.catalog());
        pluginRequest.setSchema(db.schema());
        pluginRequest.setNeedTransaction(false);

        SqlCommandResult result = executor.executeCommand(pluginRequest);

        ExecuteSqlResponse response = SqlExecutionConverter.toResponse(result);
        if (response != null) {
            response.setDatabaseName(db.catalog());
            response.setSchemaName(db.schema());
        }
        return response;
    }

    @Override
    public List<ExecuteSqlResponse> executeBatchSql(DbContext db, List<String> sqls) {
        connectionService.openConnection(db);

        ConnectionManager.ActiveConnection active = ConnectionManager.getOwnedConnection(db);

        CommandExecutor<SqlCommandRequest, SqlCommandResult> executor = DefaultPluginManager.getInstance()
                .getSqlCommandExecutorByPluginId(active.pluginId());

        List<ExecuteSqlResponse> responses = new ArrayList<>(sqls.size());
        for (String sql : sqls) {
            try {
                SqlCommandRequest pluginRequest = new SqlCommandRequest();
                pluginRequest.setConnection(active.connection());
                pluginRequest.setOriginalSql(sql);
                pluginRequest.setExecuteSql(sql);
                pluginRequest.setDatabase(db.catalog());
                pluginRequest.setSchema(db.schema());
                pluginRequest.setNeedTransaction(false);

                SqlCommandResult result = executor.executeCommand(pluginRequest);

                ExecuteSqlResponse response = SqlExecutionConverter.toResponse(result);
                if (response != null) {
                    response.setDatabaseName(db.catalog());
                    response.setSchemaName(db.schema());
                }
                responses.add(response);
            } catch (Exception e) {
                log.warn("Batch SQL execution failed for statement [{}]: {}", sql, e.getMessage());
                ExecuteSqlResponse errorResponse = ExecuteSqlResponse.builder()
                        .success(false)
                        .errorMessage(e.getMessage())
                        .originalSql(sql)
                        .databaseName(db.catalog())
                        .schemaName(db.schema())
                        .build();
                responses.add(errorResponse);
            }
        }
        return responses;
    }
}
