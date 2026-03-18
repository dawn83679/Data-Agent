package edu.zsc.ai.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Global shared thread pool for discovery, async tasks, and all parallel IO scenarios.
 */
@Configuration
public class ExecutorConfig {

    /** Bean name for the global shared executor. Inject by this qualifier when needed. */
    public static final String SHARED_EXECUTOR_BEAN_NAME = "sharedExecutor";

    /**
     * Global bounded thread pool. Used by discovery (getEnvironmentOverview / searchObjects),
     * AsyncTaskManager, etc. Lifecycle managed by Spring (graceful shutdown).
     */
    @Bean(name = SHARED_EXECUTOR_BEAN_NAME)
    public Executor sharedExecutor(
            @Value("${app.executor.pool-size:20}") int poolSize) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(poolSize);
        executor.setMaxPoolSize(poolSize);
        executor.setQueueCapacity(0);
        executor.setThreadNamePrefix("app-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(10);
        executor.initialize();
        return executor;
    }
}
