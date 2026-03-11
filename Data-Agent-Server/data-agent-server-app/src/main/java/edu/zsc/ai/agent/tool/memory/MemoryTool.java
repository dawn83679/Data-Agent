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
            "Calling this tool can greatly improve accuracy when user preferences or business rules matter — ",
            "one memory hit can prevent misinterpretation (e.g. 'active users' vs 'status=active'). ",
            "Retrieves confirmed preferences, business rules, terminology, and SQL patterns by natural-language query.",
            "",
            "When to Use: when domain terms, user conventions, or recurring patterns might be relevant to the current task.",
            "When NOT to Use: when the request is generic and no user-specific knowledge is needed.",
            "Relation: optional; call before or during thinking/discovery when context might benefit from past learnings. limit optional, max 100."
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
            "Calling this before createCandidateMemory greatly improves memory quality — it prevents ",
            "duplicate or conflicting candidates and user confusion. ",
            "Lists pending memory candidates in this conversation so you avoid proposing duplicates.",
            "",
            "When to Use: before createCandidateMemory to check what is already proposed.",
            "When NOT to Use: when you are not about to create a candidate.",
            "Relation: call before createCandidateMemory; duplicate or conflicting candidates reduce user trust. conversationId/limit optional."
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
            "Calling this tool greatly improves long-term accuracy — each confirmed memory makes future ",
            "conversations smarter for this user. ",
            "Creates a memory candidate for user review; once confirmed it improves future conversations.",
            "",
            "When to Use: when you discover stable, confirmed knowledge: preferences, business rules, terminology, golden SQL, workflow constraints.",
            "When NOT to Use: when the same or conflicting candidate already exists — check listCandidateMemories first.",
            "Relation: call listCandidateMemories before proposing to avoid duplicates. candidateType: PREFERENCE|BUSINESS_RULE|KNOWLEDGE_POINT|GOLDEN_SQL_CASE|WORKFLOW_CONSTRAINT."
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
            "Calling this when a candidate is wrong greatly improves memory quality — it keeps the ",
            "candidate list clean and trustworthy. ",
            "Deletes a memory candidate by id so incorrect or redundant entries do not reach the user.",
            "",
            "When to Use: when a candidate is wrong, duplicated, or superseded by better information.",
            "When NOT to Use: when you are unsure of the candidateId — use listCandidateMemories to verify.",
            "Relation: use listCandidateMemories to get candidateId; then call here to remove. Server returns error if id not found or not owned."
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
