package edu.zsc.ai.domain.service.ai.recall;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class MemoryRecallContext {

    private Long conversationId;

    private String queryText;

    private String scope;

    private String memoryType;

    private String subType;

    private Double minScore;

    @Builder.Default
    private MemoryRecallMode recallMode = MemoryRecallMode.PROMPT;
}
