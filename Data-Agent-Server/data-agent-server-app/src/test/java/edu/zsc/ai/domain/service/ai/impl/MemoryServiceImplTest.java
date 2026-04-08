package edu.zsc.ai.domain.service.ai.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.baomidou.mybatisplus.core.conditions.Wrapper;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import edu.zsc.ai.common.enums.ai.MemoryEnableEnum;
import edu.zsc.ai.common.enums.ai.MemoryOperationEnum;
import edu.zsc.ai.common.enums.ai.MemoryToolActionEnum;
import edu.zsc.ai.config.ai.MemoryProperties;
import edu.zsc.ai.context.RequestContext;
import edu.zsc.ai.context.RequestContextInfo;
import edu.zsc.ai.domain.exception.BusinessException;
import edu.zsc.ai.domain.model.dto.request.ai.MemoryCreateRequest;
import edu.zsc.ai.domain.model.dto.request.ai.MemoryUpdateRequest;
import edu.zsc.ai.domain.model.dto.request.ai.MemoryMutationRequest;
import edu.zsc.ai.domain.model.entity.ai.AiMemory;
import edu.zsc.ai.domain.service.ai.model.MemorySearchResult;
import edu.zsc.ai.domain.service.ai.recall.MemoryRecallMode;
import edu.zsc.ai.domain.service.ai.recall.MemoryRecallQuery;
import edu.zsc.ai.domain.service.ai.recall.MemoryRecallQueryStrategy;
import edu.zsc.ai.domain.service.ai.model.MemoryWriteResult;

class MemoryServiceImplTest {

    private EmbeddingStore<TextSegment> embeddingStore;
    private InMemoryMemoryService service;

    @BeforeEach
    void setUp() {
        @SuppressWarnings("unchecked")
        EmbeddingStore<TextSegment> store = mock(EmbeddingStore.class);
        embeddingStore = store;
        EmbeddingModel embeddingModel = mock(EmbeddingModel.class);
        when(embeddingModel.embed(any(String.class))).thenReturn(Response.from(Embedding.from(new float[]{1.0f, 2.0f})));

        MemoryProperties memoryProperties = new MemoryProperties();

        service = new InMemoryMemoryService(store, embeddingModel, memoryProperties);
        RequestContext.set(RequestContextInfo.builder().userId(42L).conversationId(7L).build());
    }

    @AfterEach
    void tearDown() {
        RequestContext.clear();
    }

    @Test
    void createUpdateDisableEnableAndDeleteMemory() {
        MemoryCreateRequest createRequest = new MemoryCreateRequest();
        createRequest.setConversationId(7L);
        createRequest.setMemoryType("preference");
        createRequest.setSubType("response_format");
        createRequest.setContent("User prefers concise SQL explanations.");
        createRequest.setScope("user");

        AiMemory created = service.createManualMemory(createRequest);

        assertNotNull(created.getId());
        assertEquals("PREFERENCE", created.getMemoryType());
        assertEquals("USER", created.getScope());
        assertEquals(MemoryEnableEnum.ENABLE.getCode(), created.getEnable());
        verify(embeddingStore, atLeastOnce()).addAll(anyList(), anyList(), anyList());
        ArgumentCaptor<List<String>> addIdsCaptor = ArgumentCaptor.forClass(List.class);
        verify(embeddingStore, atLeastOnce()).addAll(addIdsCaptor.capture(), anyList(), anyList());
        UUID.fromString(addIdsCaptor.getValue().get(0));

        MemoryUpdateRequest updateRequest = new MemoryUpdateRequest();
        updateRequest.setMemoryType("business_rule");
        updateRequest.setSubType("domain_rule");
        updateRequest.setContent("Always confirm write SQL against production-like databases.");
        updateRequest.setScope("conversation");
        updateRequest.setSourceType("manual");

        AiMemory updated = service.updateMemory(created.getId(), updateRequest);

        assertEquals("BUSINESS_RULE", updated.getMemoryType());
        assertEquals("CONVERSATION", updated.getScope());
        ArgumentCaptor<String> removeIdCaptor = ArgumentCaptor.forClass(String.class);
        verify(embeddingStore, atLeastOnce()).remove(removeIdCaptor.capture());
        UUID.fromString(removeIdCaptor.getValue());

        AiMemory disabled = service.disableMemory(created.getId());
        assertEquals(MemoryEnableEnum.DISABLE.getCode(), disabled.getEnable());

        AiMemory enabled = service.enableMemory(created.getId());
        assertEquals(MemoryEnableEnum.ENABLE.getCode(), enabled.getEnable());

        service.deleteMemory(created.getId());
        assertThrows(BusinessException.class, () -> service.getByIdForCurrentUser(created.getId()));
    }

