package edu.zsc.ai.controller.ai;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import edu.zsc.ai.common.converter.ai.ConversationConverter;
import edu.zsc.ai.domain.model.dto.request.base.PageRequest;
import edu.zsc.ai.domain.model.dto.request.ai.ConversationUpdateRequest;
import edu.zsc.ai.domain.model.dto.response.base.ApiResponse;
import edu.zsc.ai.domain.model.dto.response.base.PageResponse;
import edu.zsc.ai.domain.model.dto.response.ai.ConversationResponse;
import edu.zsc.ai.domain.model.entity.ai.AiConversation;
import edu.zsc.ai.domain.service.ai.AiConversationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Conversation management API.
 * All operations are scoped to the current login user.
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private final AiConversationService aiConversationService;

    @GetMapping
    public ApiResponse<PageResponse<ConversationResponse>> list(
            @RequestParam(defaultValue = "1") @Min(1) Integer current,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) Integer size) {
        PageRequest pageRequest = PageRequest.builder().current(current).size(size).build();
        Page<AiConversation> page = aiConversationService.pageByCurrentUser(pageRequest);
        List<ConversationResponse> records = page.getRecords().stream()
                .map(ConversationConverter::toResponse)
                .toList();
        PageResponse<ConversationResponse> body = PageResponse.<ConversationResponse>builder()
                .current(page.getCurrent())
                .size(page.getSize())
                .total(page.getTotal())
                .pages(page.getPages())
                .records(records)
                .build();
        return ApiResponse.success(body);
    }

    @GetMapping("/{id}")
    public ApiResponse<ConversationResponse> getById(@PathVariable @NotNull Long id) {
        AiConversation conversation = aiConversationService.getByIdForCurrentUser(id);
        return ApiResponse.success(ConversationConverter.toResponse(conversation));
    }

    @PostMapping("/{id}")
    public ApiResponse<ConversationResponse> update(
            @PathVariable @NotNull Long id,
            @Valid @RequestBody ConversationUpdateRequest request) {
        AiConversation updated = aiConversationService.updateTitle(id, request.getTitle());
        return ApiResponse.success(ConversationConverter.toResponse(updated));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable @NotNull Long id) {
        aiConversationService.deleteByCurrentUser(id);
        return ApiResponse.success();
    }
}
