package edu.zsc.ai.api.controller.ai;

import java.util.List;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import edu.zsc.ai.common.converter.ai.MemoryConverter;
import edu.zsc.ai.domain.model.dto.request.ai.MemoryCreateRequest;
import edu.zsc.ai.domain.model.dto.request.ai.MemorySemanticSearchRequest;
import edu.zsc.ai.domain.model.dto.request.ai.MemoryUpdateRequest;
import edu.zsc.ai.domain.model.dto.response.ai.MemoryMetadataResponse;
import edu.zsc.ai.domain.model.dto.request.base.PageRequest;
import edu.zsc.ai.domain.model.dto.response.ai.MemoryResponse;
import edu.zsc.ai.domain.model.dto.response.base.ApiResponse;
import edu.zsc.ai.domain.model.dto.response.base.PageResponse;
import edu.zsc.ai.domain.model.entity.ai.AiMemory;
import edu.zsc.ai.domain.service.ai.MemoryService;
import edu.zsc.ai.domain.service.ai.model.MemorySearchResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;

@Validated
@RestController
@RequestMapping("/api/memories")
@RequiredArgsConstructor
public class MemoryController {

    private final MemoryService memoryService;

    @GetMapping
    public ApiResponse<PageResponse<MemoryResponse>> list(
            @RequestParam(defaultValue = "1") @Min(1) Integer current,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String memoryType,
            @RequestParam(required = false) Integer enable,
            @RequestParam(required = false) String scope) {
        PageResponse<AiMemory> page = memoryService.pageCurrentUserMemories(
                PageRequest.builder().current(current).size(size).build(),
                keyword,
                memoryType,
                enable,
                scope);
        PageResponse<MemoryResponse> body = PageResponse.<MemoryResponse>builder()
                .current(page.getCurrent())
                .size(page.getSize())
                .total(page.getTotal())
                .pages(page.getPages())
                .records(page.getRecords().stream().map(MemoryConverter::toMemoryResponse).toList())
                .build();
        return ApiResponse.success(body);
    }

    @GetMapping("/metadata")
    public ApiResponse<MemoryMetadataResponse> metadata() {
        return ApiResponse.success(MemoryMetadataResponse.fromEnums());
    }

    @GetMapping("/{id}")
    public ApiResponse<MemoryResponse> getById(@PathVariable @NotNull Long id) {
        return ApiResponse.success(MemoryConverter.toMemoryResponse(memoryService.getByIdForCurrentUser(id)));
    }

    @PostMapping("/search")
    public ApiResponse<List<MemorySearchResult>> search(@Valid @RequestBody MemorySemanticSearchRequest request) {
        List<MemorySearchResult> results = memoryService.searchEnabledMemories(
                request.getQueryText(),
                request.getLimit(),
                request.getMinScore(),
                request.getMemoryType() == null ? null : request.getMemoryType().getCode(),
                request.getScope() == null ? null : request.getScope().getCode());
        return ApiResponse.success(results);
    }

    @PostMapping
    public ApiResponse<MemoryResponse> create(@Valid @RequestBody MemoryCreateRequest request) {
        AiMemory created = memoryService.createManualMemory(request);
        return ApiResponse.success(MemoryConverter.toMemoryResponse(created));
    }

    @PutMapping("/{id}")
    public ApiResponse<MemoryResponse> update(@PathVariable @NotNull Long id,
                                              @Valid @RequestBody MemoryUpdateRequest request) {
        AiMemory updated = memoryService.updateMemory(id, request);
        return ApiResponse.success(MemoryConverter.toMemoryResponse(updated));
    }

    @PostMapping("/{id}/disable")
    public ApiResponse<MemoryResponse> disableMemory(@PathVariable @NotNull Long id) {
        return ApiResponse.success(MemoryConverter.toMemoryResponse(memoryService.disableMemory(id)));
    }

    @PostMapping("/{id}/enable")
    public ApiResponse<MemoryResponse> enableMemory(@PathVariable @NotNull Long id) {
        return ApiResponse.success(MemoryConverter.toMemoryResponse(memoryService.enableMemory(id)));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable @NotNull Long id) {
        memoryService.deleteMemory(id);
        return ApiResponse.success();
    }
}
