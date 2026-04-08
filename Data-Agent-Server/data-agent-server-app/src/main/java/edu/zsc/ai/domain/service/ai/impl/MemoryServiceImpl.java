package edu.zsc.ai.domain.service.ai.impl;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

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
import edu.zsc.ai.common.converter.ai.MemoryMutationConverter;
import edu.zsc.ai.common.constant.MemoryLogConstant;
import edu.zsc.ai.common.constant.MemoryMetadataConstant;
import edu.zsc.ai.common.constant.MemoryRecallLogConstant;
import edu.zsc.ai.common.constant.MemoryRecallPlanningConstant;
import edu.zsc.ai.common.enums.ai.MemoryScopeEnum;
import edu.zsc.ai.common.enums.ai.MemorySourceTypeEnum;
import edu.zsc.ai.common.enums.ai.MemorySubTypeEnum;
import edu.zsc.ai.common.enums.ai.MemoryOperationEnum;
import edu.zsc.ai.common.enums.ai.MemoryToolActionEnum;
import edu.zsc.ai.common.enums.ai.MemoryTypeEnum;
import edu.zsc.ai.common.enums.ai.MemoryEnableEnum;
import edu.zsc.ai.config.ai.MemoryProperties;
import edu.zsc.ai.context.RequestContext;
import edu.zsc.ai.domain.exception.BusinessException;
import edu.zsc.ai.domain.mapper.ai.AiMemoryMapper;
import edu.zsc.ai.domain.model.dto.request.ai.MemoryCreateRequest;
import edu.zsc.ai.domain.model.dto.request.ai.MemoryUpdateRequest;
import edu.zsc.ai.domain.model.dto.request.ai.MemoryMutationRequest;
import edu.zsc.ai.domain.model.dto.request.base.PageRequest;
import edu.zsc.ai.domain.model.dto.response.base.PageResponse;
import edu.zsc.ai.domain.model.entity.ai.AiConversationMemoryCursor;
import edu.zsc.ai.domain.model.entity.ai.AiMemory;
import edu.zsc.ai.domain.service.ai.AiConversationMemoryCursorService;
import edu.zsc.ai.domain.service.ai.MemoryService;
import edu.zsc.ai.domain.service.ai.model.MemorySearchResult;
import edu.zsc.ai.domain.service.ai.model.MemoryWriteContext.MemorySummary;
import edu.zsc.ai.domain.service.ai.model.MemoryWriteItem;
import edu.zsc.ai.domain.service.ai.model.MemoryWriteResult;
import edu.zsc.ai.domain.service.ai.recall.MemoryRecallMode;
import edu.zsc.ai.domain.service.ai.recall.MemoryRecallQuery;
import edu.zsc.ai.domain.service.ai.recall.MemoryRecallQueryStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemoryServiceImpl extends ServiceImpl<AiMemoryMapper, AiMemory> implements MemoryService {

    private static final Logger runtimeLog = LoggerFactory.getLogger(MemoryLogConstant.LOGGER_NAME);


    private static final int ENABLED_MEMORY_VALUE = MemoryEnableEnum.ENABLE.getCode();
    private static final int CONVERSATION_FALLBACK_LIMIT = 5;
    private static final String DEFAULT_SCOPE = MemoryConstant.DEFAULT_SCOPE;
    private static final String DEFAULT_SOURCE_TYPE = MemorySourceTypeEnum.MANUAL.getCode();
    private static final String AGENT_SOURCE_TYPE = MemorySourceTypeEnum.AGENT.getCode();

    private final EmbeddingStore<TextSegment> memoryEmbeddingStore;
    private final EmbeddingModel embeddingModel;
    private final MemoryProperties memoryProperties;
    private final AiConversationMemoryCursorService cursorService;

    @Override
    public List<MemorySearchResult> searchEnabledMemories(String queryText, int limit, double minScore, String memoryType, String scope) {
        int safeLimit = Math.max(1, limit);
        List<MemorySearchResult> results = recallAccessibleMemories(new MemoryRecallQuery(
                        MemoryRecallPlanningConstant.LEGACY_QUERY_NAME,
                        MemoryRecallPlanningConstant.LEGACY_PLANNING_REASON,
                        scope,
                        RequestContext.getConversationId(),
                        queryText,
                        StringUtils.isBlank(memoryType) ? null : resolveMemoryType(memoryType),
                        null,
                        minScore,
                        MemoryRecallMode.PROMPT,
                        MemoryRecallQueryStrategy.SEMANTIC,
                        0)).stream()
                .limit(safeLimit)
                .toList();
        logRuntimeInfo(MemoryLogConstant.EVENT_MEMORY_SEARCH,
                fieldsOf(
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
        String normalizedScope = StringUtils.isBlank(query.targetScope()) ? null : query.targetScope().trim().toUpperCase();
        String queryText = query.queryText();
        double minScore = query.minScore() == null ? 0.0D : query.minScore();
        String normalizedMemoryType = StringUtils.isBlank(query.memoryType()) ? null : query.memoryType().trim().toUpperCase();
        String normalizedSubType = StringUtils.isBlank(query.subType()) ? null : query.subType().trim().toUpperCase();
        MemoryRecallQueryStrategy strategy = query.queryStrategy() == null ? MemoryRecallQueryStrategy.HYBRID : query.queryStrategy();

        RecallExecutionResult executionResult = recallByStrategy(
                strategy, userId, conversationId, normalizedScope, normalizedMemoryType, normalizedSubType, queryText, minScore, query.recallMode());
        List<MemorySearchResult> annotatedResults = annotateRecallResults(
                executionResult.results(), strategy, executionResult.executionPath(), executionResult.usedFallback());
        recordRecallQueryResult(query, annotatedResults, strategy, normalizedScope, queryText,
                executionResult.usedFallback(), executionResult.executionPath());
        return annotatedResults;
    }

    private RecallExecutionResult recallByStrategy(MemoryRecallQueryStrategy strategy,
                                                   Long userId,
                                                   Long conversationId,
                                                   String normalizedScope,
                                                   String normalizedMemoryType,
                                                   String normalizedSubType,
                                                   String queryText,
                                                   double minScore,
                                                   MemoryRecallMode recallMode) {
        return switch (strategy) {
            case BROWSE -> browseRecall(userId, conversationId, normalizedScope, normalizedMemoryType, normalizedSubType);
            case SEMANTIC -> semanticRecall(userId, conversationId, normalizedScope, normalizedMemoryType, normalizedSubType, queryText, minScore, recallMode);
            case HYBRID -> hybridRecall(userId, conversationId, normalizedScope, normalizedMemoryType, normalizedSubType, queryText, minScore, recallMode);
        };
    }

    private RecallExecutionResult browseRecall(Long userId,
                                               Long conversationId,
                                               String normalizedScope,
                                               String normalizedMemoryType,
                                               String normalizedSubType) {
        return new RecallExecutionResult(
                listVisibleEnabledMemories(userId, conversationId, normalizedScope, normalizedMemoryType, normalizedSubType),
                MemoryRecallLogConstant.EXECUTION_PATH_BROWSE,
                false);
    }

    private RecallExecutionResult hybridRecall(Long userId,
                                               Long conversationId,
                                               String normalizedScope,
                                               String normalizedMemoryType,
                                               String normalizedSubType,
                                               String queryText,
                                               double minScore,
                                               MemoryRecallMode recallMode) {
        if (StringUtils.isBlank(queryText)) {
            if (recallMode == MemoryRecallMode.PROMPT) {
                return new RecallExecutionResult(
                        List.of(),
                        MemoryRecallLogConstant.EXECUTION_PATH_HYBRID_SEMANTIC,
                        false);
            }
            return new RecallExecutionResult(
                    browseRecall(userId, conversationId, normalizedScope, normalizedMemoryType, normalizedSubType).results(),
                    MemoryRecallLogConstant.EXECUTION_PATH_HYBRID_BROWSE_FALLBACK,
                    true);
        }
        RecallExecutionResult semanticResult = semanticRecall(
                userId, conversationId, normalizedScope, normalizedMemoryType, normalizedSubType, queryText, minScore, recallMode);
        if (!semanticResult.results().isEmpty()) {
            return new RecallExecutionResult(
                    semanticResult.results(),
                    MemoryRecallLogConstant.EXECUTION_PATH_HYBRID_SEMANTIC,
                    false);
        }
        if (recallMode == MemoryRecallMode.PROMPT && shouldUseConversationPromptFallback(normalizedScope, conversationId)) {
            List<MemorySearchResult> fallbackResults = browseConversationFallbackMemories(
                    userId, conversationId, normalizedMemoryType, normalizedSubType);
            if (!fallbackResults.isEmpty()) {
                return new RecallExecutionResult(
                        fallbackResults,
                        MemoryRecallLogConstant.EXECUTION_PATH_HYBRID_CONVERSATION_BROWSE_FALLBACK,
                        true);
            }
        }
        if (recallMode == MemoryRecallMode.PROMPT) {
            return new RecallExecutionResult(
                    List.of(),
                    MemoryRecallLogConstant.EXECUTION_PATH_HYBRID_SEMANTIC,
                    false);
        }
        return new RecallExecutionResult(
                browseRecall(userId, conversationId, normalizedScope, normalizedMemoryType, normalizedSubType).results(),
                MemoryRecallLogConstant.EXECUTION_PATH_HYBRID_BROWSE_FALLBACK,
                true);
    }

    private RecallExecutionResult semanticRecall(Long userId,
                                                 Long conversationId,
                                                 String normalizedScope,
                                                 String normalizedMemoryType,
                                                 String normalizedSubType,
                                                 String queryText,
                                                 double minScore,
                                                 MemoryRecallMode recallMode) {
        if (StringUtils.isBlank(queryText)) {
            return new RecallExecutionResult(
                    List.of(),
                    MemoryRecallLogConstant.EXECUTION_PATH_SEMANTIC,
                    false);
        }
        try {
            Embedding queryEmbedding = embeddingModel.embed(queryText).content();
            var baseFilter = MetadataFilterBuilder.metadataKey(MemoryMetadataConstant.USER_ID).isEqualTo(userId)
                    .and(MetadataFilterBuilder.metadataKey(MemoryMetadataConstant.ENABLE).isEqualTo(ENABLED_MEMORY_VALUE));
            var filter = baseFilter;
            if (StringUtils.isNotBlank(normalizedScope)) {
                filter = filter.and(MetadataFilterBuilder.metadataKey(MemoryMetadataConstant.SCOPE).isEqualTo(normalizedScope));
            }
            if (StringUtils.isNotBlank(normalizedMemoryType)) {
                filter = filter.and(MetadataFilterBuilder.metadataKey(MemoryMetadataConstant.MEMORY_TYPE).isEqualTo(normalizedMemoryType));
            }
            if (StringUtils.isNotBlank(normalizedSubType)) {
                filter = filter.and(MetadataFilterBuilder.metadataKey(MemoryMetadataConstant.SUB_TYPE).isEqualTo(normalizedSubType));
            }

            EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                    .queryEmbedding(queryEmbedding)
                    .maxResults(resolveRecallCandidateLimit(userId))
                    .minScore(minScore)
                    .filter(filter)
                    .build();

            EmbeddingSearchResult<TextSegment> searchResult = memoryEmbeddingStore.search(request);
            List<MemorySearchResult> results = searchResult.matches().stream()
                    .map(this::toMemorySearchResult)
                    .filter(result -> matchesScope(result, normalizedScope))
                    .filter(result -> matchesMemoryType(result, normalizedMemoryType))
                    .filter(result -> matchesSubType(result, normalizedSubType))
                    .filter(result -> isVisibleToCurrentContext(result, conversationId))
                    .sorted(promptMemoryComparator())
                    .toList();
            return new RecallExecutionResult(
                    results,
                    MemoryRecallLogConstant.EXECUTION_PATH_SEMANTIC,
                    false);
        } catch (Exception e) {
            log.warn("Failed to recall memory by embedding, fallback to visible enabled memories", e);
            return new RecallExecutionResult(
                    List.of(),
                    MemoryRecallLogConstant.EXECUTION_PATH_SEMANTIC,
                    false);
        }
    }

    private List<MemorySearchResult> annotateRecallResults(List<MemorySearchResult> results,
                                                           MemoryRecallQueryStrategy strategy,
                                                           String executionPath,
                                                           boolean usedFallback) {
        if (results == null || results.isEmpty()) {
            return List.of();
        }
        String queryStrategy = strategy == null ? null : strategy.name();
        return results.stream()
                .map(result -> MemorySearchResult.builder()
                        .id(result.getId())
                        .scope(result.getScope())
                        .memoryType(result.getMemoryType())
                        .subType(result.getSubType())
                        .title(result.getTitle())
                        .content(result.getContent())
                        .reason(result.getReason())
                        .sourceType(result.getSourceType())
                        .score(result.getScore())
                        .queryStrategy(queryStrategy)
                        .executionPath(executionPath)
                        .usedFallback(usedFallback)
                        .accessCount(result.getAccessCount())
                        .conversationId(result.getConversationId())
                        .updatedAt(result.getUpdatedAt())
                        .build())
                .toList();
    }

    private void recordRecallQueryResult(MemoryRecallQuery query,
                                         List<MemorySearchResult> results,
                                         MemoryRecallQueryStrategy strategy,
                                         String normalizedScope,
                                         String queryText,
                                         boolean usedFallback,
                                         String executionPath) {
        Logger recallRuntimeLog = LoggerFactory.getLogger(MemoryRecallLogConstant.LOGGER_NAME);
        recallRuntimeLog.info("{} {}", MemoryRecallLogConstant.EVENT_RECALL_QUERY_RESULT,
                fieldsOf(
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

    private record MutationExecution(AiMemory memory, MemoryToolActionEnum action) {
    }

    @Override
    public PageResponse<AiMemory> pageCurrentUserMemories(PageRequest pageRequest,
                                                          String keyword,
                                                          String memoryType,
                                                          Integer enable,
                                                          String scope) {
        Long userId = requireUserId();
        int enableFilter = resolveMemoryEnable(enable);
        LambdaQueryWrapper<AiMemory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiMemory::getUserId, userId)
                .eq(AiMemory::getEnable, enableFilter)
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
        String scope = resolveScopeOrDefault(request.getScope(), DEFAULT_SCOPE);
        String memoryType = resolveMemoryType(request.getMemoryType());
        String subType = resolveMemorySubType(request.getMemoryType(), request.getSubType());
        validatePreferenceScope(scope, memoryType);
        MemoryMutationConverter.Mutation mutation = new MemoryMutationConverter.Mutation(
                request.getConversationId(),
                scope,
                memoryType,
                subType,
                resolveSourceTypeOrDefault(request.getSourceType(), DEFAULT_SOURCE_TYPE),
                resolveTitle(request.getTitle(), content),
                content,
                StringUtils.trimToNull(request.getReason()),
                ENABLED_MEMORY_VALUE,
                0,
                now
        );
        AiMemory existingPreference = findLatestEnabledPreferenceMemory(userId, memoryType, subType, null);
        if (existingPreference != null) {
            MemoryMutationConverter.apply(existingPreference, mutation);
            updateById(existingPreference);
            disableConflictingEnabledPreferenceMemories(userId, subType, existingPreference.getId(), now);
            rebuildEmbedding(existingPreference);
            recordMemoryMutation(MemoryLogConstant.EVENT_MEMORY_MANUAL_UPDATE, existingPreference, null);
            return existingPreference;
        }
        AiMemory memory = MemoryMutationConverter.create(userId, mutation);
        save(memory);
        rebuildEmbedding(memory);
        recordMemoryMutation(MemoryLogConstant.EVENT_MEMORY_MANUAL_CREATE, memory, null);
        return memory;
    }

    @Override
    public AiMemory updateMemory(Long memoryId, MemoryUpdateRequest request) {
        AiMemory memory = getByIdForCurrentUser(memoryId);
        String content = StringUtils.trimToEmpty(request.getContent());
        String targetScope = resolveScopeOrDefault(request.getScope(), StringUtils.defaultIfBlank(memory.getScope(), DEFAULT_SCOPE));
        String memoryType = resolveMemoryType(request.getMemoryType());
        String subType = resolveMemorySubType(request.getMemoryType(), request.getSubType());
        validatePreferenceScope(targetScope, memoryType);
        MemoryMutationConverter.Mutation mutation = new MemoryMutationConverter.Mutation(
                memory.getConversationId(),
                targetScope,
                memoryType,
                subType,
                resolveSourceTypeOrDefault(request.getSourceType(),
                        StringUtils.defaultIfBlank(memory.getSourceType(), DEFAULT_SOURCE_TYPE)),
                resolveTitle(request.getTitle(), content),
                content,
                StringUtils.trimToNull(request.getReason()),
                memory.getEnable(),
                memory.getAccessCount(),
                LocalDateTime.now()
        );
        MemoryMutationConverter.apply(memory, mutation);
        updateById(memory);
        disableConflictingEnabledPreferenceMemories(memory.getUserId(), subType, memory.getId(), memory.getUpdatedAt());
        rebuildEmbedding(memory);
        recordMemoryMutation(MemoryLogConstant.EVENT_MEMORY_MANUAL_UPDATE, memory, null);
        return memory;
    }

    @Override
    public MemoryWriteResult mutateAgentMemory(MemoryMutationRequest request) {
        Long userId = requireUserId();
        BusinessException.assertNotNull(request, "memory mutation request is required");
        MemoryOperationEnum operation = resolveOperation(request.getOperation());
        MutationExecution execution = switch (operation) {
            case CREATE -> createAgentMemory(request, userId);
            case UPDATE -> updateAgentMemory(request);
            case DELETE -> softDeleteAgentMemory(request);
        };
        recordMemoryMutation(MemoryLogConstant.EVENT_MEMORY_AGENT_WRITE, execution.memory(),
                fieldsOf(MemoryLogConstant.FIELD_ACTION, execution.action().getCode()));
        return MemoryWriteResult.builder()
                .memory(execution.memory())
                .action(execution.action())
                .build();
    }

    @Override
    public AiMemory disableMemory(Long memoryId) {
        AiMemory memory = getByIdForCurrentUser(memoryId);
        memory.setEnable(MemoryEnableEnum.DISABLE.getCode());
        memory.setUpdatedAt(LocalDateTime.now());
        updateById(memory);
        removeEmbeddingQuietly(memory.getId());
        recordMemoryMutation(MemoryLogConstant.EVENT_MEMORY_DISABLE, memory, null);
        return memory;
    }

    @Override
    public AiMemory enableMemory(Long memoryId) {
        AiMemory memory = getByIdForCurrentUser(memoryId);
        memory.setEnable(ENABLED_MEMORY_VALUE);
        memory.setUpdatedAt(LocalDateTime.now());
        updateById(memory);
        disableConflictingEnabledPreferenceMemories(memory.getUserId(), memory.getSubType(), memory.getId(), memory.getUpdatedAt());
        rebuildEmbedding(memory);
        recordMemoryMutation(MemoryLogConstant.EVENT_MEMORY_ENABLE, memory, null);
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
    public void recordMemoryAccess(List<Long> memoryIds) {
        int processedCount = touchMemories(memoryIds);
        recordTouchEvent(MemoryLogConstant.EVENT_MEMORY_ACCESS_RECORDED, memoryIds, processedCount);
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
                .memoryType(metadata.getString(MemoryMetadataConstant.MEMORY_TYPE))
                .subType(blankToNull(metadata.getString(MemoryMetadataConstant.SUB_TYPE)))
                .title(blankToNull(metadata.getString(MemoryMetadataConstant.TITLE)))
                .content(segment.text())
                .reason(blankToNull(metadata.getString(MemoryMetadataConstant.REASON)))
                .sourceType(blankToNull(metadata.getString(MemoryMetadataConstant.SOURCE_TYPE)))
                .score(match.score())
                .accessCount(longToInt(metadata.getLong(MemoryMetadataConstant.ACCESS_COUNT)))
                .conversationId(metadata.getLong(MemoryMetadataConstant.CONVERSATION_ID))
                .updatedAt(parseDateTime(metadata.getString(MemoryMetadataConstant.UPDATED_AT)))
                .build();
    }

    private MemorySearchResult toMemorySearchResult(AiMemory memory, double score) {
        return MemorySearchResult.builder()
                .id(memory.getId())
                .scope(memory.getScope())
                .memoryType(memory.getMemoryType())
                .subType(memory.getSubType())
                .title(memory.getTitle())
                .content(memory.getContent())
                .reason(memory.getReason())
                .sourceType(memory.getSourceType())
                .score(score)
                .accessCount(defaultInt(memory.getAccessCount(), 0))
                .conversationId(memory.getConversationId())
                .updatedAt(memory.getUpdatedAt())
                .build();
    }

    private void rebuildEmbedding(AiMemory memory) {
        removeEmbeddingQuietly(memory.getId());
        if (!Objects.equals(memory.getEnable(), ENABLED_MEMORY_VALUE) || StringUtils.isBlank(memory.getContent())) {
            return;
        }
        try {
            Embedding embedding = embeddingModel.embed(memory.getContent()).content();
            Metadata metadata = new Metadata()
                    .put(MemoryMetadataConstant.USER_ID, memory.getUserId())
                    .put(MemoryMetadataConstant.ENABLE, memory.getEnable())
                    .put(MemoryMetadataConstant.SCOPE, StringUtils.defaultString(memory.getScope()))
                    .put(MemoryMetadataConstant.MEMORY_TYPE, memory.getMemoryType())
                    .put(MemoryMetadataConstant.SUB_TYPE, StringUtils.defaultString(memory.getSubType()))
                    .put(MemoryMetadataConstant.TITLE, StringUtils.defaultString(memory.getTitle()))
                    .put(MemoryMetadataConstant.REASON, StringUtils.defaultString(memory.getReason()))
                    .put(MemoryMetadataConstant.SOURCE_TYPE, StringUtils.defaultString(memory.getSourceType()))
                    .put(MemoryMetadataConstant.ACCESS_COUNT, defaultInt(memory.getAccessCount(), 0))
                    .put(MemoryMetadataConstant.UPDATED_AT, memory.getUpdatedAt() == null ? "" : memory.getUpdatedAt().toString())
                    .put(MemoryMetadataConstant.CONVERSATION_ID, memory.getConversationId())
                    .put(MemoryMetadataConstant.MEMORY_ID, memory.getId());
            memoryEmbeddingStore.addAll(
                    List.of(embeddingStoreId(memory.getId())),
                    List.of(embedding),
                    List.of(TextSegment.from(memory.getContent(), metadata)));
        } catch (Exception e) {
            log.warn("Failed to rebuild memory embedding for memory {}", memory.getId(), e);
            logRuntimeError(MemoryLogConstant.EVENT_MEMORY_EMBEDDING_REBUILD_FAILED, e,
                    fieldsOf(
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
            memoryEmbeddingStore.remove(embeddingStoreId(memoryId));
        } catch (Exception e) {
            log.warn("Failed to remove memory embedding for memory {}", memoryId, e);
            logRuntimeError(MemoryLogConstant.EVENT_MEMORY_EMBEDDING_REMOVE_FAILED, e,
                    fieldsOf(MemoryLogConstant.FIELD_MEMORY_ID, memoryId));
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

    private String resolveScopeOrDefault(String value, String defaultValue) {
        String normalized = normalizeOrDefault(value, defaultValue);
        MemoryScopeEnum scope = MemoryScopeEnum.fromCode(normalized);
        if (scope == null) {
            throw BusinessException.badRequest("Unsupported scope '%s'. Valid values: %s",
                    value, MemoryScopeEnum.validCodes());
        }
        return scope.getCode();
    }

    private MemoryOperationEnum resolveOperation(String value) {
        MemoryOperationEnum operation = MemoryOperationEnum.fromCode(value);
        if (operation == null) {
            throw BusinessException.badRequest("Unsupported operation '%s'. Valid values: %s",
                    value, MemoryOperationEnum.validCodes());
        }
        return operation;
    }

    private MutationExecution createAgentMemory(MemoryMutationRequest request, Long userId) {
        String content = requireMutationContent(request.getContent());
        String scope = resolveScopeOrDefault(request.getScope(), DEFAULT_SCOPE);
        String memoryType = resolveMemoryType(request.getMemoryType());
        String subType = resolveMemorySubType(request.getMemoryType(), request.getSubType());
        validatePreferenceScope(scope, memoryType);
        LocalDateTime now = LocalDateTime.now();
        MemoryMutationConverter.Mutation mutation = new MemoryMutationConverter.Mutation(
                RequestContext.getConversationId(),
                scope,
                memoryType,
                subType,
                AGENT_SOURCE_TYPE,
                resolveTitle(request.getTitle(), content),
                content,
                StringUtils.trimToNull(request.getReason()),
                ENABLED_MEMORY_VALUE,
                0,
                now
        );
        AiMemory memory = MemoryMutationConverter.create(userId, mutation);
        save(memory);
        disableConflictingEnabledPreferenceMemories(userId, subType, memory.getId(), now);
        rebuildEmbedding(memory);
        return new MutationExecution(memory, MemoryToolActionEnum.CREATED);
    }

    private MutationExecution updateAgentMemory(MemoryMutationRequest request) {
        Long memoryId = requireMutationMemoryId(request.getMemoryId(), MemoryOperationEnum.UPDATE);
        AiMemory memory = getByIdForCurrentUser(memoryId);
        String content = requireMutationContent(request.getContent());
        String targetScope = resolveScopeOrDefault(request.getScope(),
                StringUtils.defaultIfBlank(memory.getScope(), DEFAULT_SCOPE));
        String targetMemoryTypeInput = StringUtils.defaultIfBlank(request.getMemoryType(), memory.getMemoryType());
        String targetSubTypeInput = StringUtils.defaultIfBlank(request.getSubType(), memory.getSubType());
        String memoryType = resolveMemoryType(targetMemoryTypeInput);
        String subType = resolveMemorySubType(targetMemoryTypeInput, targetSubTypeInput);
        validatePreferenceScope(targetScope, memoryType);
        String title = StringUtils.isBlank(request.getTitle())
                ? StringUtils.defaultIfBlank(memory.getTitle(), resolveTitle(null, content))
                : resolveTitle(request.getTitle(), content);
        String reason = StringUtils.isBlank(request.getReason())
                ? StringUtils.trimToNull(memory.getReason())
                : StringUtils.trimToNull(request.getReason());
        MemoryMutationConverter.Mutation mutation = new MemoryMutationConverter.Mutation(
                memory.getConversationId(),
                targetScope,
                memoryType,
                subType,
                AGENT_SOURCE_TYPE,
                title,
                content,
                reason,
                memory.getEnable(),
                memory.getAccessCount(),
                LocalDateTime.now()
        );
        MemoryMutationConverter.apply(memory, mutation);
        updateById(memory);
        disableConflictingEnabledPreferenceMemories(memory.getUserId(), subType, memory.getId(), memory.getUpdatedAt());
        rebuildEmbedding(memory);
        return new MutationExecution(memory, MemoryToolActionEnum.UPDATED);
    }

    private MutationExecution softDeleteAgentMemory(MemoryMutationRequest request) {
        Long memoryId = requireMutationMemoryId(request.getMemoryId(), MemoryOperationEnum.DELETE);
        AiMemory memory = getByIdForCurrentUser(memoryId);
        memory.setEnable(MemoryEnableEnum.DISABLE.getCode());
        memory.setUpdatedAt(LocalDateTime.now());
        updateById(memory);
        removeEmbeddingQuietly(memory.getId());
        return new MutationExecution(memory, MemoryToolActionEnum.DELETED);
    }

    private Long requireMutationMemoryId(Long memoryId, MemoryOperationEnum operation) {
        if (memoryId == null || memoryId <= 0L) {
            throw BusinessException.badRequest("memoryId is required for operation '%s'.", operation.getCode());
        }
        return memoryId;
    }

    private String requireMutationContent(String contentValue) {
        String content = StringUtils.trimToEmpty(contentValue);
        if (content.isBlank()) {
            throw BusinessException.badRequest("memory content is required");
        }
        return content;
    }

    private void validatePreferenceScope(String scope, String memoryType) {
        if (MemoryTypeEnum.PREFERENCE.matches(memoryType)) {
            BusinessException.assertTrue(MemoryScopeEnum.USER.matches(scope),
                    "PREFERENCE memories must use USER scope.");
        }
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

    private int resolveMemoryEnable(Integer value) {
        try {
            return MemoryEnableEnum.fromCode(value).getCode();
        } catch (IllegalArgumentException e) {
            throw BusinessException.badRequest("Unsupported enable value '%s'. Valid values: 0, 1", value);
        }
    }

    private void recordMemoryMutation(String eventName, AiMemory memory, Map<String, Object> extraFields) {
        if (memory == null) {
            return;
        }
        Map<String, Object> fields = fieldsOf(
                MemoryLogConstant.FIELD_MEMORY_ID, memory.getId(),
                MemoryLogConstant.FIELD_USER_ID, memory.getUserId(),
                MemoryLogConstant.FIELD_CONVERSATION_ID, memory.getConversationId(),
                MemoryLogConstant.FIELD_SCOPE, memory.getScope(),
                MemoryLogConstant.FIELD_MEMORY_TYPE, memory.getMemoryType(),
                MemoryLogConstant.FIELD_SUB_TYPE, memory.getSubType(),
                MemoryLogConstant.FIELD_SOURCE_TYPE, memory.getSourceType(),
                MemoryLogConstant.FIELD_ENABLE, memory.getEnable());
        if (extraFields != null && !extraFields.isEmpty()) {
            fields.putAll(extraFields);
        }
        logRuntimeInfo(eventName, fields);
    }

    private void recordTouchEvent(String eventName, List<Long> memoryIds, int processedCount) {
        List<Long> uniqueIds = memoryIds == null ? List.of() : memoryIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        logRuntimeInfo(eventName,
                fieldsOf(
                        MemoryLogConstant.FIELD_USER_ID, RequestContext.getUserId(),
                        MemoryLogConstant.FIELD_MEMORY_IDS, uniqueIds,
                        MemoryLogConstant.FIELD_PROCESSED_COUNT, processedCount));
    }

    private void logRuntimeInfo(String eventName, Map<String, Object> fields) {
        runtimeLog.info("{} {}", eventName, fields);
    }

    private void logRuntimeError(String eventName, Throwable throwable, Map<String, Object> fields) {
        runtimeLog.error("{} {}", eventName, fields, throwable);
    }

    private Map<String, Object> fieldsOf(Object... keyValues) {
        Map<String, Object> fields = new LinkedHashMap<>();
        if (keyValues == null) {
            return fields;
        }
        for (int i = 0; i + 1 < keyValues.length; i += 2) {
            fields.put(String.valueOf(keyValues[i]), keyValues[i + 1]);
        }
        return fields;
    }

    private String resolveTitle(String title, String content) {
        String trimmedTitle = StringUtils.trimToNull(title);
        if (trimmedTitle != null) {
            return trimmedTitle;
        }
        String normalized = PromptSafeText.normalizeTitle(content);
        return normalized.length() <= 120 ? normalized : normalized.substring(0, 120) + "...";
    }

    private boolean isVisibleToCurrentContext(MemorySearchResult result,
                                              Long conversationId) {
        String scope = normalizeOrDefault(result.getScope(), DEFAULT_SCOPE);
        if (MemoryScopeEnum.USER.matches(scope)) {
            return true;
        }
        if (MemoryScopeEnum.CONVERSATION.matches(scope)) {
            return conversationId != null && Objects.equals(conversationId, result.getConversationId());
        }
        return false;
    }

    private boolean shouldUseConversationPromptFallback(String normalizedScope, Long conversationId) {
        return conversationId != null && MemoryScopeEnum.CONVERSATION.matches(normalizedScope);
    }

    private Comparator<MemorySearchResult> promptMemoryComparator() {
        return Comparator
                .comparingInt(this::scopePriority)
                .thenComparing(Comparator.comparingDouble(MemorySearchResult::getScore).reversed())
                .thenComparing(Comparator.comparingInt(this::accessPriority).reversed())
                .thenComparing(MemorySearchResult::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(MemorySearchResult::getId, Comparator.nullsLast(Comparator.reverseOrder()));
    }

    private int scopePriority(MemorySearchResult result) {
        MemoryScopeEnum scope = MemoryScopeEnum.fromCode(normalizeOrDefault(result.getScope(), DEFAULT_SCOPE));
        return scope == null ? Integer.MAX_VALUE : scope.getPriority();
    }

    private int accessPriority(MemorySearchResult result) {
        return result.getAccessCount() == null ? 0 : result.getAccessCount();
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

    private String embeddingStoreId(Long memoryId) {
        if (memoryId == null) {
            throw new IllegalArgumentException("memoryId is required");
        }
        return UUID.nameUUIDFromBytes(("ai-memory:" + memoryId).getBytes(StandardCharsets.UTF_8)).toString();
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
                .eq(AiMemory::getEnable, ENABLED_MEMORY_VALUE);
        long candidateCount = count(wrapper);
        if (candidateCount <= 0) {
            return 1;
        }
        return candidateCount >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) candidateCount;
    }

    private List<MemorySearchResult> listVisibleEnabledMemories(Long userId,
                                                                Long conversationId,
                                                                String scope,
                                                                String memoryType,
                                                                String subType) {
        LambdaQueryWrapper<AiMemory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiMemory::getUserId, userId)
                .eq(AiMemory::getEnable, ENABLED_MEMORY_VALUE)
                .orderByDesc(AiMemory::getUpdatedAt)
                .orderByDesc(AiMemory::getId);
        if (StringUtils.isNotBlank(scope)) {
            wrapper.eq(AiMemory::getScope, scope);
        }
        if (StringUtils.isNotBlank(memoryType)) {
            wrapper.eq(AiMemory::getMemoryType, memoryType);
        }
        if (StringUtils.isNotBlank(subType)) {
            wrapper.eq(AiMemory::getSubType, subType);
        }
        return list(wrapper).stream()
                .map(memory -> toMemorySearchResult(memory, 0.0D))
                .filter(result -> matchesScope(result, scope))
                .filter(result -> matchesMemoryType(result, memoryType))
                .filter(result -> matchesSubType(result, subType))
                .filter(result -> isVisibleToCurrentContext(result, conversationId))
                .sorted(promptMemoryComparator())
                .toList();
    }

    private List<MemorySearchResult> browseConversationFallbackMemories(Long userId,
                                                                        Long conversationId,
                                                                        String memoryType,
                                                                        String subType) {
        if (userId == null || conversationId == null) {
            return List.of();
        }
        return queryConversationFallbackMemories(userId, conversationId, memoryType, subType, CONVERSATION_FALLBACK_LIMIT).stream()
                .limit(CONVERSATION_FALLBACK_LIMIT)
                .map(memory -> toMemorySearchResult(memory, 0.0D))
                .toList();
    }

    protected List<AiMemory> queryConversationFallbackMemories(Long userId,
                                                               Long conversationId,
                                                               String memoryType,
                                                               String subType,
                                                               int limit) {
        if (userId == null || conversationId == null || limit <= 0) {
            return List.of();
        }
        LambdaQueryWrapper<AiMemory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiMemory::getUserId, userId)
                .eq(AiMemory::getConversationId, conversationId)
                .eq(AiMemory::getEnable, ENABLED_MEMORY_VALUE)
                .eq(AiMemory::getScope, MemoryScopeEnum.CONVERSATION.getCode())
                .orderByDesc(AiMemory::getUpdatedAt)
                .orderByDesc(AiMemory::getId);
        if (StringUtils.isNotBlank(memoryType)) {
            wrapper.eq(AiMemory::getMemoryType, memoryType);
        }
        if (StringUtils.isNotBlank(subType)) {
            wrapper.eq(AiMemory::getSubType, subType);
        }
        Page<AiMemory> page = new Page<>(1, limit, false);
        return page(page, wrapper).getRecords();
    }

    private boolean matchesScope(MemorySearchResult result, String scope) {
        return StringUtils.isBlank(scope) || StringUtils.equalsIgnoreCase(scope, result.getScope());
    }

    private boolean matchesMemoryType(MemorySearchResult result, String memoryType) {
        return StringUtils.isBlank(memoryType) || StringUtils.equalsIgnoreCase(memoryType, result.getMemoryType());
    }

    private boolean matchesSubType(MemorySearchResult result, String subType) {
        return StringUtils.isBlank(subType) || StringUtils.equalsIgnoreCase(subType, result.getSubType());
    }

    protected List<AiMemory> listUserMemoriesByUpdatedAt(Long userId) {
        LambdaQueryWrapper<AiMemory> wrapper = new LambdaQueryWrapper<>();
        BusinessException.assertNotNull(userId, "error.not.login");
        wrapper.eq(AiMemory::getUserId, userId);
        wrapper.orderByDesc(AiMemory::getUpdatedAt).orderByDesc(AiMemory::getId);
        return list(wrapper);
    }

    private AiMemory findLatestEnabledPreferenceMemory(Long userId,
                                                       String memoryType,
                                                       String subType,
                                                       Long excludeId) {
        if (!MemoryTypeEnum.PREFERENCE.matches(memoryType) || StringUtils.isBlank(subType) || userId == null) {
            return null;
        }
        return listUserMemoriesByUpdatedAt(userId).stream()
                .filter(this::isEnabledMemory)
                .filter(memory -> userId.equals(memory.getUserId()))
                .filter(memory -> MemoryTypeEnum.PREFERENCE.matches(memory.getMemoryType()))
                .filter(memory -> StringUtils.equals(subType, memory.getSubType()))
                .filter(memory -> excludeId == null || !excludeId.equals(memory.getId()))
                .sorted(preferenceMemoryComparator())
                .findFirst()
                .orElse(null);
    }

    private void disableConflictingEnabledPreferenceMemories(Long userId,
                                                             String subType,
                                                             Long keepId,
                                                             LocalDateTime now) {
        if (userId == null || StringUtils.isBlank(subType)) {
            return;
        }
        listUserMemoriesByUpdatedAt(userId).stream()
                .filter(this::isEnabledMemory)
                .filter(memory -> userId.equals(memory.getUserId()))
                .filter(memory -> MemoryTypeEnum.PREFERENCE.matches(memory.getMemoryType()))
                .filter(memory -> StringUtils.equals(subType, memory.getSubType()))
                .filter(memory -> keepId == null || !keepId.equals(memory.getId()))
                .forEach(memory -> {
                    memory.setEnable(MemoryEnableEnum.DISABLE.getCode());
                    memory.setUpdatedAt(now == null ? LocalDateTime.now() : now);
                    updateById(memory);
                    removeEmbeddingQuietly(memory.getId());
                });
    }

    private Comparator<AiMemory> preferenceMemoryComparator() {
        return Comparator.comparing(AiMemory::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(AiMemory::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(AiMemory::getId, Comparator.nullsLast(Comparator.reverseOrder()));
    }

    private boolean isEnabledMemory(AiMemory memory) {
        return Objects.equals(memory.getEnable(), MemoryEnableEnum.ENABLE.getCode());
    }

    private boolean isDisabledMemory(AiMemory memory) {
        return Objects.equals(memory.getEnable(), MemoryEnableEnum.DISABLE.getCode());
    }

    private int touchMemories(List<Long> memoryIds) {
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
            memory.setAccessCount(defaultInt(memory.getAccessCount(), 0) + 1);
            memory.setLastAccessedAt(now);
            updateById(memory);
            processedCount++;
        }
        return processedCount;
    }

    private int defaultInt(Integer value, int defaultValue) {
        return value == null ? defaultValue : value;
    }

    @Override
    public List<MemorySummary> getEnabledMemorySummaries(Long userId) {
        LambdaQueryWrapper<AiMemory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiMemory::getUserId, userId)
                .eq(AiMemory::getEnable, ENABLED_MEMORY_VALUE)
                .orderByDesc(AiMemory::getUpdatedAt);
        List<AiMemory> memories = list(wrapper);
        return memories.stream()
                .map(m -> new MemorySummary(
                        m.getId(),
                        m.getScope(),
                        m.getMemoryType(),
                        m.getSubType(),
                        m.getTitle(),
                        StringUtils.abbreviate(StringUtils.defaultString(m.getContent()), 200),
                        m.getUpdatedAt()))
                .toList();
    }

    @Override
    public boolean hasManualWritesSince(Long userId, Long conversationId, LocalDateTime since) {
        if (since == null) {
            return false;
        }
        LambdaQueryWrapper<AiMemory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiMemory::getUserId, userId)
                .eq(AiMemory::getConversationId, conversationId)
                .eq(AiMemory::getSourceType, DEFAULT_SOURCE_TYPE)
                .gt(AiMemory::getUpdatedAt, since);
        return count(wrapper) > 0;
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class)
    public void applyAutoWriteItems(Long conversationId, Long userId, List<MemoryWriteItem> items,
                                    AiConversationMemoryCursor cursor, Long lastMessageId) {
        log.info("[MemAutoWrite:Apply] Starting to apply {} items: conversationId={}, userId={}", items.size(), conversationId, userId);
        for (int i = 0; i < items.size(); i++) {
            MemoryWriteItem item = items.get(i);
            try {
                log.info("[MemAutoWrite:Apply] Applying item[{}]: op={}, memoryId={}, type={}/{}",
                        i, item.operation(), item.memoryId(), item.memoryType(), item.subType());
                switch (item.operation()) {
                    case "CREATE" -> createAutoMemory(conversationId, userId, item);
                    case "UPDATE" -> updateAutoMemory(conversationId, userId, item);
                    case "DELETE" -> deleteAutoMemory(userId, item);
                    default -> log.warn("[MemAutoWrite:Apply] Unknown operation: {}", item.operation());
                }
                log.info("[MemAutoWrite:Apply] item[{}] applied successfully", i);
            } catch (Exception e) {
                log.error("[MemAutoWrite:Apply] Failed on item[{}]: {}", i, item, e);
                throw e;
            }
        }
        cursor.setLastProcessedMessageId(lastMessageId);
        cursor.setLastProcessedAt(LocalDateTime.now());
        cursor.setUpdatedAt(LocalDateTime.now());
        cursorService.updateById(cursor);
        log.info("[MemAutoWrite:Apply] All {} items applied, cursor advanced to messageId={}", items.size(), lastMessageId);
    }

    private void createAutoMemory(Long conversationId, Long userId, MemoryWriteItem item) {
        String scope = resolveScopeOrDefault(item.scope(), DEFAULT_SCOPE);
        String memoryType = resolveMemoryType(item.memoryType());
        String subType = resolveMemorySubType(item.memoryType(), item.subType());
        String content = StringUtils.trimToEmpty(item.content());
        String title = resolveTitle(item.title(), content);
        LocalDateTime now = LocalDateTime.now();

        if (MemoryTypeEnum.PREFERENCE.matches(memoryType)) {
            AiMemory existing = findLatestEnabledPreferenceMemory(userId, memoryType, subType, null);
            if (existing != null) {
                log.info("[MemAutoWrite:Apply] Converting PREFERENCE CREATE -> UPDATE: existingMemoryId={}, subType={}",
                        existing.getId(), subType);
                updateAutoMemory(conversationId, userId, new MemoryWriteItem(
                        "UPDATE", existing.getId(), item.scope(), item.memoryType(),
                        item.subType(), item.title(), item.content(), item.reason()));
                return;
            }
        }

        AiMemory memory = AiMemory.builder()
                .userId(userId)
                .conversationId(conversationId)
                .scope(scope)
                .memoryType(memoryType)
                .subType(subType)
                .sourceType(AGENT_SOURCE_TYPE)
                .title(title)
                .content(content)
                .reason(StringUtils.trimToNull(item.reason()))
                .enable(ENABLED_MEMORY_VALUE)
                .accessCount(0)
                .createdAt(now)
                .updatedAt(now)
                .build();
        save(memory);
        disableConflictingEnabledPreferenceMemories(userId, subType, memory.getId(), now);
        rebuildEmbedding(memory);
        log.info("[MemAutoWrite:Apply] Created memory: id={}, type={}/{}, title={}, content={}",
                memory.getId(), memoryType, subType, title,
                StringUtils.abbreviate(content, 100));
    }

    private void updateAutoMemory(Long conversationId, Long userId, MemoryWriteItem item) {
        AiMemory memory = getById(item.memoryId());
        if (memory == null || !Objects.equals(memory.getUserId(), userId)) {
            log.warn("[MemAutoWrite:Apply] UPDATE skipped: memoryId={} not found or not owned by userId={}", item.memoryId(), userId);
            return;
        }
        String oldTitle = memory.getTitle();
        String oldContent = memory.getContent();
        if (StringUtils.isNotBlank(item.title())) {
            memory.setTitle(resolveTitle(item.title(), memory.getContent()));
        }
        if (StringUtils.isNotBlank(item.content())) {
            memory.setContent(StringUtils.trimToEmpty(item.content()));
        }
        if (StringUtils.isNotBlank(item.reason())) {
            memory.setReason(StringUtils.trimToNull(item.reason()));
        }
        memory.setConversationId(conversationId);
        memory.setSourceType(AGENT_SOURCE_TYPE);
        memory.setUpdatedAt(LocalDateTime.now());
        updateById(memory);
        disableConflictingEnabledPreferenceMemories(userId, memory.getSubType(), memory.getId(), memory.getUpdatedAt());
        rebuildEmbedding(memory);
        log.info("[MemAutoWrite:Apply] Updated memory: id={}, title: [{}]->[{}], content: [{}]->[{}]",
                memory.getId(),
                StringUtils.abbreviate(StringUtils.defaultString(oldTitle), 50),
                StringUtils.abbreviate(StringUtils.defaultString(memory.getTitle()), 50),
                StringUtils.abbreviate(StringUtils.defaultString(oldContent), 60),
                StringUtils.abbreviate(StringUtils.defaultString(memory.getContent()), 60));
    }

    private void deleteAutoMemory(Long userId, MemoryWriteItem item) {
        AiMemory memory = getById(item.memoryId());
        if (memory == null || !Objects.equals(memory.getUserId(), userId)) {
            log.warn("[MemAutoWrite:Apply] DELETE skipped: memoryId={} not found or not owned by userId={}", item.memoryId(), userId);
            return;
        }
        log.info("[MemAutoWrite:Apply] Deleting memory: id={}, type={}/{}, title={}",
                memory.getId(), memory.getMemoryType(), memory.getSubType(), memory.getTitle());
        removeById(memory.getId());
        removeEmbeddingQuietly(memory.getId());
    }

    private Integer longToInt(Long value) {
        return value == null ? null : Math.toIntExact(value);
    }

    private static final class PromptSafeText {

        private PromptSafeText() {
        }

        private static String normalizeTitle(String value) {
            String normalized = StringUtils.normalizeSpace(StringUtils.defaultString(value));
            return normalized.isBlank() ? MemoryConstant.UNTITLED_MEMORY : normalized;
        }
    }

}