    @Test
    void mutateAgentMemory_createAlwaysCreatesNewMemory() {
        RequestContext.set(RequestContextInfo.builder()
                .userId(42L)
                .conversationId(7L)
                .build());

        MemoryWriteResult created = service.mutateAgentMemory(MemoryMutationRequest.builder()
                .operation(MemoryOperationEnum.CREATE.getCode())
                .scope("user")
                .memoryType("workflow_constraint")
                .subType("implementation_constraint")
                .title("Memory V1")
                .content("Always use catalogName instead of databaseName.")
                .reason("User explicitly corrected the naming.")
                .build());

        MemoryWriteResult updated = service.mutateAgentMemory(MemoryMutationRequest.builder()
                .operation(MemoryOperationEnum.CREATE.getCode())
                .scope("user")
                .memoryType("workflow_constraint")
                .subType("implementation_constraint")
                .title("Memory V2")
                .content("Always use catalogName instead of databaseName!")
                .reason("The latest correction should overwrite the earlier phrasing.")
                .build());

        assertEquals(2, service.memoryStoreSize());
        assertEquals("CREATED", created.getAction().getCode());
        assertEquals("CREATED", updated.getAction().getCode());
        assertEquals("USER", updated.getMemory().getScope());
        assertEquals("WORKFLOW_CONSTRAINT", updated.getMemory().getMemoryType());
        assertEquals("IMPLEMENTATION_CONSTRAINT", updated.getMemory().getSubType());
        assertEquals("AGENT", updated.getMemory().getSourceType());
        assertEquals("Memory V2", updated.getMemory().getTitle());
        assertEquals("Always use catalogName instead of databaseName!", updated.getMemory().getContent());
        assertEquals(MemoryToolActionEnum.CREATED.getCode(), created.getAction().getCode());
        assertEquals(MemoryToolActionEnum.CREATED.getCode(), updated.getAction().getCode());
    }

    @Test
    void mutateAgentMemory_updatesExistingMemoryById() {
        RequestContext.set(RequestContextInfo.builder()
                .userId(42L)
                .conversationId(7L)
                .build());

        MemoryWriteResult created = service.mutateAgentMemory(MemoryMutationRequest.builder()
                .operation(MemoryOperationEnum.CREATE.getCode())
                .scope("user")
                .memoryType("preference")
                .subType("language_preference")
                .title("语言偏好")
                .content("用户偏好使用中文回答。")
                .reason("用户明确要求后续用中文回答。")
                .build());

        MemoryWriteResult updated = service.mutateAgentMemory(MemoryMutationRequest.builder()
                .operation(MemoryOperationEnum.UPDATE.getCode())
                .memoryId(created.getMemory().getId())
                .title("语言偏好更新")
                .content("用户默认希望使用简体中文回答。")
                .reason("用户再次明确了中文偏好。")
                .build());

        assertEquals(1, service.memoryStoreSize());
        assertEquals(created.getMemory().getId(), updated.getMemory().getId());
        assertEquals(MemoryToolActionEnum.CREATED.getCode(), created.getAction().getCode());
        assertEquals(MemoryToolActionEnum.UPDATED.getCode(), updated.getAction().getCode());
        assertEquals("USER", updated.getMemory().getScope());
        assertEquals("PREFERENCE", updated.getMemory().getMemoryType());
        assertEquals("LANGUAGE_PREFERENCE", updated.getMemory().getSubType());
        assertEquals("用户默认希望使用简体中文回答。", updated.getMemory().getContent());
    }

    @Test
    void mutateAgentMemory_softDeletesMemoryById() {
        MemoryWriteResult created = service.mutateAgentMemory(MemoryMutationRequest.builder()
                .operation(MemoryOperationEnum.CREATE.getCode())
                .scope("conversation")
                .memoryType("workflow_constraint")
                .subType("implementation_constraint")
                .title("Scope guard")
                .content("Always use catalogName instead of databaseName.")
                .reason("User explicitly corrected the naming.")
                .build());

        MemoryWriteResult deleted = service.mutateAgentMemory(MemoryMutationRequest.builder()
                .operation(MemoryOperationEnum.DELETE.getCode())
                .memoryId(created.getMemory().getId())
                .build());

        assertEquals(MemoryToolActionEnum.DELETED.getCode(), deleted.getAction().getCode());
        assertEquals(MemoryEnableEnum.DISABLE.getCode(), deleted.getMemory().getEnable());
        assertEquals(MemoryEnableEnum.DISABLE.getCode(),
                service.getByIdForCurrentUser(created.getMemory().getId()).getEnable());
    }

