package edu.zsc.ai.domain.service.db;

import java.util.List;

/**
 * Database Service Interface
 * Provides operations for listing databases on an active connection.
 *
 * @author Data-Agent
 * @since 0.0.1
 */
public interface DatabaseService {

    /**
     * List databases available on the data source for the given connection.
     *
     * @param connectionId unique connection identifier (from open connection)
     * @return list of database names, never null
     * @throws edu.zsc.ai.util.exception.BusinessException if connection not found or plugin does not support DatabaseProvider
     */
    List<String> listDatabases(String connectionId);
}
