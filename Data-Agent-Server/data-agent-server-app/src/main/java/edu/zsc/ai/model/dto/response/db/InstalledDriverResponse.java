package edu.zsc.ai.model.dto.response.db;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for locally installed/downloaded driver files.
 * Represents drivers that are already on the local disk.
 *
 * @author Data-Agent
 * @since 0.0.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InstalledDriverResponse {
    
    /**
     * Database type (from directory name, e.g., "MySQL")
     */
    private String databaseType;
    
    /**
     * Driver file name (e.g., "mysql-connector-j-8.0.33.jar")
     */
    private String fileName;
    
    /**
     * Driver version (extracted from filename)
     */
    private String version;
    
    /**
     * Full path to driver JAR file
     */
    private String filePath;
    
    /**
     * File size in bytes
     */
    private Long fileSize;
    
    /**
     * File last modified timestamp
     */
    private LocalDateTime lastModified;
}

