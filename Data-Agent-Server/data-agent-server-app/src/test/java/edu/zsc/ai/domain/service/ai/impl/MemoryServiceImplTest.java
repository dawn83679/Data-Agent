package edu.zsc.ai.domain.service.ai.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.baomidou.mybatisplus.core.conditions.Wrapper;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import edu.zsc.ai.common.constant.MemoryLogConstant;
import edu.zsc.ai.common.constant.MemoryRecallLogConstant;
import edu.zsc.ai.common.enums.ai.MemoryStatusEnum;
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
        memoryProperties.getMaintenance().setArchiveExpiredEnabled(true);
        memoryProperties.getMaintenance().setHideDuplicateEnabled(true);

        agentLogService = new CaptureAgentLogService();
        service = new InMemoryMemoryService(store, embeddingModel, memoryProperties, agentLogService);
        RequestContext.set(RequestContextInfo.builder().userId(42L).conversationId(7L).build());
    }

    @AfterEach
    void tearDown() {
        RequestContext.clear();
    }

    @Test
    void createUpdateArchiveRestoreAndDeleteMemory() {
        MemoryCreateRequest createRequest = new MemoryCreateRequest();
        createRequest.setConversationId(7L);
        createRequest.setMemoryType("preference");
        createRequest.setSubType("output_format");
        createRequest.setContent("User prefers concise SQL explanations.");
        createRequest.setScope("user");

        AiMemory created = service.createManualMemory(createRequest);

        assertNotNull(created.getId());
        assertEquals("PREFERENCE", created.getMemoryType());
        assertEquals("USER", created.getScope());
        assertEquals(MemoryStatusEnum.ACTIVE.getCode(), created.getStatus());
        verify(embeddingStore, atLeastOnce()).addAll(anyList(), anyList(), anyList());

        MemoryUpdateRequest updateRequest = new MemoryUpdateRequest();
        updateRequest.setMemoryType("business_rule");
        updateRequest.setSubType("domain_rule");
        updateRequest.setContent("Always confirm write SQL against production-like databases.");
        updateRequest.setScope("conversation");
        updateRequest.setSourceType("manual");
        updateRequest.setConfidenceScore(0.88);
        updateRequest.setSalienceScore(0.77);
        updateRequest.setExpiresAt(LocalDateTime.of(2026, 4, 1, 0, 0));

        AiMemory updated = service.updateMemory(created.getId(), updateRequest);

        assertEquals("BUSINESS_RULE", updated.getMemoryType());
        assertEquals("CONVERSATION", updated.getScope());
        assertEquals(0.88, updated.getConfidenceScore());
        assertNotNull(updated.getExpiresAt());
        verify(embeddingStore, atLeastOnce()).remove(String.valueOf(created.getId()));

        AiMemory archived = service.archiveMemory(created.getId());
        assertEquals(MemoryStatusEnum.ARCHIVED.getCode(), archived.getStatus());
        assertNotNull(archived.getArchivedAt());

        AiMemory restored = service.restoreMemory(created.getId());
        assertEquals(MemoryStatusEnum.ACTIVE.getCode(), restored.getStatus());
        assertNull(restored.getArchivedAt());

        service.deleteMemory(created.getId());
        assertThrows(BusinessException.class, () -> service.getByIdForCurrentUser(created.getId()));
        assertEquals(List.of(
                        MemoryLogConstant.EVENT_MEMORY_MANUAL_CREATE,
                        MemoryLogConstant.EVENT_MEMORY_MANUAL_UPDATE,
                        MemoryLogConstant.EVENT_MEMORY_ARCHIVE,
                        MemoryLogConstant.EVENT_MEMORY_RESTORE,
                        MemoryLogConstant.EVENT_MEMORY_DELETE),
                agentLogService.eventNames());
    }

    @Test
    void writeAgentMemory_upsertsExistingActiveMemoryByNormalizedKey() {
        RequestContext.set(RequestContextInfo.builder()
                .userId(42L)
                .conversationId(7L)
                .connectionId(9L)
                .catalog("analytics")
                .schema("public")
                .build());

        MemoryWriteResult created = service.writeAgentMemory(MemoryWriteRequest.builder()
                .scope("workspace")
                .workspaceLevel("schema")
                .workspaceConnectionId(9L)
                .workspaceCatalogName("analytics")
                .workspaceSchemaName("public")
                .memoryType("workflow_constraint")
                .subType("implementation_constraint")
                .title("Memory V1")
                .content("Always use catalogName instead of databaseName.")
                .reason("User explicitly corrected the naming.")
                .confidence(0.93)
                .sourceMessageIds(List.of("msg-1"))
                .build());

        MemoryWriteResult updated = service.writeAgentMemory(MemoryWriteRequest.builder()
                .scope("workspace")
                .workspaceLevel("schema")
                .workspaceConnectionId(9L)
                .workspaceCatalogName("analytics")
                .workspaceSchemaName("public")
                .memoryType("workflow_constraint")
                .subType("implementation_constraint")
                .title("Memory V2")
                .content("Always use catalogName instead of databaseName!")
                .reason("The latest correction should overwrite the earlier phrasing.")
                .confidence(0.98)
                .sourceMessageIds(List.of("msg-1", "msg-2"))
                .build());

        assertEquals(created.getMemory().getId(), updated.getMemory().getId());
        assertEquals(1, service.memoryStoreSize());
        assertEquals("CREATED", created.getAction().getCode());
        assertEquals("UPDATED", updated.getAction().getCode());
        assertEquals("WORKSPACE", updated.getMemory().getScope());
        assertEquals("WORKFLOW_CONSTRAINT", updated.getMemory().getMemoryType());
        assertEquals("IMPLEMENTATION_CONSTRAINT", updated.getMemory().getSubType());
        assertEquals("AGENT", updated.getMemory().getSourceType());
        assertEquals("SCHEMA", updated.getMemory().getWorkspaceLevel());
        assertEquals("9:analytics:public", updated.getMemory().getWorkspaceContextKey());
        assertEquals("always use catalogname instead of databasename", updated.getMemory().getNormalizedContentKey());
        assertEquals("Memory V2", updated.getMemory().getTitle());
        assertEquals("Always use catalogName instead of databaseName!", updated.getMemory().getContent());
        assertEquals("[\"msg-1\",\"msg-2\"]", updated.getMemory().getSourceMessageIds());
        assertEquals(0.98, updated.getMemory().getConfidenceScore());
        assertEquals(List.of(MemoryLogConstant.EVENT_MEMORY_AGENT_WRITE, MemoryLogConstant.EVENT_MEMORY_AGENT_WRITE),
                agentLogService.eventNames());
        assertEquals(MemoryToolActionEnum.CREATED.getCode(),
                agentLogService.event(0).getPayload().get(MemoryLogConstant.FIELD_ACTION));
        assertEquals(MemoryToolActionEnum.UPDATED.getCode(),
                agentLogService.event(1).getPayload().get(MemoryLogConstant.FIELD_ACTION));
    }

    @Test
    void writeAgentMemory_requiresWorkspaceLevelForWorkspaceScope() {
        RequestContext.set(RequestContextInfo.builder()
                .userId(42L)
                .conversationId(7L)
                .connectionId(9L)
                .catalog("analytics")
                .schema("public")
                .build());

        BusinessException exception = assertThrows(BusinessException.class, () -> service.writeAgentMemory(
                MemoryWriteRequest.builder()
                        .scope("workspace")
                        .memoryType("workflow_constraint")
                        .subType("implementation_constraint")
                        .content("Always use catalogName instead of databaseName.")
                        .build()));

        assertEquals("workspaceLevel is required for WORKSPACE scope.", exception.getMessage());
    }

    @Test
    void writeAgentMemory_resolvesCatalogBindingFromExplicitWorkspaceFields() {
        MemoryWriteResult created = service.writeAgentMemory(MemoryWriteRequest.builder()
                .scope("workspace")
                .workspaceLevel("catalog")
                .workspaceConnectionId(9L)
                .workspaceCatalogName("analytics")
                .memoryType("business_rule")
                .subType("domain_rule")
                .title("Catalog rule")
                .content("Analytics uses cents for money.")
                .build());

        assertEquals("CATALOG", created.getMemory().getWorkspaceLevel());
        assertEquals("9:analytics", created.getMemory().getWorkspaceContextKey());
    }

    @Test
    void writeAgentMemory_requiresExplicitWorkspaceBindingForSchemaLevel() {
        BusinessException exception = assertThrows(BusinessException.class, () -> service.writeAgentMemory(
                MemoryWriteRequest.builder()
                        .scope("workspace")
                        .workspaceLevel("schema")
                        .workspaceConnectionId(9L)
                        .workspaceCatalogName("analytics")
                        .memoryType("knowledge_point")
                        .subType("object_knowledge")
                        .content("Orders live in public schema.")
                        .build()));

        assertEquals("workspaceSchemaName is required for WORKSPACE level SCHEMA.", exception.getMessage());
    }

    @Test
    void createManualMemory_rejectsWorkspaceScope() {
        MemoryCreateRequest request = new MemoryCreateRequest();
        request.setMemoryType("preference");
        request.setScope("workspace");
        request.setContent("Workspace memories are AI-owned.");

        BusinessException exception = assertThrows(BusinessException.class, () -> service.createManualMemory(request));

        assertEquals("Manual memory creation does not support WORKSPACE scope.", exception.getMessage());
    }

    @Test
    void createManualMemory_rejectsInvalidSubTypeForMemoryType() {
        MemoryCreateRequest request = new MemoryCreateRequest();
        request.setMemoryType("preference");
        request.setSubType("domain_rule");
        request.setScope("user");
        request.setContent("User likes concise output.");

        BusinessException exception = assertThrows(BusinessException.class, () -> service.createManualMemory(request));

        assertEquals("subType 'domain_rule' does not belong to memoryType 'PREFERENCE'. Valid subTypes: RESPONSE_STYLE, OUTPUT_FORMAT, LANGUAGE_PREFERENCE, INTERACTION_STYLE, DECISION_STYLE",
                exception.getMessage());
    }

    @Test
    void runCurrentUserMaintenanceArchivesExpiresAndHidesDuplicates() {
        LocalDateTime now = LocalDateTime.now();

        service.seedMemory(AiMemory.builder()
                .userId(42L)
                .conversationId(7L)
                .scope("USER")
                .memoryType("PREFERENCE")
                .subType("OUTPUT_FORMAT")
                .sourceType("MANUAL")
                .title("Expired")
                .content("Remember the legacy billing exception")
                .normalizedContentKey("remember the legacy billing exception")
                .detailJson("{}")
                .status(MemoryStatusEnum.ACTIVE.getCode())
                .confidenceScore(0.9)
                .salienceScore(0.6)
                .expiresAt(now.minusDays(1))
                .createdAt(now.minusDays(10))
                .updatedAt(now.minusDays(2))
                .build());

        service.seedMemory(AiMemory.builder()
                .userId(42L)
                .conversationId(7L)
                .scope("USER")
                .memoryType("BUSINESS_RULE")
                .subType("PROCESS_RULE")
                .sourceType("MANUAL")
                .title("Duplicate older")
                .content("Always verify write SQL on staging first")
                .normalizedContentKey("always verify write sql on staging first")
                .detailJson("{}")
                .status(MemoryStatusEnum.ACTIVE.getCode())
                .confidenceScore(0.9)
                .salienceScore(0.7)
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
                .normalizedContentKey("always verify write sql on staging first")
                .detailJson("{}")
                .status(MemoryStatusEnum.ACTIVE.getCode())
                .confidenceScore(0.95)
                .salienceScore(0.8)
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
                .normalizedContentKey("the revenue table is partitioned by day")
                .detailJson("{}")
                .status(MemoryStatusEnum.ACTIVE.getCode())
                .confidenceScore(0.8)
                .salienceScore(0.6)
                .createdAt(now.minusDays(3))
                .updatedAt(now.minusDays(1))
                .build());

        MemoryMaintenanceReport preview = service.inspectCurrentUserMaintenance();
        assertEquals(4, preview.getActiveMemoryCount());
        assertEquals(1, preview.getExpiredActiveMemoryCount());
        assertEquals(1, preview.getDuplicateActiveMemoryCount());

        MemoryMaintenanceReport report = service.runCurrentUserMaintenance();

        assertEquals(1, report.getProcessedArchivedCount());
        assertEquals(1, report.getProcessedHiddenCount());
        assertEquals(2, report.getActiveMemoryCount());
        assertEquals(1, report.getArchivedMemoryCount());
        assertEquals(1, report.getHiddenMemoryCount());
        assertEquals(0, report.getExpiredActiveMemoryCount());
        assertEquals(0, report.getDuplicateActiveMemoryCount());
        AgentLogEvent event = agentLogService.lastEvent();
        assertEquals(MemoryLogConstant.EVENT_MEMORY_MAINTENANCE_RUN, event.getPayload().get("eventName"));
        assertEquals(1, event.getPayload().get(MemoryLogConstant.FIELD_PROCESSED_ARCHIVED_COUNT));
        assertEquals(1, event.getPayload().get(MemoryLogConstant.FIELD_PROCESSED_HIDDEN_COUNT));
    }

    @Test
    void recordMemoryAccessAndUsage_updatesCounters() {
        AiMemory memory = service.createManualMemory(buildCreateRequest());

        service.recordMemoryAccess(List.of(memory.getId(), memory.getId()));
        service.recordMemoryUsage(List.of(memory.getId()));

        AiMemory updated = service.getById(memory.getId());
        assertEquals(1, updated.getAccessCount());
        assertEquals(1, updated.getUseCount());
        assertNotNull(updated.getLastAccessedAt());
        assertNotNull(updated.getLastUsedAt());
        assertEquals(List.of(
                        MemoryLogConstant.EVENT_MEMORY_MANUAL_CREATE,
                        MemoryLogConstant.EVENT_MEMORY_ACCESS_RECORDED,
                        MemoryLogConstant.EVENT_MEMORY_USAGE_RECORDED),
                agentLogService.eventNames());
        assertEquals(List.of(memory.getId()),
                agentLogService.event(1).getPayload().get(MemoryLogConstant.FIELD_MEMORY_IDS));
        assertEquals(1, agentLogService.event(1).getPayload().get(MemoryLogConstant.FIELD_PROCESSED_COUNT));
    }

    @Test
    void searchActiveMemories_recordsSearchEvent() {
        @SuppressWarnings("unchecked")
        EmbeddingSearchResult<TextSegment> searchResult = mock(EmbeddingSearchResult.class);
        when(searchResult.matches()).thenReturn(List.of());
        when(embeddingStore.search(any())).thenReturn(searchResult);

        AiMemory memory = service.createManualMemory(buildCreateRequest());

        List<?> results = service.searchActiveMemories("concise output", 5, 0.2D);

        assertEquals(1, results.size());
        AgentLogEvent event = agentLogService.lastEvent();
        assertEquals(MemoryLogConstant.EVENT_MEMORY_SEARCH, event.getPayload().get("eventName"));
        assertEquals(true, event.getPayload().get(MemoryLogConstant.FIELD_QUERY_TEXT_PRESENT));
        assertEquals(5, event.getPayload().get(MemoryLogConstant.FIELD_LIMIT));
        assertEquals(List.of(memory.getId()), event.getPayload().get(MemoryLogConstant.FIELD_MEMORY_IDS));
    }

    @Test
    void recallAccessibleMemories_browseStrategySkipsEmbeddingLookup() {
        service.seedMemory(AiMemory.builder()
                .userId(42L)
                .conversationId(7L)
                .scope("USER")
                .memoryType("PREFERENCE")
                .subType("OUTPUT_FORMAT")
                .sourceType("MANUAL")
                .content("User prefers concise output.")
                .normalizedContentKey("user prefers concise output")
                .detailJson("{}")
                .status(MemoryStatusEnum.ACTIVE.getCode())
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
                "OUTPUT_FORMAT",
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
                .subType("OUTPUT_FORMAT")
                .sourceType("MANUAL")
                .content("User prefers concise output.")
                .normalizedContentKey("user prefers concise output")
                .detailJson("{}")
                .status(MemoryStatusEnum.ACTIVE.getCode())
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
                "OUTPUT_FORMAT",
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

    private MemoryCreateRequest buildCreateRequest() {
        MemoryCreateRequest request = new MemoryCreateRequest();
        request.setConversationId(7L);
        request.setMemoryType("preference");
        request.setSubType("output_format");
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
                    .filter(memory -> memory.getStatus() != null
                            && memory.getStatus() == MemoryStatusEnum.ACTIVE.getCode())
                    .findFirst()
                    .orElse(null);
        }

        @Override
        public long count(Wrapper<AiMemory> queryWrapper) {
            return store.stream()
                    .filter(memory -> memory.getStatus() != null
                            && memory.getStatus() == MemoryStatusEnum.ACTIVE.getCode())
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

        @Override
        protected void persistMaintenanceMemory(AiMemory memory) {
            updateById(memory);
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
