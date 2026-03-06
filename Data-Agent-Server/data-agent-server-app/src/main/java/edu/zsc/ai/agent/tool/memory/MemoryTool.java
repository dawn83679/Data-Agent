package edu.zsc.ai.agent.tool.memory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.invocation.InvocationParameters;
import edu.zsc.ai.agent.tool.annotation.AgentTool;
import edu.zsc.ai.agent.tool.model.AgentMemoryCandidateView;
import edu.zsc.ai.agent.tool.model.AgentMemoryView;
import edu.zsc.ai.agent.tool.model.AgentToolResult;
import edu.zsc.ai.common.constant.RequestContextConstant;
import edu.zsc.ai.common.converter.ai.MemoryConverter;
import edu.zsc.ai.config.ai.MemoryProperties;
import edu.zsc.ai.domain.model.dto.response.ai.MemoryCandidateResponse;
import edu.zsc.ai.domain.model.entity.ai.AiMemoryCandidate;
import edu.zsc.ai.domain.service.ai.MemoryCandidateService;
import edu.zsc.ai.domain.service.ai.MemoryService;
import edu.zsc.ai.domain.service.ai.model.MemorySearchResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AgentTool
@Slf4j
@RequiredArgsConstructor
public class MemoryTool {

    private static final int DEFAULT_LIMIT = 50;
    private static final int MAX_LIMIT = 100;

    private final MemoryCandidateService memoryCandidateService;
    private final MemoryService memoryService;
    private final MemoryProperties memoryProperties;

    @Tool({
            "[GOAL] Retrieve reusable user-confirmed knowledge (preferences, rules, terms) to guide current decisions.",
            "[PRECHECK] Use when prior memory may affect SQL scope, filter conventions, naming, or output style.",
            "[WHEN] Call for memory-aware tasks, especially repeated workflows or preference-sensitive behavior.",
            "[INPUT] Provide concise queryText; limit controls top-K candidates."
    })
    public AgentToolResult searchMemories(
            @P("Natural language query to search memories") String queryText,
            @P(value = "Maximum number of results to return", required = false) Integer limit,
            InvocationParameters parameters) {
        try {
            Long userId = parameters.get(RequestContextConstant.USER_ID);
            if (Objects.isNull(userId)) {
                return AgentToolResult.noContext();
            }

            MemoryProperties.Retrieval retrieval = memoryProperties.getRetrieval();
            int safeLimit = limit == null ? retrieval.getCandidateTopK()
                    : Math.max(1, Math.min(limit, MAX_LIMIT));

            List<MemorySearchResult> results = memoryService.searchActiveMemories(
                    userId, queryText, safeLimit, retrieval.getMinScore());

            if (results.isEmpty()) {
                return AgentToolResult.empty();
            }
            return AgentToolResult.success(AgentMemoryView.fromList(results));
        } catch (Exception e) {
            log.error("[Tool error] searchMemories", e);
            return AgentToolResult.fail(e);
        }
    }

    @Tool({
            "[GOAL] Review pending candidate memories in current conversation to avoid duplicate creation.",
            "[PRECHECK] Prefer before createCandidateMemory in the same conversation.",
            "[WHEN] Use when deciding whether a new candidate is necessary or existing candidate should be edited/deleted.",
            "[INPUT] conversationId is context-driven; limit controls response size."
    })
    public AgentToolResult listCandidateMemories(
            @P(value = "Conversation id from current session context", required = false) Long conversationId,
            @P(value = "Maximum number of candidates to return", required = false) Integer limit,
            InvocationParameters parameters) {
        try {
            Long userId = parameters.get(RequestContextConstant.USER_ID);
            Long contextConversationId = parameters.get(RequestContextConstant.CONVERSATION_ID);
            if (Objects.isNull(userId) || Objects.isNull(contextConversationId)) {
                return AgentToolResult.noContext();
            }
            if (conversationId != null && !conversationId.equals(contextConversationId)) {
                log.warn("[Tool] listCandidateMemories ignored mismatched conversationId arg={}, context={}",
                        conversationId, contextConversationId);
            }

            int safeLimit = limit == null ? DEFAULT_LIMIT : Math.max(1, Math.min(limit, MAX_LIMIT));
            List<AiMemoryCandidate> candidates = memoryCandidateService
                    .listCurrentConversationCandidates(userId, contextConversationId, safeLimit);

            List<MemoryCandidateResponse> response = candidates.stream()
                    .map(MemoryConverter::toCandidateResponse)
                    .toList();

            if (response.isEmpty()) {
                return AgentToolResult.empty();
            }
            return AgentToolResult.success(AgentMemoryCandidateView.fromList(response));
        } catch (Exception e) {
            log.error("[Tool error] listCandidateMemories", e);
            return AgentToolResult.fail(e);
        }
    }

