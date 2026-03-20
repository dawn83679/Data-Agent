package edu.zsc.ai.domain.service.ai.model;

public record CompressionResult(
        String summary,
        Integer totalTokens,
        Integer outputTokens
) {
}
