package edu.zsc.ai.domain.service.db.impl;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import edu.zsc.ai.config.db.ConnectionPoolProperties;
import edu.zsc.ai.domain.service.db.ManagedDataSourceFactory;
import edu.zsc.ai.domain.service.db.support.ManagerBackedDataSource;
import edu.zsc.ai.plugin.capability.ConnectionManager;
import edu.zsc.ai.plugin.connection.ConnectionConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;

@Component
@RequiredArgsConstructor
public class HikariManagedDataSourceFactory implements ManagedDataSourceFactory {

    private final ConnectionPoolProperties connectionPoolProperties;

    @Override
    public DataSource create(ConnectionManager connectionManager,
                             ConnectionConfig connectionConfig,
                             ManagedDataSourceRequest request) {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setPoolName(buildPoolName(request));
        hikariConfig.setDataSource(new ManagerBackedDataSource(connectionManager, connectionConfig));
        hikariConfig.setMaximumPoolSize(connectionPoolProperties.getMaximumPoolSize());
        hikariConfig.setMinimumIdle(connectionPoolProperties.getMinimumIdle());
        hikariConfig.setConnectionTimeout(connectionPoolProperties.getConnectionTimeoutMs());
        hikariConfig.setValidationTimeout(Math.min(
                connectionPoolProperties.getConnectionTimeoutMs(),
                connectionPoolProperties.getValidationTimeoutMs()
        ));
        hikariConfig.setIdleTimeout(connectionPoolProperties.getIdleTimeoutMs());
        hikariConfig.setMaxLifetime(connectionPoolProperties.getMaxLifetimeMs());

        HikariDataSource dataSource = new HikariDataSource(hikariConfig);
        try (Connection ignored = dataSource.getConnection()) {
            return dataSource;
        } catch (Exception e) {
            dataSource.close();
            throw new RuntimeException("Failed to initialize pooled data source: " + e.getMessage(), e);
        }
    }

    private String buildPoolName(ManagedDataSourceRequest request) {
        String catalog = request.catalog() != null ? request.catalog() : "root";
        String schema = request.schema() != null ? request.schema() : "default";
        return String.format("%s-%d-%s-%s", request.dbType(), request.connectionId(), catalog, schema);
    }
}
