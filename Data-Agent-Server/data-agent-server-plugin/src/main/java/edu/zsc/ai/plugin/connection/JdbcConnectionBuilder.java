package edu.zsc.ai.plugin.connection;

import java.util.Properties;

/**
 * Builder for JDBC connection components.
 * Constructs JDBC URL and connection properties from configuration.
 */
public interface JdbcConnectionBuilder {

    String PROP_USER = "user";
    String PROP_PASSWORD = "password";
    String PROP_CONNECT_TIMEOUT = "connectTimeout";
    String PROP_DATABASE = "database";

    /**
     * Build JDBC URL from connection configuration.
     */
    String buildUrl(ConnectionConfig config, String urlTemplate, int defaultPort);

    /**
     * Build connection properties from configuration.
     */
    Properties buildProperties(ConnectionConfig config);
}

