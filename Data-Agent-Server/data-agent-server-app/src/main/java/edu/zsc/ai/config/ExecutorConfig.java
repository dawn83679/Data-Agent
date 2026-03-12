package edu.zsc.ai.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 全局通用线程池，供发现、异步任务等所有并行 IO 场景共用。
 */
@Configuration
public class ExecutorConfig {

    /** 全局共享线程池的 Bean 名称，需要时注入此 Executor 即可。 */
    public static final String SHARED_EXECUTOR_BEAN_NAME = "sharedExecutor";

    /**
     * 全局通用有界线程池。发现（getEnvironmentOverview / searchObjects）、AsyncTaskManager 等
     * 均可注入使用，由 Spring 管理生命周期（优雅关闭）。
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
