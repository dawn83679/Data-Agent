package edu.zsc.ai.plugin.connection;

import edu.zsc.ai.plugin.exception.PluginErrorCode;
import edu.zsc.ai.plugin.exception.PluginException;
import edu.zsc.ai.plugin.model.ConnectionConfig;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.*;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;

/**
 * Utility class for loading JDBC drivers.
 * Handles loading drivers from external JAR files.
 */
public final class DriverLoader {

    /**
     * Cache for dynamically loaded driver class loaders
     */
    private static final ConcurrentMap<String, URLClassLoader> DRIVER_LOADERS = new ConcurrentHashMap<>();

    /**
     * Private constructor to prevent instantiation.
     */
    private DriverLoader() {
        // Utility class
    }

    /**
     * Load JDBC driver from external JAR file.
     *
     * @param config connection configuration (must contain driverJarPath)
     * @param driverClassName JDBC driver class name
     * @throws PluginException if driver loading fails
     */
    public static void loadDriver(ConnectionConfig config, String driverClassName) throws PluginException {
        loadDriverFromJar(config.getDriverJarPath(), driverClassName);
    }

    /**
     * Load driver from external JAR file
     */
    private static void loadDriverFromJar(String driverJarPath, String driverClassName) throws PluginException {
        File driverJar = new File(driverJarPath);
        if (!driverJar.exists() || !driverJar.isFile()) {
            throw new PluginException(PluginErrorCode.CONNECTION_FAILED,
                String.format("Driver JAR file not found: %s", driverJarPath));
        }

        // Use cached class loader if available
        URLClassLoader classLoader = DRIVER_LOADERS.computeIfAbsent(driverJarPath, path -> {
            try {
                URL jarUrl = driverJar.toURI().toURL();
                return new URLClassLoader(new URL[]{jarUrl}, Thread.currentThread().getContextClassLoader());
            } catch (Exception e) {
                throw new PluginException(PluginErrorCode.CONNECTION_FAILED,
                    String.format("Failed to create class loader for driver JAR: %s", driverJarPath), e);
            }
        });

        try {
            // Load driver class using the custom class loader
            Class<?> driverClass = Class.forName(driverClassName, true, classLoader);
            Driver driver = (Driver) driverClass.getDeclaredConstructor().newInstance();
            DriverManager.registerDriver(new DriverProxy(driver, classLoader));
        } catch (Exception e) {
            throw new PluginException(PluginErrorCode.CONNECTION_FAILED,
                String.format("Failed to load JDBC driver '%s' from %s", driverClassName, driverJarPath), e);
        }
    }

    /**
         * Driver proxy to use custom class loader
         */
        private record DriverProxy(Driver delegate, ClassLoader classLoader) implements Driver {

        @Override
            public Connection connect(String url, Properties info) throws SQLException {
                Thread currentThread = Thread.currentThread();
                ClassLoader originalLoader = currentThread.getContextClassLoader();
                try {
                    currentThread.setContextClassLoader(classLoader);
                    return delegate.connect(url, info);
                } finally {
                    currentThread.setContextClassLoader(originalLoader);
                }
            }

            @Override
            public boolean acceptsURL(String url) throws SQLException {
                return delegate.acceptsURL(url);
            }

            @Override
            public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
                return delegate.getPropertyInfo(url, info);
            }

            @Override
            public int getMajorVersion() {
                return delegate.getMajorVersion();
            }

            @Override
            public int getMinorVersion() {
                return delegate.getMinorVersion();
            }

            @Override
            public boolean jdbcCompliant() {
                return delegate.jdbcCompliant();
            }

            @Override
            public Logger getParentLogger() throws SQLFeatureNotSupportedException {
                return delegate.getParentLogger();
            }
        }
}

