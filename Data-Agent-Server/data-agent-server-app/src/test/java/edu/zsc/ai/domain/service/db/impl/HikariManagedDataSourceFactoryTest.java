package edu.zsc.ai.domain.service.db.impl;

import com.zaxxer.hikari.HikariDataSource;
import edu.zsc.ai.config.db.ConnectionPoolProperties;
import edu.zsc.ai.domain.service.db.ManagedDataSourceFactory;
import edu.zsc.ai.plugin.capability.ConnectionProvider;
import edu.zsc.ai.plugin.connection.ConnectionConfig;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HikariManagedDataSourceFactoryTest {

    @Test
    void create_appliesConfiguredPoolSettings() throws SQLException {
        ConnectionPoolProperties properties = new ConnectionPoolProperties();
        properties.setMaximumPoolSize(7);
        properties.setMinimumIdle(2);
        properties.setConnectionTimeoutMs(12_000L);
        properties.setValidationTimeoutMs(4_000L);
        properties.setIdleTimeoutMs(50_000L);
        properties.setMaxLifetimeMs(180_000L);

        HikariManagedDataSourceFactory factory = new HikariManagedDataSourceFactory(properties);
        ConnectionProvider provider = mock(ConnectionProvider.class);
        Connection connection = mock(Connection.class);
        when(connection.isClosed()).thenReturn(false);
        when(connection.isValid(anyInt())).thenReturn(true);
        when(provider.connect(any())).thenReturn(connection);

        ConnectionConfig config = new ConnectionConfig();
        config.setHost("127.0.0.1");
        config.setPort(3306);
        config.setDatabase("demo");
        config.setUsername("demo");
        config.setPassword("secret");

        ManagedDataSourceFactory.ManagedDataSourceRequest request =
                new ManagedDataSourceFactory.ManagedDataSourceRequest(9L, "mysql", "demo", null);

        HikariDataSource dataSource = assertInstanceOf(HikariDataSource.class, factory.create(provider, config, request));
        try {
            assertEquals("mysql-9-demo-default", dataSource.getPoolName());
            assertEquals(7, dataSource.getMaximumPoolSize());
            assertEquals(2, dataSource.getMinimumIdle());
            assertEquals(12_000L, dataSource.getConnectionTimeout());
            assertEquals(4_000L, dataSource.getValidationTimeout());
            assertEquals(50_000L, dataSource.getIdleTimeout());
            assertEquals(180_000L, dataSource.getMaxLifetime());
        } finally {
            dataSource.close();
        }
    }

    @Test
    void create_failsWhenInitialConnectionCannotBeEstablished() {
        ConnectionPoolProperties properties = new ConnectionPoolProperties();
        HikariManagedDataSourceFactory factory = new HikariManagedDataSourceFactory(properties);
        ConnectionProvider provider = mock(ConnectionProvider.class);
        when(provider.connect(any())).thenThrow(new RuntimeException("boom"));

        ConnectionConfig config = new ConnectionConfig();
        config.setHost("127.0.0.1");
        ManagedDataSourceFactory.ManagedDataSourceRequest request =
                new ManagedDataSourceFactory.ManagedDataSourceRequest(9L, "mysql", null, null);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> factory.create(provider, config, request)
        );

        assertNotNull(exception.getMessage());
    }
}
