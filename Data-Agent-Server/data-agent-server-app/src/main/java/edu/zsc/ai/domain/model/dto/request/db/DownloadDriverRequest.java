package edu.zsc.ai.domain.model.dto.request.db;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for downloading a driver.
 *
 * @author Data-Agent
 * @since 0.0.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DownloadDriverRequest {
    
    /**
     * Database type (required, e.g., "MySQL")
     */
    @NotBlank(message = "Database type cannot be null or empty")
    private String databaseType;
    
    /**
     * Driver version (optional, uses latest version if not provided)
     */
    private String version;
}

