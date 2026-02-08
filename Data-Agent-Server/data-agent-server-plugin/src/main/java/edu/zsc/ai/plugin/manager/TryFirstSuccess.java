package edu.zsc.ai.plugin.manager;

import java.util.List;
import java.util.function.Function;

public final class TryFirstSuccess {

    private TryFirstSuccess() {
    }

    public record AttemptResult<T, R>(T candidate, R result) {
    }

    public static <T, R> AttemptResult<T, R> tryFirstSuccess(List<T> candidates, Function<T, R> operation) {
        if (candidates == null || candidates.isEmpty()) {
            throw new IllegalArgumentException("Candidates list cannot be null or empty");
        }
        for (T c : candidates) {
            try {
                return new AttemptResult<>(c, operation.apply(c));
            } catch (Throwable ignored) {
            }
        }
        return null;
    }
}
