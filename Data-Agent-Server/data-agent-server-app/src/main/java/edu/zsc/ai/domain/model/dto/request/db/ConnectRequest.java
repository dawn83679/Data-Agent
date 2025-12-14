package edu.zsc.ai.domain.model.dto.request.db;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * Connect Request DTO
 * Request body for establishing a persistent database connection.
 *
 * @author Data-Agent
 * @since 0.0.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConnectRequest {
    
    /**
      * Database type (e.g., "MYSQL", "POSTGRESQL", "ORACLE")
     */
    @NotBlank(message = "Database type cannot be null or empty")
    private String dbType;
    
    /**
     * Database host address (e.g., "localhost", "192.168.1.100")
     */
    @NotBlank(message = "Host cannot be null or empty")
    private String host;
    
    /**
     * Database port (e.g., 3306 for MySQL, 5432 for PostgreSQL)
     */
    @Min(value = 1, message = "Port must be between 1 and 65535")
    @Max(value = 65535, message = "Port must be between 1 and 65535")
    private Integer port;
    
    /**
     * Database name / schema name (optional for some databases)
     */
    private String database;
    
    /**
     * Username for authentication
     */
    @NotBlank(message = "Username cannot be null or empty")
    private String username;
    
    /**
     * Password for authentication (can be empty for some databases)
     */
    private String password;
    
    /**
     * Path to external JDBC driver JAR file
     */
    @NotBlank(message = "Driver JAR path cannot be null or empty")
    private String driverJarPath;
    
    /**
     * Connection timeout in seconds
     */
    @Min(value = 1, message = "Timeout must be between 1 and 300 seconds")
    @Max(value = 300, message = "Timeout must be between 1 and 300 seconds")
    @Builder.Default
    private Integer timeout = 30;
    
    /**
     * Additional connection properties (e.g., SSL settings, encoding)
     */
    @Builder.Default
    private Map<String, String> properties = new HashMap<>();
}

