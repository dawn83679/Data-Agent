package edu.zsc.ai.domain.service.db;

import edu.zsc.ai.plugin.capability.ConnectionProvider;
import edu.zsc.ai.plugin.connection.ConnectionConfig;

import javax.sql.DataSource;

public interface ManagedDataSourceFactory {

    DataSource create(ConnectionProvider connectionProvider, ConnectionConfig connectionConfig, ManagedDataSourceRequest request);

    record ManagedDataSourceRequest(
            Long connectionId,
            String dbType,
            String catalog,
            String schema) {
    }
}
