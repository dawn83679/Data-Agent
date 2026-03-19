package edu.zsc.ai.domain.service.ai;

import java.util.List;

import com.baomidou.mybatisplus.extension.service.IService;

import edu.zsc.ai.domain.model.dto.request.ai.MemoryCreateRequest;
import edu.zsc.ai.domain.model.dto.request.ai.MemoryWriteRequest;
import edu.zsc.ai.domain.model.dto.request.ai.MemoryUpdateRequest;
import edu.zsc.ai.domain.model.entity.ai.AiMemory;
import edu.zsc.ai.domain.model.dto.request.base.PageRequest;
import edu.zsc.ai.domain.model.dto.response.base.PageResponse;
import edu.zsc.ai.domain.service.ai.model.MemoryMaintenanceReport;
import edu.zsc.ai.domain.service.ai.model.MemorySearchResult;

public interface MemoryService extends IService<AiMemory> {

    List<MemorySearchResult> searchActiveMemories(String queryText, int limit, double minScore);

    List<MemorySearchResult> recallAccessibleMemories(Long conversationId, String queryText, double minScore);

    PageResponse<AiMemory> pageCurrentUserMemories(PageRequest pageRequest,
                                                   String keyword,
                                                   String memoryType,
                                                   Integer status,
                                                   String reviewState,
                                                   String scope);

    AiMemory getByIdForCurrentUser(Long memoryId);

    AiMemory createManualMemory(MemoryCreateRequest request);

    AiMemory updateMemory(Long memoryId, MemoryUpdateRequest request);

    AiMemory writeAgentMemory(MemoryWriteRequest request);

    AiMemory confirmMemory(Long memoryId);

    AiMemory markMemoryNeedsReview(Long memoryId);

    AiMemory archiveMemory(Long memoryId);

    AiMemory restoreMemory(Long memoryId);

    void deleteMemory(Long memoryId);

    MemoryMaintenanceReport inspectCurrentUserMaintenance();

    MemoryMaintenanceReport runCurrentUserMaintenance();

    MemoryMaintenanceReport runGlobalMaintenance();

    void recordMemoryAccess(List<Long> memoryIds);

    void recordMemoryUsage(List<Long> memoryIds);
}
