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
import edu.zsc.ai.common.constant.MemoryLogConstant;
import edu.zsc.ai.common.constant.MemoryRecallLogConstant;
import edu.zsc.ai.common.enums.ai.MemoryEnableEnum;
import edu.zsc.ai.common.enums.ai.MemoryToolActionEnum;
import edu.zsc.ai.config.ai.MemoryProperties;
import edu.zsc.ai.context.RequestContext;
import edu.zsc.ai.context.RequestContextInfo;
import edu.zsc.ai.domain.exception.BusinessException;
import edu.zsc.ai.domain.model.dto.request.ai.MemoryCreateRequest;
import edu.zsc.ai.domain.model.dto.request.ai.MemoryUpdateRequest;
import edu.zsc.ai.domain.model.dto.request.ai.MemoryWriteRequest;
import edu.zsc.ai.domain.model.entity.ai.AiMemory;
import edu.zsc.ai.domain.service.ai.model.MemoryMaintenanceReport;
import edu.zsc.ai.domain.service.ai.model.MemorySearchResult;
import edu.zsc.ai.domain.service.ai.recall.MemoryRecallMode;
import edu.zsc.ai.domain.service.ai.recall.MemoryRecallQuery;
import edu.zsc.ai.domain.service.ai.recall.MemoryRecallQueryStrategy;
import edu.zsc.ai.domain.service.ai.model.MemoryWriteResult;
import edu.zsc.ai.observability.AgentLogEvent;
import edu.zsc.ai.observability.AgentLogService;

class MemoryServiceImplTest {

    private EmbeddingStore<TextSegment> embeddingStore;
    private InMemoryMemoryService service;
    private CaptureAgentLogService agentLogService;

    @BeforeEach
    void setUp() {
        @SuppressWarnings("unchecked")
        EmbeddingStore<TextSegment> store = mock(EmbeddingStore.class);
        embeddingStore = store;
        EmbeddingModel embeddingModel = mock(EmbeddingModel.class);
        when(embeddingModel.embed(any(String.class))).thenReturn(Response.from(Embedding.from(new float[]{1.0f, 2.0f})));

        MemoryProperties memoryProperties = new MemoryProperties();
        memoryProperties.getMaintenance().setDisableDuplicateEnabled(true);

        agentLogService = new CaptureAgentLogService();
        service = new InMemoryMemoryService(store, embeddingModel, memoryProperties, agentLogService);
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
        assertEquals(List.of(
                        MemoryLogConstant.EVENT_MEMORY_MANUAL_CREATE,
                        MemoryLogConstant.EVENT_MEMORY_MANUAL_UPDATE,
                        MemoryLogConstant.EVENT_MEMORY_DISABLE,
                        MemoryLogConstant.EVENT_MEMORY_ENABLE,
                        MemoryLogConstant.EVENT_MEMORY_DELETE),
                agentLogService.eventNames());
    }

