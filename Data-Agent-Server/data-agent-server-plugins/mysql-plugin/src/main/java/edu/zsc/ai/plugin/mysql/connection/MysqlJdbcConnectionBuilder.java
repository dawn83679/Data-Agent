package edu.zsc.ai.plugin.mysql.connection;

import edu.zsc.ai.plugin.connection.JdbcConnectionBuilder;
import edu.zsc.ai.plugin.connection.ConnectionConfig;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Properties;

public class MysqlJdbcConnectionBuilder implements JdbcConnectionBuilder {

    @Override
    public String buildUrl(ConnectionConfig config, String urlTemplate, int defaultPort) {
        String host = config.getHost();
        int port = config.getPort() != null ? config.getPort() : defaultPort;
        String database = config.getDatabase() != null ? config.getDatabase() : "";

        // Build base JDBC URL (e.g., jdbc:mysql://localhost:3306/test)
        return String.format(urlTemplate, host, port, database);
    }

    @Override
    public Properties buildProperties(ConnectionConfig config) {
        Properties props = new Properties();

        // Set username and password
        if (StringUtils.isNotBlank(config.getUsername())) {
            props.setProperty(PROP_USER, config.getUsername());
        }
        if (StringUtils.isNotBlank(config.getPassword())) {
            props.setProperty(PROP_PASSWORD, config.getPassword());
        }

        // Set connection timeout (convert seconds to milliseconds)
        if (config.getTimeout() != null) {
            props.setProperty(PROP_CONNECT_TIMEOUT, String.valueOf(config.getTimeout() * 1000));
        }

        // Add additional properties
        if (MapUtils.isNotEmpty(config.getProperties())) {
            props.putAll(config.getProperties());
        }

        // Set database name if present
        if (StringUtils.isNotBlank(config.getDatabase())) {
            props.setProperty(PROP_DATABASE, config.getDatabase());
        }

        return props;
    }
}