    @Test
    void mutateAgentMemory_rejectsUpdateWithoutMemoryId() {
        BusinessException exception = assertThrows(BusinessException.class, () -> service.mutateAgentMemory(
                MemoryMutationRequest.builder()
                        .operation(MemoryOperationEnum.UPDATE.getCode())
                        .content("用户偏好使用中文回答。")
                        .build()));

        assertEquals("memoryId is required for operation 'UPDATE'.", exception.getMessage());
    }

    @Test
    void mutateAgentMemory_rejectsPreferenceConversationScope() {
        BusinessException exception = assertThrows(BusinessException.class, () -> service.mutateAgentMemory(
                MemoryMutationRequest.builder()
                        .operation(MemoryOperationEnum.CREATE.getCode())
                        .scope("conversation")
                        .memoryType("preference")
                        .subType("language_preference")
                        .content("用户偏好使用中文回答。")
                        .build()));

        assertEquals("PREFERENCE memories must use USER scope.", exception.getMessage());
    }

    @Test
    void mutateAgentMemory_rejectsUnsupportedWorkspaceScope() {
        BusinessException exception = assertThrows(BusinessException.class, () -> service.mutateAgentMemory(
                MemoryMutationRequest.builder()
                        .operation(MemoryOperationEnum.CREATE.getCode())
                        .scope("workspace")
                        .memoryType("workflow_constraint")
                        .subType("implementation_constraint")
                        .content("Always use catalogName instead of databaseName.")
                        .build()));

        assertEquals("Unsupported scope 'workspace'. Valid values: CONVERSATION, USER", exception.getMessage());
    }

    @Test
    void createManualMemory_overwritesExistingPreferenceSubtype() {
        MemoryCreateRequest first = new MemoryCreateRequest();
        first.setConversationId(7L);
        first.setMemoryType("preference");
        first.setSubType("response_format");
        first.setContent("用户偏好简洁回答。");
        first.setScope("user");

        AiMemory created = service.createManualMemory(first);

        MemoryCreateRequest second = new MemoryCreateRequest();
        second.setConversationId(7L);
        second.setMemoryType("preference");
        second.setSubType("response_format");
        second.setContent("用户偏好分点、简洁回答。");
        second.setScope("user");

        AiMemory overwritten = service.createManualMemory(second);

        assertEquals(1, service.memoryStoreSize());
        assertEquals(created.getId(), overwritten.getId());
        assertEquals("用户偏好分点、简洁回答。", overwritten.getContent());
    }

    @Test
    void createManualMemory_rejectsWorkspaceScope() {
        MemoryCreateRequest request = new MemoryCreateRequest();
        request.setMemoryType("preference");
        request.setScope("workspace");
        request.setContent("Workspace memories are AI-owned.");

        BusinessException exception = assertThrows(BusinessException.class, () -> service.createManualMemory(request));

        assertEquals("Unsupported scope 'workspace'. Valid values: CONVERSATION, USER", exception.getMessage());
    }

    @Test
    void createManualMemory_rejectsPreferenceConversationScope() {
        MemoryCreateRequest request = new MemoryCreateRequest();
        request.setMemoryType("preference");
        request.setSubType("language_preference");
        request.setScope("conversation");
        request.setContent("用户偏好使用中文回答。");

        BusinessException exception = assertThrows(BusinessException.class, () -> service.createManualMemory(request));

        assertEquals("PREFERENCE memories must use USER scope.", exception.getMessage());
    }

    @Test
    void createManualMemory_rejectsInvalidSubTypeForMemoryType() {
        MemoryCreateRequest request = new MemoryCreateRequest();
        request.setMemoryType("preference");
        request.setSubType("domain_rule");
        request.setScope("user");
        request.setContent("User likes concise output.");

        BusinessException exception = assertThrows(BusinessException.class, () -> service.createManualMemory(request));

        assertEquals("subType 'domain_rule' does not belong to memoryType 'PREFERENCE'. Valid subTypes: RESPONSE_FORMAT, LANGUAGE_PREFERENCE",
                exception.getMessage());
    }

