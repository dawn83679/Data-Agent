package edu.zsc.ai.domain.service.db;

import edu.zsc.ai.domain.model.dto.request.db.AgentExecuteSqlRequest;
import edu.zsc.ai.domain.model.dto.response.db.ExecuteSqlResponse;

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
}
