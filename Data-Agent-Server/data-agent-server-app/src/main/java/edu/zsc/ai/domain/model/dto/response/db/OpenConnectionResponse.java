package edu.zsc.ai.domain.model.dto.response.db;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Open Connection Response DTO
 * Response body for establishing a persistent database connection.
 *
 * @author Data-Agent
 * @since 0.0.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OpenConnectionResponse {

    /**
     * Unique connection identifier (SHA-256 hash-based, enables connection reuse for same configuration)
     */
    private String connectionId;

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
     * Connection status (always true for openConnection response)
     */
    private Boolean connected;

    /**
     * Connection creation timestamp
     */
    private LocalDateTime createdAt;
}