    void recordMemoryAccess_updatesCounters() {
        AiMemory memory = service.createManualMemory(buildCreateRequest());

        service.recordMemoryAccess(List.of(memory.getId(), memory.getId()));

        AiMemory updated = service.getById(memory.getId());
        assertEquals(1, updated.getAccessCount());
        assertNotNull(updated.getLastAccessedAt());
    }

    @Test
    void searchEnabledMemories_recordsSearchEvent() {
        @SuppressWarnings("unchecked")
        EmbeddingSearchResult<TextSegment> searchResult = mock(EmbeddingSearchResult.class);
        when(searchResult.matches()).thenReturn(List.of());
        when(embeddingStore.search(any())).thenReturn(searchResult);

        service.createManualMemory(buildCreateRequest());

        List<?> results = service.searchEnabledMemories("concise output", 5, 0.2D, null, null);

        assertEquals(0, results.size());
    }

    @Test
    void searchEnabledMemories_filtersByMemoryType() {
        @SuppressWarnings("unchecked")
        EmbeddingSearchResult<TextSegment> searchResult = mock(EmbeddingSearchResult.class);
        when(searchResult.matches()).thenReturn(List.of());
        when(embeddingStore.search(any())).thenReturn(searchResult);

        service.seedMemory(AiMemory.builder()
                .userId(42L)
                .conversationId(7L)
                .scope("USER")
                .memoryType("PREFERENCE")
                .subType("LANGUAGE_PREFERENCE")
                .sourceType("AGENT")
                .title("中文偏好")
                .content("用户偏好使用中文进行交流")
                .enable(MemoryEnableEnum.ENABLE.getCode())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());

        List<MemorySearchResult> results = service.searchEnabledMemories("用户", 8, 0.6D, "KNOWLEDGE_POINT", null);

        assertEquals(0, results.size());
    }

    @Test
    void recallAccessibleMemories_browseStrategySkipsEmbeddingLookup() {
        service.seedMemory(AiMemory.builder()
                .userId(42L)
                .conversationId(7L)
                .scope("USER")
                .memoryType("PREFERENCE")
                .subType("RESPONSE_FORMAT")
                .sourceType("MANUAL")
                .content("User prefers concise output.")
                .enable(MemoryEnableEnum.ENABLE.getCode())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());

        List<?> results = service.recallAccessibleMemories(new MemoryRecallQuery(
                "browse",
                "browse_test",
                "USER",
                7L,
                "preference",
                "PREFERENCE",
                "RESPONSE_FORMAT",
                0.3D,
                MemoryRecallMode.PROMPT,
                MemoryRecallQueryStrategy.BROWSE,
                0));

        assertEquals(1, results.size());
        verify(service.embeddingModel, never()).embed(any(String.class));
    }

    @Test
    void recallAccessibleMemories_semanticStrategyReturnsSemanticResultsOnly() {
        @SuppressWarnings("unchecked")
        EmbeddingSearchResult<TextSegment> searchResult = mock(EmbeddingSearchResult.class);
        when(searchResult.matches()).thenReturn(List.of());
        when(embeddingStore.search(any())).thenReturn(searchResult);

        service.seedMemory(AiMemory.builder()
                .userId(42L)
                .conversationId(7L)
                .scope("USER")
                .memoryType("PREFERENCE")
                .subType("RESPONSE_FORMAT")
                .sourceType("MANUAL")
                .content("User prefers concise output.")
                .enable(MemoryEnableEnum.ENABLE.getCode())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());

        List<?> results = service.recallAccessibleMemories(new MemoryRecallQuery(
                "semantic",
                "semantic_test",
                "USER",
                7L,
                "preference",
                "PREFERENCE",
                "RESPONSE_FORMAT",
                0.3D,
                MemoryRecallMode.PROMPT,
                MemoryRecallQueryStrategy.SEMANTIC,
                0));

        assertEquals(0, results.size());
        verify(service.embeddingModel).embed("preference");
    }

    @Test
    void recallAccessibleMemories_toolSemanticDoesNotFallBackToBrowseWhenSemanticMisses() {
        @SuppressWarnings("unchecked")
        EmbeddingSearchResult<TextSegment> searchResult = mock(EmbeddingSearchResult.class);
        when(searchResult.matches()).thenReturn(List.of());
        when(embeddingStore.search(any())).thenReturn(searchResult);

        service.seedMemory(AiMemory.builder()
                .userId(42L)
                .conversationId(7L)
                .scope("USER")
                .memoryType("PREFERENCE")
                .subType("LANGUAGE_PREFERENCE")
                .sourceType("MANUAL")
                .title("语言偏好")
                .content("用户偏好使用中文回答。")
                .enable(MemoryEnableEnum.ENABLE.getCode())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());

        @SuppressWarnings("unchecked")
        List<MemorySearchResult> results = (List<MemorySearchResult>) (List<?>) service.recallAccessibleMemories(new MemoryRecallQuery(
                "semantic_tool",
                "semantic_tool_test",
                "USER",
                7L,
                "查询用户的偏好设置，包括回复格式、回复语言等个人偏好",
                "PREFERENCE",
                null,
                0.0D,
                MemoryRecallMode.TOOL,
                MemoryRecallQueryStrategy.SEMANTIC,
                0));

        assertEquals(0, results.size());
    }

