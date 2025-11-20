package edu.zsc.ai.model.dto.response.db;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for available driver versions from Maven Central.
 * Represents driver versions that can be downloaded from the remote repository.
 *
 * @author Data-Agent
 * @since 0.0.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvailableDriverResponse {
    
    /**
     * Database type (e.g., "MySQL")
     */
    private String databaseType;
    
    /**
     * Driver version (e.g., "8.0.33", "5.1.49")
     */
    private String version;
    
    /**
     * Whether this version is already installed locally
     */
    private Boolean installed;
    
    /**
     * Maven group ID (e.g., "com.mysql")
     */
    private String groupId;
    
    /**
     * Maven artifact ID (e.g., "mysql-connector-j")
     */
    private String artifactId;
    
    /**
     * Full Maven coordinates string (e.g., "com.mysql:mysql-connector-j:8.0.33")
     */
    private String mavenCoordinates;
}

