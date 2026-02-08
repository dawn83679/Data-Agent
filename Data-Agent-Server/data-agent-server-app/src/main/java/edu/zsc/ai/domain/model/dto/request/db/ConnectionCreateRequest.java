package edu.zsc.ai.domain.model.dto.request.db;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
public class ConnectionCreateRequest {

    private Long connectionId;

    @NotBlank(message = "Connection name cannot be null or empty")
    @Size(max = 100, message = "Connection name cannot exceed 100 characters")
    private String name;

    @NotBlank(message = "Database type cannot be null or empty")
    private String dbType;

    @NotBlank(message = "Host cannot be null or empty")
    private String host;

    @Min(value = 1, message = "Port must be a positive integer")
    private Integer port;

    private String database;

    private String username;

    private String password;

    @NotBlank(message = "Driver JAR path cannot be null or empty")
    private String driverJarPath;

    @Min(value = 1, message = "Timeout must be at least 1 second")
    @Builder.Default
    private Integer timeout = 30;

    @Builder.Default
    private Map<String, String> properties = new HashMap<>();
}