    @Test
    void recallAccessibleMemories_promptHybridDoesNotFallBackToBrowseWhenSemanticMisses() {
        @SuppressWarnings("unchecked")
        EmbeddingSearchResult<TextSegment> searchResult = mock(EmbeddingSearchResult.class);
        when(searchResult.matches()).thenReturn(List.of());
        when(embeddingStore.search(any())).thenReturn(searchResult);

        service.seedMemory(AiMemory.builder()
                .userId(42L)
                .conversationId(7L)
                .scope("USER")
                .memoryType("KNOWLEDGE_POINT")
                .subType("OBJECT_KNOWLEDGE")
                .sourceType("MANUAL")
                .title("注册表")
                .content("Use enterprise_gateway_dev.chat2db_user for registration analysis.")
                .enable(MemoryEnableEnum.ENABLE.getCode())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());

        @SuppressWarnings("unchecked")
        List<MemorySearchResult> results = (List<MemorySearchResult>) (List<?>) service.recallAccessibleMemories(new MemoryRecallQuery(
                "prompt_hybrid",
                "prompt_hybrid_test",
                "USER",
                7L,
                "Analyze gas consumption by currency in 2012",
                null,
                null,
                0.0D,
                MemoryRecallMode.PROMPT,
                MemoryRecallQueryStrategy.HYBRID,
                0));

        assertEquals(0, results.size());
    }