    @Test
    void writeAgentMemory_alwaysCreatesNewMemory() {
        RequestContext.set(RequestContextInfo.builder()
                .userId(42L)
                .conversationId(7L)
                .build());

        MemoryWriteResult created = service.writeAgentMemory(MemoryWriteRequest.builder()
                .scope("user")
                .memoryType("workflow_constraint")
                .subType("implementation_constraint")
                .title("Memory V1")
                .content("Always use catalogName instead of databaseName.")
                .reason("User explicitly corrected the naming.")
                .build());

        MemoryWriteResult updated = service.writeAgentMemory(MemoryWriteRequest.builder()
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
        assertEquals(List.of(MemoryLogConstant.EVENT_MEMORY_AGENT_WRITE, MemoryLogConstant.EVENT_MEMORY_AGENT_WRITE),
                agentLogService.eventNames());
        assertEquals(MemoryToolActionEnum.CREATED.getCode(),
                agentLogService.event(0).getPayload().get(MemoryLogConstant.FIELD_ACTION));
        assertEquals(MemoryToolActionEnum.CREATED.getCode(),
                agentLogService.event(1).getPayload().get(MemoryLogConstant.FIELD_ACTION));
    }

    @Test
    void writeAgentMemory_overwritesExistingPreferenceSubtype() {
        RequestContext.set(RequestContextInfo.builder()
                .userId(42L)
                .conversationId(7L)
                .build());

        MemoryWriteResult created = service.writeAgentMemory(MemoryWriteRequest.builder()
                .scope("user")
                .memoryType("preference")
                .subType("language_preference")
                .title("语言偏好")
                .content("用户偏好使用中文回答。")
                .reason("用户明确要求后续用中文回答。")
                .build());

        MemoryWriteResult updated = service.writeAgentMemory(MemoryWriteRequest.builder()
                .scope("user")
                .memoryType("preference")
                .subType("language_preference")
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
    void writeAgentMemory_rejectsPreferenceConversationScope() {
        BusinessException exception = assertThrows(BusinessException.class, () -> service.writeAgentMemory(
                MemoryWriteRequest.builder()
                        .scope("conversation")
                        .memoryType("preference")
                        .subType("language_preference")
                        .content("用户偏好使用中文回答。")
                        .build()));

        assertEquals("PREFERENCE memories must use USER scope.", exception.getMessage());
    }

    @Test
    void writeAgentMemory_rejectsUnsupportedWorkspaceScope() {
        BusinessException exception = assertThrows(BusinessException.class, () -> service.writeAgentMemory(
                MemoryWriteRequest.builder()
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

    @Test
    void runCurrentUserMaintenance_keepsDuplicatesUntouched() {
        LocalDateTime now = LocalDateTime.now();

        service.seedMemory(AiMemory.builder()
                .userId(42L)
                .conversationId(7L)
                .scope("USER")
                .memoryType("BUSINESS_RULE")
                .subType("PROCESS_RULE")
                .sourceType("MANUAL")
                .title("Duplicate older")
                .content("Always verify write SQL on staging first")
                .enable(MemoryEnableEnum.ENABLE.getCode())
                .createdAt(now.minusDays(8))
                .updatedAt(now.minusDays(8))
                .build());

        service.seedMemory(AiMemory.builder()
                .userId(42L)
                .conversationId(7L)
                .scope("USER")
                .memoryType("BUSINESS_RULE")
                .subType("PROCESS_RULE")
                .sourceType("MANUAL")
                .title("Duplicate newer")
                .content("Always verify write SQL on staging first")
                .enable(MemoryEnableEnum.ENABLE.getCode())
                .createdAt(now.minusDays(4))
                .updatedAt(now.minusDays(1))
                .build());

        service.seedMemory(AiMemory.builder()
                .userId(42L)
                .conversationId(7L)
                .scope("USER")
                .memoryType("KNOWLEDGE_POINT")
                .subType("DOMAIN_KNOWLEDGE")
                .sourceType("MANUAL")
                .title("Healthy")
                .content("The revenue table is partitioned by day")
                .enable(MemoryEnableEnum.ENABLE.getCode())
                .createdAt(now.minusDays(3))
                .updatedAt(now.minusDays(1))
                .build());

        MemoryMaintenanceReport preview = service.inspectCurrentUserMaintenance();
        assertEquals(3, preview.getEnabledMemoryCount());
        assertEquals(0, preview.getDuplicateEnabledMemoryCount());

        MemoryMaintenanceReport report = service.runCurrentUserMaintenance();

        assertEquals(0, report.getProcessedDisabledCount());
        assertEquals(3, report.getEnabledMemoryCount());
        assertEquals(0, report.getDisabledMemoryCount());
        assertEquals(0, report.getDuplicateEnabledMemoryCount());
        AgentLogEvent event = agentLogService.lastEvent();
        assertEquals(MemoryLogConstant.EVENT_MEMORY_MAINTENANCE_RUN, event.getPayload().get("eventName"));
        assertEquals(0, event.getPayload().get(MemoryLogConstant.FIELD_PROCESSED_DISABLED_COUNT));
    }

    @Test
    void recordMemoryAccess_updatesCounters() {
        AiMemory memory = service.createManualMemory(buildCreateRequest());

        service.recordMemoryAccess(List.of(memory.getId(), memory.getId()));

        AiMemory updated = service.getById(memory.getId());
        assertEquals(1, updated.getAccessCount());
        assertNotNull(updated.getLastAccessedAt());
        assertEquals(List.of(
                        MemoryLogConstant.EVENT_MEMORY_MANUAL_CREATE,
                        MemoryLogConstant.EVENT_MEMORY_ACCESS_RECORDED),
                agentLogService.eventNames());
        assertEquals(List.of(memory.getId()),
                agentLogService.event(1).getPayload().get(MemoryLogConstant.FIELD_MEMORY_IDS));
        assertEquals(1, agentLogService.event(1).getPayload().get(MemoryLogConstant.FIELD_PROCESSED_COUNT));
    }

    @Test
    void searchEnabledMemories_recordsSearchEvent() {
        @SuppressWarnings("unchecked")
        EmbeddingSearchResult<TextSegment> searchResult = mock(EmbeddingSearchResult.class);
        when(searchResult.matches()).thenReturn(List.of());
        when(embeddingStore.search(any())).thenReturn(searchResult);

        AiMemory memory = service.createManualMemory(buildCreateRequest());

        List<?> results = service.searchEnabledMemories("concise output", 5, 0.2D, null, null);

        assertEquals(0, results.size());
        AgentLogEvent event = agentLogService.lastEvent();
        assertEquals(MemoryLogConstant.EVENT_MEMORY_SEARCH, event.getPayload().get("eventName"));
        assertEquals(true, event.getPayload().get(MemoryLogConstant.FIELD_QUERY_TEXT_PRESENT));
        assertEquals(5, event.getPayload().get(MemoryLogConstant.FIELD_LIMIT));
        assertEquals(List.of(), event.getPayload().get(MemoryLogConstant.FIELD_MEMORY_IDS));
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
        AgentLogEvent event = agentLogService.lastEvent();
        assertEquals(MemoryRecallLogConstant.EVENT_RECALL_QUERY_RESULT, event.getPayload().get("eventName"));
        assertEquals(MemoryRecallLogConstant.EXECUTION_PATH_BROWSE,
                event.getPayload().get(MemoryRecallLogConstant.FIELD_EXECUTION_PATH));
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
        AgentLogEvent event = agentLogService.lastEvent();
        assertEquals(MemoryRecallLogConstant.EXECUTION_PATH_SEMANTIC,
                event.getPayload().get(MemoryRecallLogConstant.FIELD_EXECUTION_PATH));
        assertEquals(false, event.getPayload().get(MemoryRecallLogConstant.FIELD_USED_FALLBACK));
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
        AgentLogEvent event = agentLogService.lastEvent();
        assertEquals(MemoryRecallLogConstant.EXECUTION_PATH_SEMANTIC,
                event.getPayload().get(MemoryRecallLogConstant.FIELD_EXECUTION_PATH));
        assertEquals(false, event.getPayload().get(MemoryRecallLogConstant.FIELD_USED_FALLBACK));
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
        AgentLogEvent event = agentLogService.lastEvent();
        assertEquals(MemoryRecallLogConstant.EXECUTION_PATH_HYBRID_SEMANTIC,
                event.getPayload().get(MemoryRecallLogConstant.FIELD_EXECUTION_PATH));
        assertEquals(false, event.getPayload().get(MemoryRecallLogConstant.FIELD_USED_FALLBACK));
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
        private long nextId = 1L;

        private InMemoryMemoryService(EmbeddingStore<TextSegment> embeddingStore,
                                      EmbeddingModel embeddingModel,
                                      MemoryProperties memoryProperties,
                                      AgentLogService agentLogService) {
            super(embeddingStore, embeddingModel, memoryProperties, agentLogService);
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
        protected List<AiMemory> listMemoriesForMaintenance(Long userId) {
            return store.stream()
                    .filter(memory -> userId == null || userId.equals(memory.getUserId()))
                    .toList();
        }

        private void seedMemory(AiMemory memory) {
            save(memory);
        }

        private int memoryStoreSize() {
            return store.size();
        }
    }

    private static final class CaptureAgentLogService implements AgentLogService {

        private final List<AgentLogEvent> events = new ArrayList<>();

        @Override
        public void record(AgentLogEvent event) {
            events.add(event);
        }

        private AgentLogEvent lastEvent() {
            return events.get(events.size() - 1);
        }

        private AgentLogEvent event(int index) {
            return events.get(index);
        }

        private List<String> eventNames() {
            return events.stream()
                    .map(event -> event.getPayload().get("eventName"))
                    .map(String::valueOf)
                    .toList();
        }
    }
}
