package edu.zsc.ai.agent.subagent;

/**
 * Shared timeout normalization for all sub-agents.
 */
public final class SubAgentTimeoutPolicy {

    public static final long MIN_TIMEOUT_SECONDS = 180L;

    private SubAgentTimeoutPolicy() {
    }

    public static long normalizeTimeoutSeconds(Long requestedTimeoutSeconds, long defaultTimeoutSeconds) {
        long candidate = requestedTimeoutSeconds != null && requestedTimeoutSeconds > 0
                ? requestedTimeoutSeconds
                : defaultTimeoutSeconds;
        return Math.max(candidate, MIN_TIMEOUT_SECONDS);
    }
}
