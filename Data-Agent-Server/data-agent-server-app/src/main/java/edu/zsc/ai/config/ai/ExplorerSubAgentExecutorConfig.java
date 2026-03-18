package edu.zsc.ai.config.ai;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import edu.zsc.ai.observability.AgentLogService;
import edu.zsc.ai.observability.decorator.LoggingExecutorServiceDecorator;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Dedicated executor for concurrent explorer sub-agent tasks.
 * Fixed concurrency at 3 workers and uses a bounded blocking queue to apply backpressure.
 */
@Configuration
public class ExplorerSubAgentExecutorConfig {

    public static final String EXPLORER_SUB_AGENT_EXECUTOR_BEAN = "explorerSubAgentExecutor";

    @Bean(name = EXPLORER_SUB_AGENT_EXECUTOR_BEAN, destroyMethod = "shutdown")
    public ExecutorService explorerSubAgentExecutor(SubAgentProperties properties, AgentLogService agentLogService) {
        int maxConcurrency = Math.max(1, properties.getExplorer().getDispatch().getMaxConcurrency());
        int queueCapacity = Math.max(1, properties.getExplorer().getDispatch().getQueueCapacity());
        BlockingQueue<Runnable> queue = new ArrayBlockingQueue<>(queueCapacity);
        AtomicInteger threadCounter = new AtomicInteger(1);
        RejectedExecutionHandler blockingPolicy = (runnable, executor) -> {
            if (executor.isShutdown()) {
                new ThreadPoolExecutor.AbortPolicy().rejectedExecution(runnable, executor);
                return;
            }
            try {
                executor.getQueue().put(runnable);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RejectedExecutionException("Interrupted while waiting for explorer executor queue capacity", e);
            }
        };

        ExecutorService delegate = new ThreadPoolExecutor(
                maxConcurrency,
                maxConcurrency,
                60L,
                TimeUnit.SECONDS,
                queue,
                runnable -> {
                    Thread thread = new Thread(runnable);
                    thread.setName("explorer-sub-agent-" + threadCounter.getAndIncrement());
                    thread.setDaemon(true);
                    return thread;
                },
                blockingPolicy
        );
        return new LoggingExecutorServiceDecorator(delegate, agentLogService, EXPLORER_SUB_AGENT_EXECUTOR_BEAN);
    }
}
