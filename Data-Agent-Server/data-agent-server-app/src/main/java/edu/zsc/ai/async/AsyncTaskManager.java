package edu.zsc.ai.async;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

@Slf4j
@Component
public class AsyncTaskManager {

    private static final long RETENTION_SECONDS = 30 * 60;

    private final ConcurrentHashMap<String, AsyncTask<?>> tasks = new ConcurrentHashMap<>();


    public <T> AsyncTask<T> submit(String taskId, Callable<T> callable,
                                   ThreadPoolTaskExecutor executor) {
        AsyncTask<T> record = new AsyncTask<>(taskId);
        tasks.put(taskId, record);

        Future<?> future = executor.submit(() -> {
            record.status = TaskStatus.RUNNING;
            try {
                record.result = callable.call();
                record.status = TaskStatus.COMPLETED;
                log.debug("Async task completed: taskId={}", taskId);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                record.status = TaskStatus.CANCELLED;
                log.debug("Async task interrupted: taskId={}", taskId);
            } catch (Exception e) {
                record.errorMessage = e.getMessage();
                record.status = TaskStatus.FAILED;
                log.warn("Async task failed: taskId={}, error={}", taskId, e.getMessage());
            }
        });
        record.future = future;
        return record;
    }


    @SuppressWarnings("unchecked")
    public <T> Optional<AsyncTask<T>> get(String taskId) {
        return Optional.ofNullable((AsyncTask<T>) tasks.get(taskId));
    }


    public boolean cancel(String taskId) {
        AsyncTask<?> record = tasks.get(taskId);
        if (record == null || isTerminal(record.status)) {
            return false;
        }
        record.status = TaskStatus.CANCELLED;
        if (record.future != null) {
            record.future.cancel(true);
        }
        return true;
    }

    @Scheduled(fixedDelay = 600_000)
    void cleanup() {
        long cutoff = Instant.now().getEpochSecond() - RETENTION_SECONDS;
        int removed = 0;
        var iter = tasks.entrySet().iterator();
        while (iter.hasNext()) {
            var entry = iter.next();
            if (isTerminal(entry.getValue().status) && entry.getValue().createdAt < cutoff) {
                iter.remove();
                removed++;
            }
        }
        if (removed > 0) {
            log.debug("Cleaned up {} expired async tasks", removed);
        }
    }

    public static boolean isTerminal(TaskStatus status) {
        return status == TaskStatus.COMPLETED
                || status == TaskStatus.FAILED
                || status == TaskStatus.CANCELLED;
    }
}
