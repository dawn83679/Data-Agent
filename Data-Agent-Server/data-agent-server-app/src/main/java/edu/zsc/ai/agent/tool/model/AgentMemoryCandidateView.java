package edu.zsc.ai.agent.tool.model;

import edu.zsc.ai.domain.model.dto.response.ai.MemoryCandidateResponse;

import java.util.List;

/**
 * Slim memory candidate view for agent consumption.
 * Omits conversationId and createdAt which are irrelevant to agent reasoning.
 */
public record AgentMemoryCandidateView(Long id, String candidateType, String candidateContent, String reason) {

    public static AgentMemoryCandidateView from(MemoryCandidateResponse r) {
        return new AgentMemoryCandidateView(r.getId(), r.getCandidateType(), r.getCandidateContent(), r.getReason());
    }

    public static List<AgentMemoryCandidateView> fromList(List<MemoryCandidateResponse> list) {
        return list.stream().map(AgentMemoryCandidateView::from).toList();
    }
}
