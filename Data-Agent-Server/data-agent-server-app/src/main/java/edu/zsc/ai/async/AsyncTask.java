package edu.zsc.ai.async;

import lombok.Data;

import java.time.Instant;
import java.util.concurrent.Future;


@Data
public class AsyncTask<T> {

    private final String id;
    final long createdAt;

    volatile TaskStatus status = TaskStatus.PENDING;
    volatile T result;
    volatile String errorMessage;
    volatile Future<?> future;

    AsyncTask(String id) {
        this.id = id;
        this.createdAt = Instant.now().getEpochSecond();
    }
}