    @Tool({
            "[GOAL] Propose a new reusable memory candidate for later user review/commit.",
            "[PRECHECK] Candidate must be stable, reusable, and explicitly confirmed by user; avoid transient facts.",
            "[WHEN] Use for confirmed preferences, business rules, terminology mappings, or stable SQL conventions.",
            "[INPUT] Provide candidateType + normalized candidateContent + optional reason."
    })
    public AgentToolResult createCandidateMemory(
            @P(value = "Conversation id from current session context", required = false) Long conversationId,
            @P("Candidate type: PREFERENCE/BUSINESS_RULE/KNOWLEDGE_POINT/GOLDEN_SQL_CASE/WORKFLOW_CONSTRAINT") String candidateType,
            @P("Normalized candidate memory text to be reviewed by user") String candidateContent,
            @P(value = "Why this candidate should be saved", required = false) String reason,
            InvocationParameters parameters) {
        try {
            Long userId = parameters.get(RequestContextConstant.USER_ID);
            Long contextConversationId = parameters.get(RequestContextConstant.CONVERSATION_ID);
            if (Objects.isNull(userId) || Objects.isNull(contextConversationId)) {
                return AgentToolResult.noContext();
            }
            if (conversationId != null && !conversationId.equals(contextConversationId)) {
                log.warn("[Tool] createCandidateMemory ignored mismatched conversationId arg={}, context={}",
                        conversationId, contextConversationId);
            }

            AiMemoryCandidate candidate = memoryCandidateService.createCandidate(
                    userId,
                    contextConversationId,
                    candidateType,
                    candidateContent,
                    reason);

            return AgentToolResult.success(AgentMemoryCandidateView.from(MemoryConverter.toCandidateResponse(candidate)));
        } catch (Exception e) {
            log.error("[Tool error] createCandidateMemory", e);
            return AgentToolResult.fail(e);
        }
    }

    @Tool({
            "[GOAL] Remove invalid or redundant candidate memories to keep candidate box clean.",
            "[WHEN] Use when candidate is incorrect, stale, duplicate, or user rejects it.",
            "[INPUT] candidateId must come from listCandidateMemories output.",
            "[AFTER] Continue with corrected candidate strategy if needed."
    })
    public AgentToolResult deleteCandidateMemory(
            @P("Candidate id to delete") Long candidateId,
            InvocationParameters parameters) {
        try {
            Long userId = parameters.get(RequestContextConstant.USER_ID);
            if (Objects.isNull(userId)) {
                return AgentToolResult.noContext();
            }

            boolean deleted = memoryCandidateService.deleteCandidate(userId, candidateId);
            if (!deleted) {
                return AgentToolResult.empty();
            }

            Map<String, Object> result = new HashMap<>();
            result.put("candidateId", candidateId);
            result.put("deleted", true);
            return AgentToolResult.success(result);
        } catch (Exception e) {
            log.error("[Tool error] deleteCandidateMemory", e);
            return AgentToolResult.fail(e);
        }
    }
}
