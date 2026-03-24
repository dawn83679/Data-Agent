package edu.zsc.ai.domain.service.db.support;

import edu.zsc.ai.plugin.capability.ConnectionManager;
import edu.zsc.ai.plugin.connection.ConnectionConfig;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * App-managed DataSource wrapper that asks a plugin manager to create physical JDBC connections.
 */
public final class ManagerBackedDataSource implements DataSource {

    private final ConnectionManager connectionManager;
    private final ConnectionConfig baseConfig;

    public ManagerBackedDataSource(ConnectionManager connectionManager, ConnectionConfig baseConfig) {
        this.connectionManager = Objects.requireNonNull(connectionManager, "connectionManager");
        this.baseConfig = copy(baseConfig);
    }

    @Override
    public Connection getConnection() {
        return connectionManager.connect(copy(baseConfig));
    }

    @Override
    public Connection getConnection(String username, String password) {
        ConnectionConfig config = copy(baseConfig);
        config.setUsername(username);
        config.setPassword(password);
        return connectionManager.connect(config);
    }

    @Override
    public PrintWriter getLogWriter() {
        return null;
    }

    @Override
    public void setLogWriter(PrintWriter out) {
        // no-op
    }

    @Override
    public void setLoginTimeout(int seconds) {
        baseConfig.setTimeout(seconds);
    }

    @Override
    public int getLoginTimeout() {
        return baseConfig.getTimeout() != null ? baseConfig.getTimeout() : 0;
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        throw new SQLFeatureNotSupportedException("Parent logger is not supported");
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        if (iface.isInstance(this)) {
            return iface.cast(this);
        }
        throw new SQLException("Unsupported unwrap target: " + iface.getName());
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) {
        return iface.isInstance(this);
    }

    private static ConnectionConfig copy(ConnectionConfig source) {
        ConnectionConfig target = new ConnectionConfig();
        target.setHost(source.getHost());
        target.setPort(source.getPort());
        target.setDatabase(source.getDatabase());
        target.setSchema(source.getSchema());
        target.setUsername(source.getUsername());
        target.setPassword(source.getPassword());
        target.setDriverJarPath(source.getDriverJarPath());
        target.setTimeout(source.getTimeout());
        if (source.getProperties() != null) {
            target.setProperties(new HashMap<>(source.getProperties()));
        }
        return target;
    }
}
