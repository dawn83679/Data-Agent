package edu.zsc.ai.model.dto.response.db;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Connection response DTO
 *
 * @author Data-Agent
 * @since 0.0.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConnectionResponse {

    /**
     * Connection ID
     */
    private Long id;

    /**
     * Connection name
     */
    private String name;

    /**
     * Database type
     */
    private String dbType;

    /**
     * Host address
     */
    private String host;

    /**
     * Port number
     */
    private Integer port;

    /**
     * Database name
     */
    private String database;

    /**
     * Username
     */
    private String username;

    /**
     * Driver JAR path
     */
    private String driverJarPath;

    /**
     * Connection timeout in seconds
     */
    private Integer timeout;

    /**
     * Connection parameters
     */
    private Map<String, String> properties;

    /**
     * Creation time
     */
    private LocalDateTime createdAt;

    /**
     * Update time
     */
    private LocalDateTime updatedAt;
}