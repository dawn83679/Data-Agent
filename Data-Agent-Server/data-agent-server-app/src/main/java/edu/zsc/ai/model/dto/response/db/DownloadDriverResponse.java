package edu.zsc.ai.model.dto.response.db;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for driver download operation.
 *
 * @author Data-Agent
 * @since 0.0.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DownloadDriverResponse {
    
    /**
     * Path to downloaded driver file
     */
    private String driverPath;
    
    /**
     * Database type (e.g., "MySQL")
     */
    private String databaseType;
    
    /**
     * Driver file name (e.g., "mysql-connector-j-8.0.33.jar")
     */
    private String fileName;
    
    /**
     * Driver version
     */
    private String version;
}