    @Test
    void recallAccessibleMemories_promptHybridFallsBackToRecentConversationMemoriesWhenSemanticMisses() {
        @SuppressWarnings("unchecked")
        EmbeddingSearchResult<TextSegment> searchResult = mock(EmbeddingSearchResult.class);
        when(searchResult.matches()).thenReturn(List.of());
        when(embeddingStore.search(any())).thenReturn(searchResult);

        LocalDateTime now = LocalDateTime.now();
        service.seedMemory(AiMemory.builder()
                .userId(42L)
                .conversationId(7L)
                .scope("CONVERSATION")
                .memoryType("WORKFLOW_CONSTRAINT")
                .subType("PROCESS_RULE")
                .sourceType("MANUAL")
                .title("Oldest")
                .content("Oldest conversation memory.")
                .enable(MemoryEnableEnum.ENABLE.getCode())
                .accessCount(100)
                .createdAt(now.minusHours(6))
                .updatedAt(now.minusHours(6))
                .build());
        service.seedMemory(AiMemory.builder()
                .userId(42L)
                .conversationId(7L)
                .scope("CONVERSATION")
                .memoryType("WORKFLOW_CONSTRAINT")
                .subType("APPROVAL_RULE")
                .sourceType("MANUAL")
                .title("Fifth newest")
                .content("Fifth newest conversation memory.")
                .enable(MemoryEnableEnum.ENABLE.getCode())
                .createdAt(now.minusHours(5))
                .updatedAt(now.minusHours(5))
                .build());
        service.seedMemory(AiMemory.builder()
                .userId(42L)
                .conversationId(7L)
                .scope("CONVERSATION")
                .memoryType("WORKFLOW_CONSTRAINT")
                .subType("IMPLEMENTATION_CONSTRAINT")
                .sourceType("MANUAL")
                .title("Fourth newest")
                .content("Fourth newest conversation memory.")
                .enable(MemoryEnableEnum.ENABLE.getCode())
                .createdAt(now.minusHours(4))
                .updatedAt(now.minusHours(4))
                .build());
        service.seedMemory(AiMemory.builder()
                .userId(42L)
                .conversationId(7L)
                .scope("CONVERSATION")
                .memoryType("KNOWLEDGE_POINT")
                .subType("OBJECT_KNOWLEDGE")
                .sourceType("MANUAL")
                .title("Third newest")
                .content("Third newest conversation memory.")
                .enable(MemoryEnableEnum.ENABLE.getCode())
                .createdAt(now.minusHours(3))
                .updatedAt(now.minusHours(3))
                .build());
        service.seedMemory(AiMemory.builder()
                .userId(42L)
                .conversationId(7L)
                .scope("CONVERSATION")
                .memoryType("BUSINESS_RULE")
                .subType("DOMAIN_RULE")
                .sourceType("MANUAL")
                .title("Second newest")
                .content("Second newest conversation memory.")
                .enable(MemoryEnableEnum.ENABLE.getCode())
                .createdAt(now.minusHours(2))
                .updatedAt(now.minusHours(2))
                .build());
        service.seedMemory(AiMemory.builder()
                .userId(42L)
                .conversationId(7L)
                .scope("CONVERSATION")
                .memoryType("WORKFLOW_CONSTRAINT")
                .subType("REVIEW_CONSTRAINT")
                .sourceType("MANUAL")
                .title("Newest")
                .content("Newest conversation memory.")
                .enable(MemoryEnableEnum.ENABLE.getCode())
                .createdAt(now.minusHours(1))
                .updatedAt(now.minusHours(1))
                .build());
        service.seedMemory(AiMemory.builder()
                .userId(42L)
                .conversationId(7L)
                .scope("CONVERSATION")
                .memoryType("KNOWLEDGE_POINT")
                .subType("DOMAIN_KNOWLEDGE")
                .sourceType("MANUAL")
                .title("Disabled")
                .content("Disabled conversation memory.")
                .enable(MemoryEnableEnum.DISABLE.getCode())
                .createdAt(now.minusMinutes(30))
                .updatedAt(now.minusMinutes(30))
                .build());
        service.seedMemory(AiMemory.builder()
                .userId(42L)
                .conversationId(8L)
                .scope("CONVERSATION")
                .memoryType("WORKFLOW_CONSTRAINT")
                .subType("PROCESS_RULE")
                .sourceType("MANUAL")
                .title("Other conversation")
                .content("Other conversation memory.")
                .enable(MemoryEnableEnum.ENABLE.getCode())
                .createdAt(now.minusMinutes(20))
                .updatedAt(now.minusMinutes(20))
                .build());
        service.seedMemory(AiMemory.builder()
                .userId(42L)
                .conversationId(7L)
                .scope("USER")
                .memoryType("KNOWLEDGE_POINT")
                .subType("GLOSSARY")
                .sourceType("MANUAL")
                .title("User scope")
                .content("User scope memory.")
                .enable(MemoryEnableEnum.ENABLE.getCode())
                .createdAt(now.minusMinutes(10))
                .updatedAt(now.minusMinutes(10))
                .build());

        @SuppressWarnings("unchecked")
        List<MemorySearchResult> results = (List<MemorySearchResult>) (List<?>) service.recallAccessibleMemories(new MemoryRecallQuery(
                "prompt_hybrid_conversation",
                "prompt_hybrid_conversation_test",
                "CONVERSATION",
                7L,
                "Analyze gas consumption by currency in 2012",
                null,
                null,
                0.0D,
                MemoryRecallMode.PROMPT,
                MemoryRecallQueryStrategy.HYBRID,
                0));

        assertEquals(5, results.size());
        assertEquals(List.of("Newest", "Second newest", "Third newest", "Fourth newest", "Fifth newest"),
                results.stream().map(MemorySearchResult::getTitle).toList());
        assertEquals(List.of(7L, 7L, 7L, 7L, 7L),
                results.stream().map(MemorySearchResult::getConversationId).toList());
        assertEquals(List.of(true, true, true, true, true),
                results.stream().map(MemorySearchResult::isUsedFallback).toList());
        assertEquals(0, service.userMemoryListQueryCount());
        assertEquals(1, service.conversationFallbackQueryCount());
    }

