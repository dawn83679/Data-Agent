package edu.zsc.ai.domain.service.db.impl;

import edu.zsc.ai.common.converter.db.SqlExecutionConverter;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class SqlExecutionServiceImpl implements SqlExecutionService {

    private final ConnectionService connectionService;

    @Override
    public ExecuteSqlResponse executeSql(AgentExecuteSqlRequest request) {
        Long connectionId = request.getConnectionId();
        String databaseName = request.getDatabaseName();
        String schemaName = request.getSchemaName();
        String sql = request.getSql();
        Long userId = request.getUserId();

        connectionService.openConnection(connectionId, databaseName, schemaName, userId);

        ConnectionManager.ActiveConnection active = ConnectionManager.getOwnedConnection(
                connectionId, databaseName, schemaName, userId);

        CommandExecutor<SqlCommandRequest, SqlCommandResult> executor = DefaultPluginManager.getInstance()
                .getSqlCommandExecutorByPluginId(active.pluginId());

        SqlCommandRequest pluginRequest = new SqlCommandRequest();
        pluginRequest.setConnection(active.connection());
        pluginRequest.setOriginalSql(sql);
        pluginRequest.setExecuteSql(sql);
        pluginRequest.setDatabase(databaseName);
        pluginRequest.setSchema(schemaName);
        pluginRequest.setNeedTransaction(false);

        SqlCommandResult result = executor.executeCommand(pluginRequest);

        return SqlExecutionConverter.toResponse(result);
    }
}
