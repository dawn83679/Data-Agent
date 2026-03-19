package edu.zsc.ai.domain.service.ai.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
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
import dev.langchain4j.store.embedding.EmbeddingStore;
import edu.zsc.ai.common.enums.ai.MemoryStatusEnum;
import edu.zsc.ai.config.ai.MemoryProperties;
import edu.zsc.ai.context.RequestContext;
import edu.zsc.ai.context.RequestContextInfo;
import edu.zsc.ai.domain.exception.BusinessException;
import edu.zsc.ai.domain.model.dto.request.ai.MemoryCreateRequest;
import edu.zsc.ai.domain.model.dto.request.ai.MemoryUpdateRequest;
import edu.zsc.ai.domain.model.dto.request.ai.MemoryWriteRequest;
import edu.zsc.ai.domain.model.entity.ai.AiMemory;
import edu.zsc.ai.domain.service.ai.model.MemoryMaintenanceReport;

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
        memoryProperties.getMaintenance().setArchiveExpiredEnabled(true);
        memoryProperties.getMaintenance().setHideDuplicateEnabled(true);

        service = new InMemoryMemoryService(store, embeddingModel, memoryProperties);
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
        updateRequest.setReviewState("needs_review");
        updateRequest.setSourceType("manual");
        updateRequest.setConfidenceScore(0.88);
        updateRequest.setSalienceScore(0.77);
        updateRequest.setExpiresAt(LocalDateTime.of(2026, 4, 1, 0, 0));

        AiMemory updated = service.updateMemory(created.getId(), updateRequest);

        assertEquals("BUSINESS_RULE", updated.getMemoryType());
        assertEquals("CONVERSATION", updated.getScope());
        assertEquals("NEEDS_REVIEW", updated.getReviewState());
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

        AiMemory created = service.writeAgentMemory(MemoryWriteRequest.builder()
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

        AiMemory updated = service.writeAgentMemory(MemoryWriteRequest.builder()
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

        assertEquals(created.getId(), updated.getId());
        assertEquals(1, service.memoryStoreSize());
        assertEquals("WORKSPACE", updated.getScope());
        assertEquals("WORKFLOW_CONSTRAINT", updated.getMemoryType());
        assertEquals("IMPLEMENTATION_CONSTRAINT", updated.getSubType());
        assertEquals("NEEDS_REVIEW", updated.getReviewState());
        assertEquals("AGENT", updated.getSourceType());
        assertEquals("SCHEMA", updated.getWorkspaceLevel());
        assertEquals("9:analytics:public", updated.getWorkspaceContextKey());
        assertEquals("always use catalogname instead of databasename", updated.getNormalizedContentKey());
        assertEquals("Memory V2", updated.getTitle());
        assertEquals("Always use catalogName instead of databaseName!", updated.getContent());
        assertEquals("[\"msg-1\",\"msg-2\"]", updated.getSourceMessageIds());
        assertEquals(0.98, updated.getConfidenceScore());
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
        AiMemory created = service.writeAgentMemory(MemoryWriteRequest.builder()
                .scope("workspace")
                .workspaceLevel("catalog")
                .workspaceConnectionId(9L)
                .workspaceCatalogName("analytics")
                .memoryType("business_rule")
                .subType("domain_rule")
                .title("Catalog rule")
                .content("Analytics uses cents for money.")
                .build());

        assertEquals("CATALOG", created.getWorkspaceLevel());
        assertEquals("9:analytics", created.getWorkspaceContextKey());
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

        assertEquals("subType 'domain_rule' does not belong to memoryType 'PREFERENCE'. Valid subTypes: RESPONSE_STYLE, OUTPUT_FORMAT, INTERACTION_STYLE, DECISION_STYLE",
                exception.getMessage());
    }

    @Test
    void updateMemory_rejectsInvalidReviewState() {
        MemoryCreateRequest createRequest = new MemoryCreateRequest();
        createRequest.setConversationId(7L);
        createRequest.setMemoryType("preference");
        createRequest.setSubType("output_format");
        createRequest.setContent("User prefers concise SQL explanations.");
        createRequest.setScope("user");

        AiMemory created = service.createManualMemory(createRequest);

        MemoryUpdateRequest updateRequest = new MemoryUpdateRequest();
        updateRequest.setMemoryType("preference");
        updateRequest.setSubType("output_format");
        updateRequest.setContent("User prefers concise SQL explanations.");
        updateRequest.setScope("user");
        updateRequest.setReviewState("bad_state");

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.updateMemory(created.getId(), updateRequest));

        assertEquals("Unsupported reviewState 'bad_state'. Valid values: USER_CONFIRMED, NEEDS_REVIEW",
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
                .reviewState("USER_CONFIRMED")
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
                .reviewState("USER_CONFIRMED")
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
                .reviewState("USER_CONFIRMED")
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
                .reviewState("USER_CONFIRMED")
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
    }

    @Test
    void confirmAndNeedsReviewUpdateReviewState() {
        AiMemory memory = service.createManualMemory(buildCreateRequest());

        AiMemory needsReview = service.markMemoryNeedsReview(memory.getId());
        assertEquals("NEEDS_REVIEW", needsReview.getReviewState());

        AiMemory confirmed = service.confirmMemory(memory.getId());
        assertEquals("USER_CONFIRMED", confirmed.getReviewState());
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
        private long nextId = 1L;

        private InMemoryMemoryService(EmbeddingStore<TextSegment> embeddingStore,
                                      EmbeddingModel embeddingModel,
                                      MemoryProperties memoryProperties) {
            super(embeddingStore, embeddingModel, memoryProperties);
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
}
