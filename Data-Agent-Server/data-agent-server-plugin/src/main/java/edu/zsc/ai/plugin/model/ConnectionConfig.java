package edu.zsc.ai.plugin.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * Database connection configuration.
 * Contains all necessary information to establish a database connection.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConnectionConfig {
    
    /**
     * Database host address (e.g., "localhost", "192.168.1.100")
     */
    private String host;
    
    /**
     * Database port (e.g., 3306 for MySQL, 5432 for PostgreSQL)
     */
    private Integer port;
    
    /**
     * Database name / schema name
     */
    private String database;
    
    /**
     * Username for authentication
     */
    private String username;
    
    /**
     * Password for authentication
     */
    private String password;
    
    /**
     * Additional connection properties (e.g., SSL settings, timeout, encoding)
     */
    private Map<String, String> properties;
    
    /**
     * Path to external JDBC driver JAR file.
     * Required. Drivers are loaded dynamically from external JAR files.
     */
    private String driverJarPath;
    
    /**
     * Connection timeout in seconds
     */
    private Integer timeout = 30;
    
    /**
     * Add a connection property
     *
     * @param key property key
     * @param value property value
     */
    public void addProperty(String key, String value) {
        if (properties == null) {
            properties = new HashMap<>();
        }
        properties.put(key, value);
    }
}

