package edu.zsc.ai.domain.service.ai;

import java.util.List;

import com.baomidou.mybatisplus.extension.service.IService;

import edu.zsc.ai.domain.model.dto.request.ai.MemoryCreateRequest;
import edu.zsc.ai.domain.model.dto.request.ai.MemoryMutationRequest;
import edu.zsc.ai.domain.model.dto.request.ai.MemoryUpdateRequest;
import edu.zsc.ai.domain.model.entity.ai.AiMemory;
import edu.zsc.ai.domain.model.dto.request.base.PageRequest;
import edu.zsc.ai.domain.model.dto.response.base.PageResponse;
import edu.zsc.ai.domain.service.ai.model.MemoryMaintenanceReport;
import edu.zsc.ai.domain.service.ai.model.MemorySearchResult;
import edu.zsc.ai.domain.service.ai.model.MemoryWriteResult;
import edu.zsc.ai.domain.service.ai.recall.MemoryRecallQuery;

public interface MemoryService extends IService<AiMemory> {

    List<MemorySearchResult> searchEnabledMemories(String queryText, int limit, double minScore, String memoryType, String scope);

    List<MemorySearchResult> recallAccessibleMemories(Long conversationId, String queryText, double minScore);

    List<MemorySearchResult> recallAccessibleMemories(Long conversationId, String queryText, double minScore, String scope);

    List<MemorySearchResult> recallAccessibleMemories(MemoryRecallQuery query);

    PageResponse<AiMemory> pageCurrentUserMemories(PageRequest pageRequest,
                                                   String keyword,
                                                   String memoryType,
                                                   Integer enable,
                                                   String scope);

    AiMemory getByIdForCurrentUser(Long memoryId);

    AiMemory createManualMemory(MemoryCreateRequest request);

    AiMemory updateMemory(Long memoryId, MemoryUpdateRequest request);

    MemoryWriteResult mutateAgentMemory(MemoryMutationRequest request);

    AiMemory disableMemory(Long memoryId);

    AiMemory enableMemory(Long memoryId);

    void deleteMemory(Long memoryId);

    MemoryMaintenanceReport inspectCurrentUserMaintenance();

    MemoryMaintenanceReport runCurrentUserMaintenance();

    MemoryMaintenanceReport runGlobalMaintenance();

    void recordMemoryAccess(List<Long> memoryIds);
}
