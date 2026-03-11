package edu.zsc.ai.agent.tool.memory;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.invocation.InvocationParameters;
import edu.zsc.ai.agent.annotation.AgentTool;
import edu.zsc.ai.agent.tool.ToolContext;
import edu.zsc.ai.agent.tool.memory.model.AgentMemoryCandidateView;
import edu.zsc.ai.agent.tool.memory.model.AgentMemoryView;
import edu.zsc.ai.agent.tool.model.AgentToolResult;
import edu.zsc.ai.common.converter.ai.MemoryConverter;
import edu.zsc.ai.config.ai.MemoryProperties;
import edu.zsc.ai.context.RequestContext;
import edu.zsc.ai.domain.model.dto.response.ai.MemoryCandidateResponse;
import edu.zsc.ai.domain.model.entity.ai.AiMemoryCandidate;
import edu.zsc.ai.domain.service.ai.MemoryCandidateService;
import edu.zsc.ai.domain.service.ai.MemoryService;
import edu.zsc.ai.domain.service.ai.model.MemorySearchResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
            "Retrieves user's confirmed preferences, business rules, terminology mappings, and SQL patterns ",
            "from past conversations via semantic search.",
            "",
            "Use when: domain-specific terms, recurring patterns, or user conventions may be relevant.",
            "Skip when: query is purely structural with no domain-specific terminology."
    })
    public AgentToolResult searchMemories(
            @P("Natural language query to search memories") String queryText,
            @P(value = "Maximum number of results to return", required = false) Integer limit,
            InvocationParameters parameters) {
        try (var ctx = ToolContext.from(parameters)) {
            MemoryProperties.Retrieval retrieval = memoryProperties.getRetrieval();
            int safeLimit = limit == null ? retrieval.getCandidateTopK()
                    : Math.max(1, Math.min(limit, MAX_LIMIT));

            List<MemorySearchResult> results = memoryService.searchActiveMemories(
                    queryText, safeLimit, retrieval.getMinScore());

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
            "Lists all pending memory candidates in this conversation.",
            "",
            "Use when: checking for duplicates before createCandidateMemory.",
            "Skip when: no memories have been proposed in this conversation."
    })
    public AgentToolResult listCandidateMemories(
            @P(value = "Conversation id from current session context", required = false) Long conversationId,
            @P(value = "Maximum number of candidates to return", required = false) Integer limit,
            InvocationParameters parameters) {
        try (var ctx = ToolContext.from(parameters)) {
            Long contextConversationId = RequestContext.getConversationId();
            if (contextConversationId == null) {
                return AgentToolResult.noContext();
            }
            if (conversationId != null && !conversationId.equals(contextConversationId)) {
                log.warn("[Tool] listCandidateMemories ignored mismatched conversationId arg={}, context={}",
                        conversationId, contextConversationId);
            }

            int safeLimit = limit == null ? DEFAULT_LIMIT : Math.max(1, Math.min(limit, MAX_LIMIT));
            List<AiMemoryCandidate> candidates = memoryCandidateService
                    .listCurrentConversationCandidates(contextConversationId, safeLimit);

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
            "Proposes a reusable knowledge entry for user review: preferences, business rules, ",
            "terminology, golden SQL patterns, or workflow constraints.",
            "",
            "Use when: discovering stable, confirmed knowledge worth preserving.",
            "Skip when: information is session-specific or unverified."
    })
    public AgentToolResult createCandidateMemory(
            @P(value = "Conversation id from current session context", required = false) Long conversationId,
            @P("Candidate type: PREFERENCE/BUSINESS_RULE/KNOWLEDGE_POINT/GOLDEN_SQL_CASE/WORKFLOW_CONSTRAINT") String candidateType,
            @P("Normalized candidate memory text to be reviewed by user") String candidateContent,
            @P(value = "Why this candidate should be saved", required = false) String reason,
            InvocationParameters parameters) {
        try (var ctx = ToolContext.from(parameters)) {
            Long contextConversationId = RequestContext.getConversationId();
            if (contextConversationId == null) {
                return AgentToolResult.noContext();
            }
            if (conversationId != null && !conversationId.equals(contextConversationId)) {
                log.warn("[Tool] createCandidateMemory ignored mismatched conversationId arg={}, context={}",
                        conversationId, contextConversationId);
            }

            AiMemoryCandidate candidate = memoryCandidateService.createCandidate(
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
            "Removes an incorrect, outdated, or duplicate memory candidate.",
            "",
            "Use when: a candidate is wrong, duplicated, or superseded by better info."
    })
    public AgentToolResult deleteCandidateMemory(
            @P("Candidate id to delete") Long candidateId,
            InvocationParameters parameters) {
        try (var ctx = ToolContext.from(parameters)) {
            boolean deleted = memoryCandidateService.deleteCandidate(candidateId);
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
