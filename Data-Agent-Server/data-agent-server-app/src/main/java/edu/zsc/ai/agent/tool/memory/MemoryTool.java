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
            "Supercharges your accuracy with learned knowledge — retrieves the user's confirmed ",
            "preferences, business rules, terminology mappings, and proven SQL patterns. Memories ",
            "contain hard-won insights from past conversations that no schema can provide.",
            "",
            "Call this whenever domain-specific terms, recurring patterns, or user conventions ",
            "might be relevant. One memory hit can save you from misinterpreting 'active users' ",
            "as 'status=active' when the user actually means 'logged in within 30 days'."
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
            return AgentToolResult.fail("Failed to search memories with query '" + queryText + "': " + e.getMessage());
        }
    }

    @Tool({
            "Prevents duplicate knowledge and keeps your memory proposals organized — shows all ",
            "pending candidates in this conversation so you know exactly what's already been ",
            "captured before proposing new entries.",
            "",
            "Check this before every createCandidateMemory call. Duplicate or conflicting ",
            "candidates confuse users during review and erode trust in the memory system."
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
            return AgentToolResult.fail("Failed to list candidate memories: " + e.getMessage());
        }
    }

    @Tool({
            "Makes the system smarter over time — captures reusable knowledge that will improve ",
            "accuracy in all future conversations. Each confirmed memory is a permanent boost to ",
            "the system's understanding of this user's domain, preferences, and conventions.",
            "",
            "Propose candidates when you discover stable, confirmed knowledge: user preferences, ",
            "business rules, domain terminology, golden SQL patterns, workflow constraints. ",
            "The user reviews all candidates, so propose generously — quality filtering happens ",
            "at review time."
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
            return AgentToolResult.fail("Failed to create candidate memory (type=" + candidateType + "): " + e.getMessage()
                    + ". Verify candidateType is one of PREFERENCE/BUSINESS_RULE/KNOWLEDGE_POINT/GOLDEN_SQL_CASE/WORKFLOW_CONSTRAINT.");
        }
    }

    @Tool({
            "Maintains memory quality — removes incorrect, outdated, or redundant candidates ",
            "so only high-value knowledge reaches the user for review. Clean candidates build ",
            "user trust in the memory system and improve long-term learning accuracy.",
            "",
            "Use proactively when you discover a candidate is wrong, duplicated, or superseded ",
            "by better information. A curated candidate list is far more valuable than a noisy one."
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
                return AgentToolResult.fail("Candidate memory with id=" + candidateId
                        + " not found or not owned by current user. Use listCandidateMemories to verify the candidateId.");
            }

            Map<String, Object> result = new HashMap<>();
            result.put("candidateId", candidateId);
            result.put("deleted", true);
            return AgentToolResult.success(result);
        } catch (Exception e) {
            log.error("[Tool error] deleteCandidateMemory, candidateId={}", candidateId, e);
            return AgentToolResult.fail("Failed to delete candidate memory with id=" + candidateId + ": " + e.getMessage());
        }
    }
}
