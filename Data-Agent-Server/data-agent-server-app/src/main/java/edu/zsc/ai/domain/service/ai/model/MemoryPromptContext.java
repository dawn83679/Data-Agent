package edu.zsc.ai.domain.service.ai.model;

import java.util.List;

import edu.zsc.ai.domain.service.ai.recall.MemoryRecallItem;
import edu.zsc.ai.domain.service.ai.recall.MemoryRecallResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemoryPromptContext {

    @Builder.Default
    private MemoryRecallResult recallResult = MemoryRecallResult.empty();

    private String currentConversationMemory;

    public List<MemoryRecallItem> getMemories() {
        return recallResult == null ? List.of() : recallResult.getItems();
    }
}
