package edu.zsc.ai.domain.service.db;

import edu.zsc.ai.plugin.capability.ConnectionManager;
import edu.zsc.ai.plugin.connection.ConnectionConfig;

import javax.sql.DataSource;

public interface ManagedDataSourceFactory {

    DataSource create(ConnectionManager connectionManager, ConnectionConfig connectionConfig, ManagedDataSourceRequest request);

    record ManagedDataSourceRequest(
            Long connectionId,
            String dbType,
            String catalog,
            String schema) {
    }
}