    @Test
    void recallAccessibleMemories_promptHybridConversationFallbackPreservesExplicitTypeAndSubtypeFilters() {
        @SuppressWarnings("unchecked")
        EmbeddingSearchResult<TextSegment> searchResult = mock(EmbeddingSearchResult.class);
        when(searchResult.matches()).thenReturn(List.of());
        when(embeddingStore.search(any())).thenReturn(searchResult);

        LocalDateTime now = LocalDateTime.now();
        service.seedMemory(AiMemory.builder()
                .userId(42L)
                .conversationId(7L)
                .scope("CONVERSATION")
                .memoryType("WORKFLOW_CONSTRAINT")
                .subType("PROCESS_RULE")
                .sourceType("MANUAL")
                .title("Matched newest")
                .content("Matched newest conversation memory.")
                .enable(MemoryEnableEnum.ENABLE.getCode())
                .createdAt(now.minusMinutes(5))
                .updatedAt(now.minusMinutes(5))
                .build());
        service.seedMemory(AiMemory.builder()
                .userId(42L)
                .conversationId(7L)
                .scope("CONVERSATION")
                .memoryType("WORKFLOW_CONSTRAINT")
                .subType("PROCESS_RULE")
                .sourceType("MANUAL")
                .title("Matched older")
                .content("Matched older conversation memory.")
                .enable(MemoryEnableEnum.ENABLE.getCode())
                .createdAt(now.minusMinutes(10))
                .updatedAt(now.minusMinutes(10))
                .build());
        service.seedMemory(AiMemory.builder()
                .userId(42L)
                .conversationId(7L)
                .scope("CONVERSATION")
                .memoryType("WORKFLOW_CONSTRAINT")
                .subType("APPROVAL_RULE")
                .sourceType("MANUAL")
                .title("Wrong subtype")
                .content("Wrong subtype conversation memory.")
                .enable(MemoryEnableEnum.ENABLE.getCode())
                .createdAt(now.minusMinutes(3))
                .updatedAt(now.minusMinutes(3))
                .build());
        service.seedMemory(AiMemory.builder()
                .userId(42L)
                .conversationId(7L)
                .scope("CONVERSATION")
                .memoryType("BUSINESS_RULE")
                .subType("DOMAIN_RULE")
                .sourceType("MANUAL")
                .title("Wrong type")
                .content("Wrong type conversation memory.")
                .enable(MemoryEnableEnum.ENABLE.getCode())
                .createdAt(now.minusMinutes(1))
                .updatedAt(now.minusMinutes(1))
                .build());

        @SuppressWarnings("unchecked")
        List<MemorySearchResult> results = (List<MemorySearchResult>) (List<?>) service.recallAccessibleMemories(new MemoryRecallQuery(
                "prompt_hybrid_conversation_filtered",
                "prompt_hybrid_conversation_filtered_test",
                "CONVERSATION",
                7L,
                "Need current conversation process rules",
                "WORKFLOW_CONSTRAINT",
                "PROCESS_RULE",
                0.0D,
                MemoryRecallMode.PROMPT,
                MemoryRecallQueryStrategy.HYBRID,
                0));

        assertEquals(2, results.size());
        assertEquals(List.of("Matched newest", "Matched older"),
                results.stream().map(MemorySearchResult::getTitle).toList());
        assertEquals(List.of("WORKFLOW_CONSTRAINT", "WORKFLOW_CONSTRAINT"),
                results.stream().map(MemorySearchResult::getMemoryType).toList());
        assertEquals(List.of("PROCESS_RULE", "PROCESS_RULE"),
                results.stream().map(MemorySearchResult::getSubType).toList());
    }

    @Test
    void browseRecallPrefersMoreFrequentlyAccessedMemoryWhenOtherSignalsMatch() {
        LocalDateTime now = LocalDateTime.now();

        service.seedMemory(AiMemory.builder()
                .userId(42L)
                .conversationId(7L)
                .scope("USER")
                .memoryType("PREFERENCE")
                .subType("RESPONSE_FORMAT")
                .sourceType("MANUAL")
                .title("Less accessed")
                .content("User prefers bullet summaries.")
                .enable(MemoryEnableEnum.ENABLE.getCode())
                .accessCount(1)
                .createdAt(now.minusDays(1))
                .updatedAt(now)
                .build());

        service.seedMemory(AiMemory.builder()
                .userId(42L)
                .conversationId(7L)
                .scope("USER")
                .memoryType("PREFERENCE")
                .subType("RESPONSE_FORMAT")
                .sourceType("MANUAL")
                .title("More accessed")
                .content("User prefers concise output.")
                .enable(MemoryEnableEnum.ENABLE.getCode())
                .accessCount(5)
                .createdAt(now.minusDays(1))
                .updatedAt(now)
                .build());

        @SuppressWarnings("unchecked")
        List<MemorySearchResult> results = (List<MemorySearchResult>) (List<?>) service.recallAccessibleMemories(new MemoryRecallQuery(
                "browse",
                "browse_access_priority",
                "USER",
                7L,
                "preference",
                "PREFERENCE",
                "RESPONSE_FORMAT",
                0.0D,
                MemoryRecallMode.PROMPT,
                MemoryRecallQueryStrategy.BROWSE,
                0));

        assertEquals(2, results.size());
        assertEquals("More accessed", results.get(0).getTitle());
        assertEquals(5, results.get(0).getAccessCount());
    }

