package edu.zsc.ai.agent.tool;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Thread-safe conversation turn statistics collector.
 * Tracks tool invocations and token usage during a single conversation turn.
 */
public class AgentToolTracker {

    private final Map<String, AtomicInteger> toolCounts = new ConcurrentHashMap<>();
    private final AtomicInteger totalCount = new AtomicInteger(0);

    // Token usage (set once when onCompleteResponse fires)
    private volatile Integer outputTokens;
    private volatile Integer totalTokens;

    /**
     * Record one invocation of the given tool.
     */
    public void record(String toolName) {
        toolCounts.computeIfAbsent(toolName, k -> new AtomicInteger(0)).incrementAndGet();
        totalCount.incrementAndGet();
    }

    public int getTotalCount() {
        return totalCount.get();
    }

    /**
     * Returns an immutable snapshot of per-tool counts.
     */
    public Map<String, Integer> getToolCounts() {
        return toolCounts.entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, e -> e.getValue().get()));
    }

    public void setTokenUsage(Integer outputTokens, Integer totalTokens) {
        this.outputTokens = outputTokens;
        this.totalTokens = totalTokens;
    }

    /**
     * Build metadata map for inclusion in the doneBlock.
     */
    public Map<String, Object> toMetadata() {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("toolCount", getTotalCount());
        metadata.put("toolCounts", getToolCounts());
        if (totalTokens != null) {
            metadata.put("totalTokens", totalTokens);
        }
        if (outputTokens != null) {
            metadata.put("outputTokens", outputTokens);
        }
        return metadata;
    }
}
