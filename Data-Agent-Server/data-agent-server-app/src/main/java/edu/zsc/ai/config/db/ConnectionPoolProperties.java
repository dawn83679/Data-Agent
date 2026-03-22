package edu.zsc.ai.config.db;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "db.pool")
public class ConnectionPoolProperties {

    private int maximumPoolSize = 10;

    private int minimumIdle = 1;

    private long connectionTimeoutMs = 30_000L;

    private long validationTimeoutMs = 5_000L;

    private long idleTimeoutMs = 600_000L;

    private long maxLifetimeMs = 1_800_000L;
}
