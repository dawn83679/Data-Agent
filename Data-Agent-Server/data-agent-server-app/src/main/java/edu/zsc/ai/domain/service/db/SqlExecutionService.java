package edu.zsc.ai.domain.service.db;

import edu.zsc.ai.domain.model.dto.request.db.AgentExecuteSqlRequest;
import edu.zsc.ai.domain.model.dto.response.db.ExecuteSqlResponse;

import java.util.List;

/**
 * Service for executing SQL on a user-owned connection.
 */
public interface SqlExecutionService {

    /**
     * Execute SQL in the context of the given request (connection, database, schema, user).
     *
     * @param request execution context (with userId) and SQL
     * @return execution result (query result set or DML affected rows, or error info)
     */
    ExecuteSqlResponse executeSql(AgentExecuteSqlRequest request);

    /**
     * Execute multiple SQL statements in batch, opening the connection once.
     * Each statement is executed independently — a failure in one does not affect the others.
     *
     * @param connectionId target connection
     * @param databaseName target database (catalog)
     * @param schemaName   target schema (nullable)
     * @param userId       owner user
     * @param sqls         list of SQL statements to execute
     * @return one response per statement, in the same order as input
     */
    List<ExecuteSqlResponse> executeBatchSql(Long connectionId, String databaseName,
                                             String schemaName, Long userId, List<String> sqls);
}
