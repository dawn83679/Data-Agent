package edu.zsc.ai.domain.model.dto.request.db;

import java.util.HashMap;
import java.util.Map;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Connection creation request
 *
 * @author Data-Agent
 * @since 0.0.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConnectionCreateRequest {

    /**
     * Connection name, must be unique
     */
    @NotBlank(message = "Connection name cannot be null or empty")
    @Size(max = 100, message = "Connection name cannot exceed 100 characters")
    private String name;

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
    @Min(value = 1, message = "Port must be a positive integer")
    private Integer port;

    /**
     * Database name / schema name (optional for some databases)
     */
    private String database;

    /**
     * Username for authentication
     */
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
    @Min(value = 1, message = "Timeout must be at least 1 second")
    @Builder.Default
    private Integer timeout = 30;

    /**
     * Additional connection properties (e.g., SSL settings, encoding)
     */
    @Builder.Default
    private Map<String, String> properties = new HashMap<>();

    /**
     * User ID who owns this connection
     */
    private Long userId;
}