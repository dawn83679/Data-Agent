package edu.zsc.ai.domain.service.ai.impl;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.MetadataFilterBuilder;
import edu.zsc.ai.common.constant.MemoryConstant;
import edu.zsc.ai.common.constant.MemoryLogConstant;
import edu.zsc.ai.common.constant.MemoryMetadataConstant;
import edu.zsc.ai.common.constant.MemoryRecallLogConstant;
import edu.zsc.ai.common.constant.MemoryRecallPlanningConstant;
import edu.zsc.ai.common.enums.ai.MemoryScopeEnum;
import edu.zsc.ai.common.enums.ai.MemorySourceTypeEnum;
import edu.zsc.ai.common.enums.ai.MemorySubTypeEnum;
import edu.zsc.ai.common.enums.ai.MemoryToolActionEnum;
import edu.zsc.ai.common.enums.ai.MemoryTypeEnum;
import edu.zsc.ai.common.enums.ai.MemoryWorkspaceLevelEnum;
import edu.zsc.ai.common.enums.ai.MemoryStatusEnum;
import edu.zsc.ai.config.ai.MemoryProperties;
import edu.zsc.ai.context.RequestContext;
import edu.zsc.ai.domain.exception.BusinessException;
import edu.zsc.ai.domain.mapper.ai.AiMemoryMapper;
import edu.zsc.ai.domain.model.dto.request.ai.MemoryCreateRequest;
import edu.zsc.ai.domain.model.dto.request.ai.MemoryUpdateRequest;
import edu.zsc.ai.domain.model.dto.request.ai.MemoryWriteRequest;
import edu.zsc.ai.domain.model.dto.request.base.PageRequest;
import edu.zsc.ai.domain.model.dto.response.base.PageResponse;
import edu.zsc.ai.domain.model.entity.ai.AiMemory;
import edu.zsc.ai.domain.service.ai.MemoryService;
import edu.zsc.ai.domain.service.ai.model.MemoryMaintenanceReport;
import edu.zsc.ai.domain.service.ai.model.MemorySearchResult;
import edu.zsc.ai.domain.service.ai.model.MemoryWriteResult;
import edu.zsc.ai.domain.service.ai.recall.MemoryRecallQuery;
import edu.zsc.ai.domain.service.ai.recall.MemoryRecallQueryStrategy;
import edu.zsc.ai.observability.AgentLogFields;
import edu.zsc.ai.observability.AgentLogService;
import edu.zsc.ai.observability.AgentLogType;
import edu.zsc.ai.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemoryServiceImpl extends ServiceImpl<AiMemoryMapper, AiMemory> implements MemoryService {

    private static final int ACTIVE_MEMORY_STATUS = MemoryStatusEnum.ACTIVE.getCode();
    private static final String DEFAULT_SCOPE = MemoryConstant.DEFAULT_SCOPE;
    private static final String DEFAULT_WORKSPACE_CONTEXT_KEY = MemoryConstant.DEFAULT_WORKSPACE_CONTEXT_KEY;
    private static final String DEFAULT_SOURCE_TYPE = MemorySourceTypeEnum.MANUAL.getCode();
    private static final String AGENT_SOURCE_TYPE = MemorySourceTypeEnum.AGENT.getCode();
    private static final double DEFAULT_CONFIDENCE = 0.90;
    private static final double DEFAULT_SALIENCE = 0.60;
    private static final String DEFAULT_DETAIL_JSON = MemoryConstant.EMPTY_DETAIL_JSON;

    private final EmbeddingStore<TextSegment> memoryEmbeddingStore;
    private final EmbeddingModel embeddingModel;
    private final MemoryProperties memoryProperties;
    private final AgentLogService agentLogService;

    @Override
    public List<MemorySearchResult> searchActiveMemories(String queryText, int limit, double minScore) {
        int safeLimit = Math.max(1, limit);
        List<MemorySearchResult> results = recallAccessibleMemories(RequestContext.getConversationId(), queryText, minScore).stream()
                .limit(safeLimit)
                .toList();
        agentLogService.recordDebug(MemoryLogConstant.LOGGER_NAME, MemoryLogConstant.EVENT_MEMORY_SEARCH,
                AgentLogFields.of(
                        MemoryLogConstant.FIELD_USER_ID, RequestContext.getUserId(),
                        MemoryLogConstant.FIELD_QUERY_TEXT_PRESENT, StringUtils.isNotBlank(queryText),
                        MemoryLogConstant.FIELD_LIMIT, safeLimit,
                        MemoryLogConstant.FIELD_MIN_SCORE, minScore,
                        MemoryLogConstant.FIELD_RESULT_COUNT, results.size(),
                        MemoryLogConstant.FIELD_MEMORY_IDS, results.stream().map(MemorySearchResult::getId).toList()
                ));
        return results;
    }

    @Override
    public List<MemorySearchResult> recallAccessibleMemories(Long conversationId, String queryText, double minScore) {
        return recallAccessibleMemories(conversationId, queryText, minScore, null);
    }

    @Override
    public List<MemorySearchResult> recallAccessibleMemories(Long conversationId, String queryText, double minScore, String scope) {
        return recallAccessibleMemories(new MemoryRecallQuery(
                MemoryRecallPlanningConstant.LEGACY_QUERY_NAME,
                MemoryRecallPlanningConstant.LEGACY_PLANNING_REASON,
                scope,
                conversationId,
                queryText,
                null,
                null,
                minScore,
                null,
                MemoryRecallQueryStrategy.HYBRID,
                0));
    }

    @Override
    public List<MemorySearchResult> recallAccessibleMemories(MemoryRecallQuery query) {
        Long userId = RequestContext.getUserId();
        if (Objects.isNull(userId)) {
            return List.of();
        }
        Long conversationId = query.conversationId();
        List<String> workspaceChain = currentWorkspaceContextChain();
        String normalizedScope = StringUtils.isBlank(query.targetScope()) ? null : query.targetScope().trim().toUpperCase();
        String queryText = query.queryText();
        double minScore = query.minScore() == null ? 0.0D : query.minScore();
        MemoryRecallQueryStrategy strategy = query.queryStrategy() == null ? MemoryRecallQueryStrategy.HYBRID : query.queryStrategy();

        RecallExecutionResult executionResult = recallByStrategy(
                strategy, userId, conversationId, workspaceChain, normalizedScope, queryText, minScore);
        recordRecallQueryResult(query, executionResult.results(), strategy, normalizedScope, queryText,
                executionResult.usedFallback(), executionResult.executionPath());
        return executionResult.results();
    }

    private RecallExecutionResult recallByStrategy(MemoryRecallQueryStrategy strategy,
                                                   Long userId,
                                                   Long conversationId,
                                                   List<String> workspaceChain,
                                                   String normalizedScope,
                                                   String queryText,
                                                   double minScore) {
        return switch (strategy) {
            case BROWSE -> browseRecall(userId, conversationId, workspaceChain, normalizedScope);
            case SEMANTIC -> semanticRecall(userId, conversationId, workspaceChain, normalizedScope, queryText, minScore);
            case HYBRID -> hybridRecall(userId, conversationId, workspaceChain, normalizedScope, queryText, minScore);
        };
    }

    private RecallExecutionResult browseRecall(Long userId,
                                               Long conversationId,
                                               List<String> workspaceChain,
                                               String normalizedScope) {
        return new RecallExecutionResult(
                listVisibleActiveMemories(userId, conversationId, workspaceChain, normalizedScope),
                MemoryRecallLogConstant.EXECUTION_PATH_BROWSE,
                false);
    }

    private RecallExecutionResult hybridRecall(Long userId,
                                               Long conversationId,
                                               List<String> workspaceChain,
                                               String normalizedScope,
                                               String queryText,
                                               double minScore) {
        if (StringUtils.isBlank(queryText)) {
            return new RecallExecutionResult(
                    browseRecall(userId, conversationId, workspaceChain, normalizedScope).results(),
                    MemoryRecallLogConstant.EXECUTION_PATH_HYBRID_BROWSE_FALLBACK,
                    true);
        }
        RecallExecutionResult semanticResult = semanticRecall(userId, conversationId, workspaceChain, normalizedScope, queryText, minScore);
        if (!semanticResult.results().isEmpty()) {
            return new RecallExecutionResult(
                    semanticResult.results(),
                    MemoryRecallLogConstant.EXECUTION_PATH_HYBRID_SEMANTIC,
                    false);
        }
        return new RecallExecutionResult(
                browseRecall(userId, conversationId, workspaceChain, normalizedScope).results(),
                MemoryRecallLogConstant.EXECUTION_PATH_HYBRID_BROWSE_FALLBACK,
                true);
    }

    private RecallExecutionResult semanticRecall(Long userId,
                                                 Long conversationId,
                                                 List<String> workspaceChain,
                                                 String normalizedScope,
                                                 String queryText,
                                                 double minScore) {
        if (StringUtils.isBlank(queryText)) {
            return new RecallExecutionResult(
                    browseRecall(userId, conversationId, workspaceChain, normalizedScope).results(),
                    MemoryRecallLogConstant.EXECUTION_PATH_SEMANTIC_EXCEPTION_FALLBACK,
                    true);
        }
        try {
            Embedding queryEmbedding = embeddingModel.embed(queryText).content();
            var baseFilter = MetadataFilterBuilder.metadataKey(MemoryMetadataConstant.USER_ID).isEqualTo(userId)
                    .and(MetadataFilterBuilder.metadataKey(MemoryMetadataConstant.STATUS).isEqualTo(ACTIVE_MEMORY_STATUS));
            var filter = StringUtils.isBlank(normalizedScope)
                    ? baseFilter
                    : baseFilter.and(MetadataFilterBuilder.metadataKey(MemoryMetadataConstant.SCOPE).isEqualTo(normalizedScope));

            EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                    .queryEmbedding(queryEmbedding)
                    .maxResults(resolveRecallCandidateLimit(userId))
                    .minScore(minScore)
                    .filter(filter)
                    .build();

            EmbeddingSearchResult<TextSegment> searchResult = memoryEmbeddingStore.search(request);
            return new RecallExecutionResult(searchResult.matches().stream()
                    .map(this::toMemorySearchResult)
                    .filter(result -> matchesScope(result, normalizedScope))
                    .filter(result -> isVisibleToCurrentContext(result, conversationId, workspaceChain))
                    .sorted(promptMemoryComparator())
                    .toList(),
                    MemoryRecallLogConstant.EXECUTION_PATH_SEMANTIC,
                    false);
        } catch (Exception e) {
            log.warn("Failed to recall memory by embedding, fallback to visible active memories", e);
            return new RecallExecutionResult(
                    browseRecall(userId, conversationId, workspaceChain, normalizedScope).results(),
                    MemoryRecallLogConstant.EXECUTION_PATH_SEMANTIC_EXCEPTION_FALLBACK,
                    true);
        }
    }

    private void recordRecallQueryResult(MemoryRecallQuery query,
                                         List<MemorySearchResult> results,
                                         MemoryRecallQueryStrategy strategy,
                                         String normalizedScope,
                                         String queryText,
                                         boolean usedFallback,
                                         String executionPath) {
        agentLogService.recordDebug(MemoryRecallLogConstant.LOGGER_NAME, MemoryRecallLogConstant.EVENT_RECALL_QUERY_RESULT,
                AgentLogFields.of(
                        MemoryRecallLogConstant.FIELD_QUERY_NAME, query.queryName(),
                        MemoryRecallLogConstant.FIELD_PLANNING_REASON, query.planningReason(),
                        MemoryRecallLogConstant.FIELD_TARGET_SCOPE, normalizedScope,
                        MemoryRecallLogConstant.FIELD_QUERY_STRATEGY, strategy.name(),
                        MemoryRecallLogConstant.FIELD_QUERY_TEXT_PRESENT, StringUtils.isNotBlank(queryText),
                        MemoryRecallLogConstant.FIELD_RESULT_COUNT, results.size(),
                        MemoryRecallLogConstant.FIELD_RESULT_MEMORY_IDS, results.stream().map(MemorySearchResult::getId).toList(),
                        MemoryRecallLogConstant.FIELD_USED_FALLBACK, usedFallback,
                        MemoryRecallLogConstant.FIELD_EXECUTION_PATH, executionPath
                ));
    }

    private record RecallExecutionResult(List<MemorySearchResult> results, String executionPath, boolean usedFallback) {
    }

    @Override
    public PageResponse<AiMemory> pageCurrentUserMemories(PageRequest pageRequest,
                                                          String keyword,
                                                          String memoryType,
                                                          Integer status,
                                                          String scope) {
        Long userId = requireUserId();
        LambdaQueryWrapper<AiMemory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiMemory::getUserId, userId)
                .orderByDesc(AiMemory::getUpdatedAt)
                .orderByDesc(AiMemory::getId);

        if (StringUtils.isNotBlank(keyword)) {
            wrapper.and(q -> q.like(AiMemory::getTitle, keyword)
                    .or()
                    .like(AiMemory::getContent, keyword));
        }
        if (StringUtils.isNotBlank(memoryType)) {
            wrapper.eq(AiMemory::getMemoryType, resolveMemoryType(memoryType));
        }
        if (status != null) {
            wrapper.eq(AiMemory::getStatus, status);
        }
        if (StringUtils.isNotBlank(scope)) {
            wrapper.eq(AiMemory::getScope, scope.trim().toUpperCase());
        }

        Page<AiMemory> page = new Page<>(pageRequest.getCurrent(), pageRequest.getSize(), false);
        return PageResponse.of(page(page, wrapper));
    }

    @Override
    public AiMemory getByIdForCurrentUser(Long memoryId) {
        Long userId = requireUserId();
        BusinessException.assertNotNull(memoryId, "memoryId is required");
        AiMemory memory = getById(memoryId);
        if (memory == null || !userId.equals(memory.getUserId())) {
            throw BusinessException.notFound("Memory not found");
        }
        return memory;
    }

    @Override
    public AiMemory createManualMemory(MemoryCreateRequest request) {
        Long userId = requireUserId();
        LocalDateTime now = LocalDateTime.now();
        String content = StringUtils.trimToEmpty(request.getContent());
        String scope = normalizeOrDefault(request.getScope(), DEFAULT_SCOPE);
        BusinessException.assertTrue(!MemoryScopeEnum.WORKSPACE.matches(scope),
                "Manual memory creation does not support WORKSPACE scope.");
        AiMemory memory = AiMemory.builder()
                .userId(userId)
                .conversationId(request.getConversationId())
                .workspaceContextKey(null)
                .workspaceLevel(null)
                .scope(scope)
                .memoryType(resolveMemoryType(request.getMemoryType()))
                .subType(resolveMemorySubType(request.getMemoryType(), request.getSubType()))
                .sourceType(resolveSourceTypeOrDefault(request.getSourceType(), DEFAULT_SOURCE_TYPE))
                .title(resolveTitle(request.getTitle(), content))
                .content(content)
                .normalizedContentKey(normalizeContentKey(content))
                .reason(StringUtils.trimToNull(request.getReason()))
                .sourceMessageIds(normalizeSourceMessageIds(request.getSourceMessageIds()))
                .detailJson(StringUtils.defaultIfBlank(request.getDetailJson(), DEFAULT_DETAIL_JSON))
                .status(ACTIVE_MEMORY_STATUS)
                .confidenceScore(defaultDouble(request.getConfidenceScore(), DEFAULT_CONFIDENCE))
                .salienceScore(defaultDouble(request.getSalienceScore(), DEFAULT_SALIENCE))
                .accessCount(0)
                .useCount(0)
                .expiresAt(request.getExpiresAt())
                .createdAt(now)
                .updatedAt(now)
                .build();
        save(memory);
        rebuildEmbedding(memory);
        recordMemoryMutation(MemoryLogConstant.EVENT_MEMORY_MANUAL_CREATE, memory, null);
        return memory;
    }

    @Override
    public AiMemory updateMemory(Long memoryId, MemoryUpdateRequest request) {
        AiMemory memory = getByIdForCurrentUser(memoryId);
        String content = StringUtils.trimToEmpty(request.getContent());
        String targetScope = normalizeOrDefault(request.getScope(), StringUtils.defaultIfBlank(memory.getScope(), DEFAULT_SCOPE));
        validateManualScopeTransition(memory.getScope(), targetScope);
        memory.setScope(targetScope);
        if (!MemoryScopeEnum.WORKSPACE.matches(targetScope)) {
            memory.setWorkspaceLevel(null);
            memory.setWorkspaceContextKey(null);
        }
        memory.setMemoryType(resolveMemoryType(request.getMemoryType()));
        memory.setSubType(resolveMemorySubType(request.getMemoryType(), request.getSubType()));
        memory.setSourceType(resolveSourceTypeOrDefault(request.getSourceType(),
                StringUtils.defaultIfBlank(memory.getSourceType(), DEFAULT_SOURCE_TYPE)));
        memory.setTitle(resolveTitle(request.getTitle(), content));
        memory.setContent(content);
        memory.setNormalizedContentKey(normalizeContentKey(content));
        memory.setReason(StringUtils.trimToNull(request.getReason()));
        memory.setSourceMessageIds(normalizeSourceMessageIds(request.getSourceMessageIds()));
        memory.setDetailJson(StringUtils.defaultIfBlank(request.getDetailJson(), DEFAULT_DETAIL_JSON));
        memory.setConfidenceScore(defaultDouble(request.getConfidenceScore(),
                defaultDouble(memory.getConfidenceScore(), DEFAULT_CONFIDENCE)));
        memory.setSalienceScore(defaultDouble(request.getSalienceScore(),
                defaultDouble(memory.getSalienceScore(), DEFAULT_SALIENCE)));
        memory.setExpiresAt(request.getExpiresAt());
        memory.setUpdatedAt(LocalDateTime.now());
        updateById(memory);
        rebuildEmbedding(memory);
        recordMemoryMutation(MemoryLogConstant.EVENT_MEMORY_MANUAL_UPDATE, memory, null);
        return memory;
    }

    @Override
    public MemoryWriteResult writeAgentMemory(MemoryWriteRequest request) {
        Long userId = requireUserId();
        BusinessException.assertNotNull(request, "memory write request is required");
        String content = StringUtils.trimToEmpty(request.getContent());
        if (content.isBlank()) {
            throw BusinessException.badRequest("memory content is required");
        }

        String scope = normalizeOrDefault(request.getScope(), DEFAULT_SCOPE);
        WorkspaceBinding workspaceBinding = resolveAgentWorkspaceBinding(scope, request);
        String memoryType = resolveMemoryType(request.getMemoryType());
        String subType = resolveMemorySubType(request.getMemoryType(), request.getSubType());
        String normalizedContentKey = normalizeContentKey(content);
        BusinessException.assertTrue(StringUtils.isNotBlank(normalizedContentKey), "normalized content key is required");

        LambdaQueryWrapper<AiMemory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiMemory::getUserId, userId)
                .eq(AiMemory::getScope, scope)
                .eq(AiMemory::getMemoryType, memoryType)
                .eq(AiMemory::getNormalizedContentKey, normalizedContentKey)
                .eq(AiMemory::getStatus, ACTIVE_MEMORY_STATUS)
                .last("LIMIT 1");
        applyNullableStringCondition(wrapper, AiMemory::getWorkspaceContextKey, workspaceBinding.workspaceContextKey());
        applyNullableStringCondition(wrapper, AiMemory::getSubType, subType);
        AiMemory existing = getOne(wrapper, false);

        LocalDateTime now = LocalDateTime.now();
        boolean created = existing == null;
        if (existing == null) {
            existing = AiMemory.builder()
                    .userId(userId)
                    .conversationId(RequestContext.getConversationId())
                    .workspaceContextKey(workspaceBinding.workspaceContextKey())
                    .workspaceLevel(workspaceBinding.workspaceLevel())
                    .scope(scope)
                    .memoryType(memoryType)
                    .subType(subType)
                    .sourceType(AGENT_SOURCE_TYPE)
                    .title(resolveTitle(request.getTitle(), content))
                    .content(content)
                    .normalizedContentKey(normalizedContentKey)
                    .reason(StringUtils.trimToNull(request.getReason()))
                    .sourceMessageIds(normalizeSourceMessageIds(request.getSourceMessageIds()))
                    .detailJson(DEFAULT_DETAIL_JSON)
                    .status(ACTIVE_MEMORY_STATUS)
                    .confidenceScore(defaultDouble(request.getConfidence(), DEFAULT_CONFIDENCE))
                    .salienceScore(DEFAULT_SALIENCE)
                    .accessCount(0)
                    .useCount(0)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();
            save(existing);
        } else {
            existing.setConversationId(RequestContext.getConversationId());
            existing.setWorkspaceContextKey(workspaceBinding.workspaceContextKey());
            existing.setWorkspaceLevel(workspaceBinding.workspaceLevel());
            existing.setScope(scope);
            existing.setMemoryType(memoryType);
            existing.setSubType(subType);
            existing.setSourceType(AGENT_SOURCE_TYPE);
            existing.setTitle(resolveTitle(request.getTitle(), content));
            existing.setContent(content);
            existing.setNormalizedContentKey(normalizedContentKey);
            existing.setReason(StringUtils.trimToNull(request.getReason()));
            existing.setSourceMessageIds(normalizeSourceMessageIds(request.getSourceMessageIds()));
            existing.setConfidenceScore(defaultDouble(request.getConfidence(),
                    defaultDouble(existing.getConfidenceScore(), DEFAULT_CONFIDENCE)));
            existing.setUpdatedAt(now);
            updateById(existing);
        }

        rebuildEmbedding(existing);
        MemoryToolActionEnum action = created ? MemoryToolActionEnum.CREATED : MemoryToolActionEnum.UPDATED;
        recordMemoryMutation(MemoryLogConstant.EVENT_MEMORY_AGENT_WRITE, existing,
                AgentLogFields.of(MemoryLogConstant.FIELD_ACTION, action.getCode()));
        return MemoryWriteResult.builder()
                .memory(existing)
                .action(action)
                .build();
    }

    @Override
    public AiMemory archiveMemory(Long memoryId) {
        AiMemory memory = getByIdForCurrentUser(memoryId);
        memory.setStatus(MemoryStatusEnum.ARCHIVED.getCode());
        memory.setArchivedAt(LocalDateTime.now());
        memory.setUpdatedAt(LocalDateTime.now());
        updateById(memory);
        removeEmbeddingQuietly(memory.getId());
        recordMemoryMutation(MemoryLogConstant.EVENT_MEMORY_ARCHIVE, memory, null);
        return memory;
    }

    @Override
    public AiMemory restoreMemory(Long memoryId) {
        AiMemory memory = getByIdForCurrentUser(memoryId);
        memory.setStatus(ACTIVE_MEMORY_STATUS);
        memory.setArchivedAt(null);
        memory.setUpdatedAt(LocalDateTime.now());
        updateById(memory);
        rebuildEmbedding(memory);
        recordMemoryMutation(MemoryLogConstant.EVENT_MEMORY_RESTORE, memory, null);
        return memory;
    }

    @Override
    public void deleteMemory(Long memoryId) {
        AiMemory memory = getByIdForCurrentUser(memoryId);
        removeById(memory.getId());
        removeEmbeddingQuietly(memory.getId());
        recordMemoryMutation(MemoryLogConstant.EVENT_MEMORY_DELETE, memory, null);
    }

    @Override
    public MemoryMaintenanceReport inspectCurrentUserMaintenance() {
        Long userId = requireUserId();
        MemoryMaintenanceReport report = inspectMaintenance(userId, LocalDateTime.now());
        recordMaintenanceEvent(MemoryLogConstant.EVENT_MEMORY_MAINTENANCE_INSPECT, userId, report);
        return report;
    }

    @Override
    public MemoryMaintenanceReport runCurrentUserMaintenance() {
        Long userId = requireUserId();
        MemoryMaintenanceReport report = executeMaintenance(userId, LocalDateTime.now());
        recordMaintenanceEvent(MemoryLogConstant.EVENT_MEMORY_MAINTENANCE_RUN, userId, report);
        return report;
    }

    @Override
    public MemoryMaintenanceReport runGlobalMaintenance() {
        MemoryMaintenanceReport report = executeMaintenance(null, LocalDateTime.now());
        recordMaintenanceEvent(MemoryLogConstant.EVENT_MEMORY_MAINTENANCE_RUN, null, report);
        return report;
    }

    @Override
    public void recordMemoryAccess(List<Long> memoryIds) {
        int processedCount = touchMemories(memoryIds, false);
        recordTouchEvent(MemoryLogConstant.EVENT_MEMORY_ACCESS_RECORDED, memoryIds, processedCount);
    }

    @Override
    public void recordMemoryUsage(List<Long> memoryIds) {
        int processedCount = touchMemories(memoryIds, true);
        recordTouchEvent(MemoryLogConstant.EVENT_MEMORY_USAGE_RECORDED, memoryIds, processedCount);
    }

    @Override
    public boolean removeById(Serializable id) {
        return super.removeById(id);
    }

    private MemorySearchResult toMemorySearchResult(dev.langchain4j.store.embedding.EmbeddingMatch<TextSegment> match) {
        TextSegment segment = match.embedded();
        Metadata metadata = segment.metadata();

        return MemorySearchResult.builder()
                .id(metadata.getLong(MemoryMetadataConstant.MEMORY_ID))
                .scope(blankToNull(metadata.getString(MemoryMetadataConstant.SCOPE)))
                .workspaceLevel(blankToNull(metadata.getString(MemoryMetadataConstant.WORKSPACE_LEVEL)))
                .workspaceContextKey(blankToNull(metadata.getString(MemoryMetadataConstant.WORKSPACE_CONTEXT_KEY)))
                .memoryType(metadata.getString(MemoryMetadataConstant.MEMORY_TYPE))
                .subType(blankToNull(metadata.getString(MemoryMetadataConstant.SUB_TYPE)))
                .title(blankToNull(metadata.getString(MemoryMetadataConstant.TITLE)))
                .content(segment.text())
                .normalizedContentKey(blankToNull(metadata.getString(MemoryMetadataConstant.NORMALIZED_CONTENT_KEY)))
                .reason(blankToNull(metadata.getString(MemoryMetadataConstant.REASON)))
                .sourceType(blankToNull(metadata.getString(MemoryMetadataConstant.SOURCE_TYPE)))
                .score(match.score())
                .conversationId(metadata.getLong(MemoryMetadataConstant.CONVERSATION_ID))
                .updatedAt(parseDateTime(metadata.getString(MemoryMetadataConstant.UPDATED_AT)))
                .build();
    }

    private MemorySearchResult toMemorySearchResult(AiMemory memory, double score) {
        return MemorySearchResult.builder()
                .id(memory.getId())
                .scope(memory.getScope())
                .workspaceLevel(memory.getWorkspaceLevel())
                .workspaceContextKey(memory.getWorkspaceContextKey())
                .memoryType(memory.getMemoryType())
                .subType(memory.getSubType())
                .title(memory.getTitle())
                .content(memory.getContent())
                .normalizedContentKey(memory.getNormalizedContentKey())
                .reason(memory.getReason())
                .sourceType(memory.getSourceType())
                .score(score)
                .conversationId(memory.getConversationId())
                .updatedAt(memory.getUpdatedAt())
                .build();
    }

    private MemoryMaintenanceReport inspectMaintenance(Long userId, LocalDateTime now) {
        List<AiMemory> memories = listMemoriesForMaintenance(userId);
        List<AiMemory> activeMemories = memories.stream()
                .filter(this::isActiveMemory)
                .toList();

        return MemoryMaintenanceReport.builder()
                .generatedAt(now)
                .activeMemoryCount((int) activeMemories.size())
                .archivedMemoryCount((int) memories.stream().filter(this::isArchivedMemory).count())
                .hiddenMemoryCount((int) memories.stream().filter(this::isHiddenMemory).count())
                .expiredActiveMemoryCount(findExpiredActiveMemories(activeMemories, now).size())
                .duplicateActiveMemoryCount(findDuplicateLosers(activeMemories).size())
                .processedArchivedCount(0)
                .processedHiddenCount(0)
                .build();
    }

    private MemoryMaintenanceReport executeMaintenance(Long userId, LocalDateTime now) {
        List<AiMemory> memories = listMemoriesForMaintenance(userId);

        int archivedCount = 0;
        int hiddenCount = 0;

        if (memoryProperties.getMaintenance().isArchiveExpiredEnabled()) {
            List<AiMemory> expiredActiveMemories = findExpiredActiveMemories(memories.stream()
                    .filter(this::isActiveMemory)
                    .toList(), now);
            for (AiMemory memory : expiredActiveMemories) {
                archiveMemoryForMaintenance(memory, now);
                archivedCount++;
            }
            memories = listMemoriesForMaintenance(userId);
        }

        if (memoryProperties.getMaintenance().isHideDuplicateEnabled()) {
            List<AiMemory> duplicateLosers = findDuplicateLosers(memories.stream()
                    .filter(this::isActiveMemory)
                    .toList());
            for (AiMemory duplicate : duplicateLosers) {
                hideMemoryForMaintenance(duplicate, now);
                hiddenCount++;
            }
            memories = listMemoriesForMaintenance(userId);
        }

        List<AiMemory> activeMemories = memories.stream()
                .filter(this::isActiveMemory)
                .toList();

        MemoryMaintenanceReport report = MemoryMaintenanceReport.builder()
                .generatedAt(now)
                .activeMemoryCount((int) activeMemories.size())
                .archivedMemoryCount((int) memories.stream().filter(this::isArchivedMemory).count())
                .hiddenMemoryCount((int) memories.stream().filter(this::isHiddenMemory).count())
                .expiredActiveMemoryCount(findExpiredActiveMemories(activeMemories, now).size())
                .duplicateActiveMemoryCount(findDuplicateLosers(activeMemories).size())
                .processedArchivedCount(archivedCount)
                .processedHiddenCount(hiddenCount)
                .build();
        log.info("Memory maintenance summary: userId={}, archivedProcessed={}, hiddenProcessed={}, activeCount={}, archivedCount={}, hiddenCount={}, expiredActiveCount={}, duplicateActiveCount={}",
                userId,
                report.getProcessedArchivedCount(),
                report.getProcessedHiddenCount(),
                report.getActiveMemoryCount(),
                report.getArchivedMemoryCount(),
                report.getHiddenMemoryCount(),
                report.getExpiredActiveMemoryCount(),
                report.getDuplicateActiveMemoryCount());
        return report;
    }

    private void rebuildEmbedding(AiMemory memory) {
        removeEmbeddingQuietly(memory.getId());
        if (!Objects.equals(memory.getStatus(), ACTIVE_MEMORY_STATUS) || StringUtils.isBlank(memory.getContent())) {
            return;
        }
        try {
            Embedding embedding = embeddingModel.embed(memory.getContent()).content();
            Metadata metadata = new Metadata()
                    .put(MemoryMetadataConstant.USER_ID, memory.getUserId())
                    .put(MemoryMetadataConstant.STATUS, memory.getStatus())
                    .put(MemoryMetadataConstant.SCOPE, StringUtils.defaultString(memory.getScope()))
                    .put(MemoryMetadataConstant.WORKSPACE_LEVEL, StringUtils.defaultString(memory.getWorkspaceLevel()))
                    .put(MemoryMetadataConstant.WORKSPACE_CONTEXT_KEY, StringUtils.defaultString(memory.getWorkspaceContextKey()))
                    .put(MemoryMetadataConstant.MEMORY_TYPE, memory.getMemoryType())
                    .put(MemoryMetadataConstant.SUB_TYPE, StringUtils.defaultString(memory.getSubType()))
                    .put(MemoryMetadataConstant.TITLE, StringUtils.defaultString(memory.getTitle()))
                    .put(MemoryMetadataConstant.NORMALIZED_CONTENT_KEY, StringUtils.defaultString(memory.getNormalizedContentKey()))
                    .put(MemoryMetadataConstant.REASON, StringUtils.defaultString(memory.getReason()))
                    .put(MemoryMetadataConstant.SOURCE_TYPE, StringUtils.defaultString(memory.getSourceType()))
                    .put(MemoryMetadataConstant.UPDATED_AT, memory.getUpdatedAt() == null ? "" : memory.getUpdatedAt().toString())
                    .put(MemoryMetadataConstant.CONVERSATION_ID, memory.getConversationId())
                    .put(MemoryMetadataConstant.MEMORY_ID, memory.getId());
            memoryEmbeddingStore.addAll(
                    List.of(String.valueOf(memory.getId())),
                    List.of(embedding),
                    List.of(TextSegment.from(memory.getContent(), metadata)));
        } catch (Exception e) {
            log.warn("Failed to rebuild memory embedding for memory {}", memory.getId(), e);
            agentLogService.recordError(AgentLogType.DEBUG_EVENT,
                    MemoryLogConstant.LOGGER_NAME,
                    MemoryLogConstant.EVENT_MEMORY_EMBEDDING_REBUILD_FAILED,
                    e,
                    AgentLogFields.of(
                            MemoryLogConstant.FIELD_USER_ID, memory.getUserId(),
                            MemoryLogConstant.FIELD_MEMORY_ID, memory.getId(),
                            MemoryLogConstant.FIELD_SCOPE, memory.getScope(),
                            MemoryLogConstant.FIELD_MEMORY_TYPE, memory.getMemoryType(),
                            MemoryLogConstant.FIELD_SUB_TYPE, memory.getSubType()
                    ));
        }
    }

    private void removeEmbeddingQuietly(Long memoryId) {
        if (memoryId == null) {
            return;
        }
        try {
            memoryEmbeddingStore.remove(String.valueOf(memoryId));
        } catch (Exception e) {
            log.warn("Failed to remove memory embedding for memory {}", memoryId, e);
            agentLogService.recordError(AgentLogType.DEBUG_EVENT,
                    MemoryLogConstant.LOGGER_NAME,
                    MemoryLogConstant.EVENT_MEMORY_EMBEDDING_REMOVE_FAILED,
                    e,
                    AgentLogFields.of(MemoryLogConstant.FIELD_MEMORY_ID, memoryId));
        }
    }

    private Long requireUserId() {
        Long userId = RequestContext.getUserId();
        BusinessException.assertNotNull(userId, "error.not.login");
        return userId;
    }

    private String normalizeOrDefault(String value, String defaultValue) {
        return StringUtils.defaultIfBlank(value, defaultValue).trim().toUpperCase();
    }

    private String normalizeWorkspaceLevel(String value) {
        MemoryWorkspaceLevelEnum level = MemoryWorkspaceLevelEnum.fromCode(value);
        return level == null ? null : level.getCode();
    }

    private WorkspaceBinding resolveAgentWorkspaceBinding(String scope, MemoryWriteRequest request) {
        if (!MemoryScopeEnum.WORKSPACE.matches(scope)) {
            BusinessException.assertTrue(StringUtils.isBlank(request.getWorkspaceLevel()),
                    "workspaceLevel is only allowed for WORKSPACE scope.");
            BusinessException.assertTrue(request.getWorkspaceConnectionId() == null,
                    "workspaceConnectionId is only allowed for WORKSPACE scope.");
            BusinessException.assertTrue(StringUtils.isBlank(request.getWorkspaceCatalogName()),
                    "workspaceCatalogName is only allowed for WORKSPACE scope.");
            BusinessException.assertTrue(StringUtils.isBlank(request.getWorkspaceSchemaName()),
                    "workspaceSchemaName is only allowed for WORKSPACE scope.");
            return new WorkspaceBinding(null, null);
        }

        MemoryWorkspaceLevelEnum level = MemoryWorkspaceLevelEnum.fromCode(request.getWorkspaceLevel());
        BusinessException.assertTrue(level != null, "workspaceLevel is required for WORKSPACE scope.");
        return new WorkspaceBinding(level.getCode(), resolveWorkspaceContextKey(
                level,
                request.getWorkspaceConnectionId(),
                request.getWorkspaceCatalogName(),
                request.getWorkspaceSchemaName()));
    }

    private String resolveWorkspaceContextKey(MemoryWorkspaceLevelEnum workspaceLevel,
                                             Long workspaceConnectionId,
                                             String workspaceCatalogName,
                                             String workspaceSchemaName) {
        return switch (workspaceLevel) {
            case GLOBAL -> DEFAULT_WORKSPACE_CONTEXT_KEY;
            case CONNECTION -> {
                BusinessException.assertTrue(workspaceConnectionId != null,
                        "workspaceConnectionId is required for WORKSPACE level CONNECTION.");
                yield String.valueOf(workspaceConnectionId);
            }
            case CATALOG -> {
                BusinessException.assertTrue(workspaceConnectionId != null,
                        "workspaceConnectionId is required for WORKSPACE level CATALOG.");
                String catalog = StringUtils.trimToNull(workspaceCatalogName);
                BusinessException.assertTrue(StringUtils.isNotBlank(catalog),
                        "workspaceCatalogName is required for WORKSPACE level CATALOG.");
                yield workspaceConnectionId + ":" + catalog;
            }
            case SCHEMA -> {
                BusinessException.assertTrue(workspaceConnectionId != null,
                        "workspaceConnectionId is required for WORKSPACE level SCHEMA.");
                String catalog = StringUtils.trimToNull(workspaceCatalogName);
                String schema = StringUtils.trimToNull(workspaceSchemaName);
                BusinessException.assertTrue(StringUtils.isNotBlank(catalog),
                        "workspaceCatalogName is required for WORKSPACE level SCHEMA.");
                BusinessException.assertTrue(StringUtils.isNotBlank(schema),
                        "workspaceSchemaName is required for WORKSPACE level SCHEMA.");
                yield workspaceConnectionId + ":" + catalog + ":" + schema;
            }
        };
    }

    private List<String> currentWorkspaceContextChain() {
        List<String> chain = new ArrayList<>();
        String connection = RequestContext.getConnectionId() == null ? null : String.valueOf(RequestContext.getConnectionId());
        String catalog = StringUtils.trimToNull(RequestContext.getCatalog());
        String schema = StringUtils.trimToNull(RequestContext.getSchema());

        if (StringUtils.isNotBlank(connection) && StringUtils.isNotBlank(catalog) && StringUtils.isNotBlank(schema)) {
            chain.add(connection + ":" + catalog + ":" + schema);
        }
        if (StringUtils.isNotBlank(connection) && StringUtils.isNotBlank(catalog)) {
            chain.add(connection + ":" + catalog);
        }
        if (StringUtils.isNotBlank(connection)) {
            chain.add(connection);
        }
        chain.add(DEFAULT_WORKSPACE_CONTEXT_KEY);
        return chain.stream().distinct().toList();
    }

    private String normalizeContentKey(String value) {
        return StringUtils.normalizeSpace(StringUtils.defaultString(value))
                .replaceAll("[\\p{Punct}]", " ")
                .trim()
                .toLowerCase();
    }

    private String resolveMemoryType(String value) {
        MemoryTypeEnum memoryType = MemoryTypeEnum.fromCode(value);
        if (memoryType == null) {
            throw BusinessException.badRequest("Unsupported memoryType '%s'. Valid values: %s",
                    value, MemoryTypeEnum.validCodes());
        }
        return memoryType.getCode();
    }

    private String resolveMemorySubType(String memoryTypeValue, String subTypeValue) {
        if (StringUtils.isBlank(subTypeValue)) {
            throw BusinessException.badRequest("subType is required");
        }
        MemoryTypeEnum memoryType = MemoryTypeEnum.fromCode(resolveMemoryType(memoryTypeValue));
        MemorySubTypeEnum subType = MemorySubTypeEnum.fromCode(subTypeValue);
        if (subType == null) {
            throw BusinessException.badRequest("Unsupported subType '%s'. Valid values: %s",
                    subTypeValue, MemorySubTypeEnum.validCodes());
        }
        if (!subType.belongsTo(memoryType)) {
            throw BusinessException.badRequest("subType '%s' does not belong to memoryType '%s'. Valid subTypes: %s",
                    subTypeValue,
                    memoryType.getCode(),
                    MemorySubTypeEnum.validCodesForText(memoryType));
        }
        return subType.getCode();
    }

    private String resolveSourceType(String value) {
        MemorySourceTypeEnum sourceType = MemorySourceTypeEnum.fromCode(value);
        if (sourceType == null) {
            throw BusinessException.badRequest("Unsupported sourceType '%s'. Valid values: %s",
                    value, MemorySourceTypeEnum.validCodes());
        }
        return sourceType.getCode();
    }

    private String resolveSourceTypeOrDefault(String value, String defaultValue) {
        return resolveSourceType(StringUtils.defaultIfBlank(value, defaultValue));
    }

    private void recordMemoryMutation(String eventName, AiMemory memory, Map<String, Object> extraFields) {
        if (memory == null) {
            return;
        }
        Map<String, Object> fields = AgentLogFields.of(
                MemoryLogConstant.FIELD_MEMORY_ID, memory.getId(),
                MemoryLogConstant.FIELD_USER_ID, memory.getUserId(),
                MemoryLogConstant.FIELD_CONVERSATION_ID, memory.getConversationId(),
                MemoryLogConstant.FIELD_SCOPE, memory.getScope(),
                MemoryLogConstant.FIELD_WORKSPACE_LEVEL, memory.getWorkspaceLevel(),
                MemoryLogConstant.FIELD_WORKSPACE_CONTEXT_KEY, memory.getWorkspaceContextKey(),
                MemoryLogConstant.FIELD_MEMORY_TYPE, memory.getMemoryType(),
                MemoryLogConstant.FIELD_SUB_TYPE, memory.getSubType(),
                MemoryLogConstant.FIELD_SOURCE_TYPE, memory.getSourceType(),
                MemoryLogConstant.FIELD_STATUS, memory.getStatus());
        if (extraFields != null && !extraFields.isEmpty()) {
            fields.putAll(extraFields);
        }
        agentLogService.recordDebug(MemoryLogConstant.LOGGER_NAME, eventName, fields);
    }

    private void recordMaintenanceEvent(String eventName, Long userId, MemoryMaintenanceReport report) {
        if (report == null) {
            return;
        }
        agentLogService.recordDebug(MemoryLogConstant.LOGGER_NAME, eventName,
                AgentLogFields.of(
                        MemoryLogConstant.FIELD_USER_ID, userId,
                        MemoryLogConstant.FIELD_ACTIVE_MEMORY_COUNT, report.getActiveMemoryCount(),
                        MemoryLogConstant.FIELD_ARCHIVED_MEMORY_COUNT, report.getArchivedMemoryCount(),
                        MemoryLogConstant.FIELD_HIDDEN_MEMORY_COUNT, report.getHiddenMemoryCount(),
                        MemoryLogConstant.FIELD_EXPIRED_ACTIVE_MEMORY_COUNT, report.getExpiredActiveMemoryCount(),
                        MemoryLogConstant.FIELD_DUPLICATE_ACTIVE_MEMORY_COUNT, report.getDuplicateActiveMemoryCount(),
                        MemoryLogConstant.FIELD_PROCESSED_ARCHIVED_COUNT, report.getProcessedArchivedCount(),
                        MemoryLogConstant.FIELD_PROCESSED_HIDDEN_COUNT, report.getProcessedHiddenCount()));
    }

    private void recordTouchEvent(String eventName, List<Long> memoryIds, int processedCount) {
        List<Long> uniqueIds = memoryIds == null ? List.of() : memoryIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        agentLogService.recordDebug(MemoryLogConstant.LOGGER_NAME, eventName,
                AgentLogFields.of(
                        MemoryLogConstant.FIELD_USER_ID, RequestContext.getUserId(),
                        MemoryLogConstant.FIELD_MEMORY_IDS, uniqueIds,
                        MemoryLogConstant.FIELD_PROCESSED_COUNT, processedCount));
    }

    private String normalizeSourceMessageIds(List<String> sourceMessageIds) {
        if (sourceMessageIds == null || sourceMessageIds.isEmpty()) {
            return MemoryConstant.EMPTY_SOURCE_MESSAGE_IDS_JSON;
        }
        List<String> sanitized = sourceMessageIds.stream()
                .map(StringUtils::trimToNull)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        return JsonUtil.object2json(sanitized);
    }

    private String normalizeSourceMessageIds(String sourceMessageIds) {
        if (StringUtils.isBlank(sourceMessageIds)) {
            return MemoryConstant.EMPTY_SOURCE_MESSAGE_IDS_JSON;
        }
        try {
            List<String> values = JsonUtil.json2Object(sourceMessageIds, new TypeReference<List<String>>() {
            });
            return normalizeSourceMessageIds(values);
        } catch (RuntimeException ignore) {
            List<String> values = java.util.Arrays.stream(sourceMessageIds.split(","))
                    .map(StringUtils::trimToNull)
                    .filter(Objects::nonNull)
                    .toList();
            return normalizeSourceMessageIds(values);
        }
    }

    private String resolveTitle(String title, String content) {
        String trimmedTitle = StringUtils.trimToNull(title);
        if (trimmedTitle != null) {
            return trimmedTitle;
        }
        String normalized = PromptSafeText.normalizeTitle(content);
        return normalized.length() <= 120 ? normalized : normalized.substring(0, 120) + "...";
    }

    private double defaultDouble(Double value, double defaultValue) {
        return value == null ? defaultValue : value;
    }

    private boolean isVisibleToCurrentContext(MemorySearchResult result,
                                              Long conversationId,
                                              List<String> workspaceChain) {
        String scope = normalizeOrDefault(result.getScope(), DEFAULT_SCOPE);
        if (MemoryScopeEnum.USER.matches(scope)) {
            return true;
        }
        if (MemoryScopeEnum.CONVERSATION.matches(scope)) {
            return conversationId != null && Objects.equals(conversationId, result.getConversationId());
        }
        if (MemoryScopeEnum.WORKSPACE.matches(scope)) {
            return StringUtils.isNotBlank(result.getWorkspaceContextKey())
                    && workspaceChain.contains(result.getWorkspaceContextKey());
        }
        return false;
    }

    private Comparator<MemorySearchResult> promptMemoryComparator() {
        return Comparator
                .comparingInt(this::scopePriority)
                .thenComparing(Comparator.comparingInt(this::workspacePriority).reversed())
                .thenComparing(Comparator.comparingDouble(MemorySearchResult::getScore).reversed())
                .thenComparing(MemorySearchResult::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(MemorySearchResult::getId, Comparator.nullsLast(Comparator.reverseOrder()));
    }

    private int scopePriority(MemorySearchResult result) {
        MemoryScopeEnum scope = MemoryScopeEnum.fromCode(normalizeOrDefault(result.getScope(), DEFAULT_SCOPE));
        return scope == null ? Integer.MAX_VALUE : scope.getPriority();
    }

    private int workspacePriority(MemorySearchResult result) {
        MemoryWorkspaceLevelEnum level = MemoryWorkspaceLevelEnum.fromCode(result.getWorkspaceLevel());
        return level == null ? -1 : level.getPriority();
    }

    private void validateManualScopeTransition(String currentScope, String targetScope) {
        String normalizedCurrentScope = normalizeOrDefault(currentScope, DEFAULT_SCOPE);
        if (MemoryScopeEnum.WORKSPACE.matches(normalizedCurrentScope) && !MemoryScopeEnum.WORKSPACE.matches(targetScope)) {
            throw BusinessException.badRequest("Manual editing cannot move WORKSPACE memories to another scope.");
        }
        if (!MemoryScopeEnum.WORKSPACE.matches(normalizedCurrentScope) && MemoryScopeEnum.WORKSPACE.matches(targetScope)) {
            throw BusinessException.badRequest("Manual memory editing cannot convert records into WORKSPACE scope.");
        }
    }

    private void applyNullableStringCondition(LambdaQueryWrapper<AiMemory> wrapper,
                                              SFunction<AiMemory, ?> column,
                                              String value) {
        if (StringUtils.isBlank(value)) {
            wrapper.isNull(column);
            return;
        }
        wrapper.eq(column, value);
    }

    private String blankToNull(String value) {
        return StringUtils.trimToNull(value);
    }

    private LocalDateTime parseDateTime(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        try {
            return LocalDateTime.parse(value);
        } catch (RuntimeException ignore) {
            return null;
        }
    }

    private int resolveRecallCandidateLimit(Long userId) {
        LambdaQueryWrapper<AiMemory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiMemory::getUserId, userId)
                .eq(AiMemory::getStatus, ACTIVE_MEMORY_STATUS);
        long candidateCount = count(wrapper);
        if (candidateCount <= 0) {
            return 1;
        }
        return candidateCount >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) candidateCount;
    }

    private List<MemorySearchResult> listVisibleActiveMemories(Long userId,
                                                               Long conversationId,
                                                               List<String> workspaceChain,
                                                               String scope) {
        LambdaQueryWrapper<AiMemory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiMemory::getUserId, userId)
                .eq(AiMemory::getStatus, ACTIVE_MEMORY_STATUS)
                .orderByDesc(AiMemory::getUpdatedAt)
                .orderByDesc(AiMemory::getId);
        if (StringUtils.isNotBlank(scope)) {
            wrapper.eq(AiMemory::getScope, scope);
        }
        return list(wrapper).stream()
                .map(memory -> toMemorySearchResult(memory, 0.0D))
                .filter(result -> matchesScope(result, scope))
                .filter(result -> isVisibleToCurrentContext(result, conversationId, workspaceChain))
                .sorted(promptMemoryComparator())
                .toList();
    }

    private boolean matchesScope(MemorySearchResult result, String scope) {
        return StringUtils.isBlank(scope) || StringUtils.equalsIgnoreCase(scope, result.getScope());
    }

    protected List<AiMemory> listMemoriesForMaintenance(Long userId) {
        LambdaQueryWrapper<AiMemory> wrapper = new LambdaQueryWrapper<>();
        if (userId != null) {
            wrapper.eq(AiMemory::getUserId, userId);
        }
        wrapper.orderByDesc(AiMemory::getUpdatedAt).orderByDesc(AiMemory::getId);
        return list(wrapper);
    }

    protected void persistMaintenanceMemory(AiMemory memory) {
        updateById(memory);
    }

    private void archiveMemoryForMaintenance(AiMemory memory, LocalDateTime now) {
        memory.setStatus(MemoryStatusEnum.ARCHIVED.getCode());
        memory.setArchivedAt(now);
        memory.setUpdatedAt(now);
        persistMaintenanceMemory(memory);
        removeEmbeddingQuietly(memory.getId());
    }

    private void hideMemoryForMaintenance(AiMemory memory, LocalDateTime now) {
        memory.setStatus(MemoryStatusEnum.HIDDEN.getCode());
        memory.setUpdatedAt(now);
        persistMaintenanceMemory(memory);
        removeEmbeddingQuietly(memory.getId());
    }

    private List<AiMemory> findExpiredActiveMemories(List<AiMemory> activeMemories, LocalDateTime now) {
        return activeMemories.stream()
                .filter(memory -> memory.getExpiresAt() != null && memory.getExpiresAt().isBefore(now))
                .toList();
    }

    private List<AiMemory> findDuplicateLosers(List<AiMemory> activeMemories) {
        Map<String, List<AiMemory>> groups = activeMemories.stream()
                .filter(memory -> StringUtils.isNotBlank(memory.getContent()))
                .collect(Collectors.groupingBy(this::duplicateKey, LinkedHashMap::new, Collectors.toList()));

        return groups.values().stream()
                .filter(group -> group.size() > 1)
                .flatMap(group -> group.stream()
                        .sorted(memoryMaintenanceComparator())
                        .skip(1))
                .toList();
    }

    private Comparator<AiMemory> memoryMaintenanceComparator() {
        return Comparator.comparing(AiMemory::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(AiMemory::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(AiMemory::getId, Comparator.nullsLast(Comparator.reverseOrder()));
    }

    private String duplicateKey(AiMemory memory) {
        return StringUtils.defaultString(memory.getUserId() == null ? null : String.valueOf(memory.getUserId()))
                + "|" + StringUtils.defaultString(memory.getWorkspaceContextKey())
                + "|" + StringUtils.defaultString(memory.getScope())
                + "|" + StringUtils.defaultString(memory.getMemoryType())
                + "|" + StringUtils.defaultString(memory.getSubType())
                + "|" + StringUtils.defaultString(memory.getNormalizedContentKey(), normalizeDuplicateContent(memory.getContent()));
    }

    private String normalizeDuplicateContent(String value) {
        return StringUtils.normalizeSpace(StringUtils.defaultString(value)).toLowerCase();
    }

    private boolean isActiveMemory(AiMemory memory) {
        return Objects.equals(memory.getStatus(), MemoryStatusEnum.ACTIVE.getCode());
    }

    private boolean isArchivedMemory(AiMemory memory) {
        return Objects.equals(memory.getStatus(), MemoryStatusEnum.ARCHIVED.getCode());
    }

    private boolean isHiddenMemory(AiMemory memory) {
        return Objects.equals(memory.getStatus(), MemoryStatusEnum.HIDDEN.getCode());
    }

    private int touchMemories(List<Long> memoryIds, boolean markUse) {
        Long userId = RequestContext.getUserId();
        if (userId == null || memoryIds == null || memoryIds.isEmpty()) {
            return 0;
        }
        LocalDateTime now = LocalDateTime.now();
        List<Long> uniqueIds = new ArrayList<>(new LinkedHashSet<>(memoryIds.stream()
                .filter(Objects::nonNull)
                .toList()));
        int processedCount = 0;
        for (Long memoryId : uniqueIds) {
            AiMemory memory = getById(memoryId);
            if (memory == null || !userId.equals(memory.getUserId())) {
                continue;
            }
            if (markUse) {
                memory.setUseCount(defaultInt(memory.getUseCount(), 0) + 1);
                memory.setLastUsedAt(now);
            } else {
                memory.setAccessCount(defaultInt(memory.getAccessCount(), 0) + 1);
                memory.setLastAccessedAt(now);
            }
            updateById(memory);
            processedCount++;
        }
        return processedCount;
    }

    private int defaultInt(Integer value, int defaultValue) {
        return value == null ? defaultValue : value;
    }

    private static final class PromptSafeText {

        private PromptSafeText() {
        }

        private static String normalizeTitle(String value) {
            String normalized = StringUtils.normalizeSpace(StringUtils.defaultString(value));
            return normalized.isBlank() ? MemoryConstant.UNTITLED_MEMORY : normalized;
        }
    }

    private record WorkspaceBinding(String workspaceLevel, String workspaceContextKey) {
    }
}
