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

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConnectRequest {
    
    @NotBlank(message = "Database type cannot be null or empty")
    private String dbType;
    
    @NotBlank(message = "Host cannot be null or empty")
    private String host;
    
    @Min(value = 1, message = "Port must be between 1 and 65535")
    @Max(value = 65535, message = "Port must be between 1 and 65535")
    private Integer port;
    
    private String database;

    @NotBlank(message = "Username cannot be null or empty")
    private String username;
    
    private String password;
    
    @NotBlank(message = "Driver JAR path cannot be null or empty")
    private String driverJarPath;
    
    @Min(value = 1, message = "Timeout must be between 1 and 300 seconds")
    @Max(value = 300, message = "Timeout must be between 1 and 300 seconds")
    @Builder.Default
    private Integer timeout = 30;
    
    @Builder.Default
    private Map<String, String> properties = new HashMap<>();
}

