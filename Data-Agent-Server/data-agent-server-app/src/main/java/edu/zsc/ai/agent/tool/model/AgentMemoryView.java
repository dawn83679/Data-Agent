package edu.zsc.ai.agent.tool.model;

import edu.zsc.ai.domain.service.ai.model.MemorySearchResult;

import java.util.List;

/**
 * Slim memory view for agent consumption.
 * Omits score and conversationId which are irrelevant to agent reasoning.
 */
public record AgentMemoryView(Long id, String memoryType, String content) {

    public static AgentMemoryView from(MemorySearchResult r) {
        return new AgentMemoryView(r.getId(), r.getMemoryType(), r.getContent());
    }

    public static List<AgentMemoryView> fromList(List<MemorySearchResult> list) {
        return list.stream().map(AgentMemoryView::from).toList();
    }
}
