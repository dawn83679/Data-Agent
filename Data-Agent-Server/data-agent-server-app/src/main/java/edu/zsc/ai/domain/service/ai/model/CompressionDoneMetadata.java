package edu.zsc.ai.domain.service.ai.model;

import java.util.LinkedHashMap;
import java.util.Map;

public record CompressionDoneMetadata(
        boolean memoryCompressed,
        Integer tokenCountBefore,
        Integer tokenCountAfter,
        Integer compressedMessageCount,
        Integer keptRecentCount,
        Integer compressionOutputTokens,
        Integer compressionTotalTokens
) {

    public Map<String, Object> toMap() {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("memoryCompressed", memoryCompressed);
        metadata.put("tokenCountBefore", tokenCountBefore);
        metadata.put("tokenCountAfter", tokenCountAfter);
        metadata.put("compressedMessageCount", compressedMessageCount);
        metadata.put("keptRecentCount", keptRecentCount);
        metadata.put("compressionOutputTokens", compressionOutputTokens);
        metadata.put("compressionTotalTokens", compressionTotalTokens);
        return metadata;
    }
}