    private MemoryCreateRequest buildCreateRequest() {
        MemoryCreateRequest request = new MemoryCreateRequest();
        request.setConversationId(7L);
        request.setMemoryType("preference");
        request.setSubType("response_format");
        request.setContent("User prefers concise SQL explanations.");
        request.setScope("user");
        return request;
    }

    private static final class InMemoryMemoryService extends MemoryServiceImpl {

        private final List<AiMemory> store = new ArrayList<>();
        private final EmbeddingModel embeddingModel;
        private int userMemoryListQueryCount;
        private int conversationFallbackQueryCount;
        private long nextId = 1L;

        private InMemoryMemoryService(EmbeddingStore<TextSegment> embeddingStore,
                                      EmbeddingModel embeddingModel,
                                      MemoryProperties memoryProperties) {
            super(embeddingStore, embeddingModel, memoryProperties, null);
            this.embeddingModel = embeddingModel;
        }

        @Override
        public boolean save(AiMemory entity) {
            if (entity.getId() == null) {
                entity.setId(nextId++);
            }
            if (entity.getCreatedAt() == null) {
                entity.setCreatedAt(LocalDateTime.now());
            }
            if (entity.getUpdatedAt() == null) {
                entity.setUpdatedAt(entity.getCreatedAt());
            }
            store.add(entity);
            return true;
        }

        @Override
        public AiMemory getById(Serializable id) {
            return store.stream()
                    .filter(memory -> memory.getId().equals(id))
                    .findFirst()
                    .orElse(null);
        }

        @Override
        public boolean updateById(AiMemory entity) {
            for (int i = 0; i < store.size(); i++) {
                if (store.get(i).getId().equals(entity.getId())) {
                    store.set(i, entity);
                    return true;
                }
            }
            return false;
        }

        @Override
        public AiMemory getOne(Wrapper<AiMemory> queryWrapper, boolean throwEx) {
            return store.stream()
                    .filter(memory -> memory.getEnable() != null
                            && memory.getEnable() == MemoryEnableEnum.ENABLE.getCode())
                    .findFirst()
                    .orElse(null);
        }

        @Override
        public long count(Wrapper<AiMemory> queryWrapper) {
            return store.stream()
                    .filter(memory -> memory.getEnable() != null
                            && memory.getEnable() == MemoryEnableEnum.ENABLE.getCode())
                    .count();
        }

        @Override
        public List<AiMemory> list(Wrapper<AiMemory> queryWrapper) {
            return new ArrayList<>(store);
        }

        @Override
        public boolean removeById(Serializable id) {
            return store.removeIf(memory -> memory.getId().equals(id));
        }

        @Override
        protected List<AiMemory> listUserMemoriesByUpdatedAt(Long userId) {
            userMemoryListQueryCount++;
            return store.stream()
                    .filter(memory -> userId != null && userId.equals(memory.getUserId()))
                    .toList();
        }

        @Override
        protected List<AiMemory> queryConversationFallbackMemories(Long userId,
                                                                   Long conversationId,
                                                                   String memoryType,
                                                                   String subType,
                                                                   int limit) {
            conversationFallbackQueryCount++;
            return store.stream()
                    .filter(memory -> userId != null && userId.equals(memory.getUserId()))
                    .filter(memory -> conversationId != null && conversationId.equals(memory.getConversationId()))
                    .filter(memory -> memory.getEnable() != null && memory.getEnable() == MemoryEnableEnum.ENABLE.getCode())
                    .filter(memory -> "CONVERSATION".equalsIgnoreCase(memory.getScope()))
                    .filter(memory -> memoryType == null || memoryType.equalsIgnoreCase(memory.getMemoryType()))
                    .filter(memory -> subType == null || subType.equalsIgnoreCase(memory.getSubType()))
                    .sorted(Comparator.comparing(AiMemory::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                            .thenComparing(AiMemory::getId, Comparator.nullsLast(Comparator.reverseOrder())))
                    .limit(limit)
                    .toList();
        }

        private void seedMemory(AiMemory memory) {
            save(memory);
        }

        private int memoryStoreSize() {
            return store.size();
        }

        private int userMemoryListQueryCount() {
            return userMemoryListQueryCount;
        }

        private int conversationFallbackQueryCount() {
            return conversationFallbackQueryCount;
        }
    }
}
