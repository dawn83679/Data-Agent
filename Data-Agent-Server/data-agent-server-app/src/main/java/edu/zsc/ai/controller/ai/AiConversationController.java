package edu.zsc.ai.controller.ai;

import cn.dev33.satoken.annotation.SaCheckLogin;
import edu.zsc.ai.domain.model.dto.request.ai.conversation.ConversationListRequest;
import edu.zsc.ai.domain.model.dto.request.ai.conversation.CreateConversationRequest;
import edu.zsc.ai.domain.model.dto.request.ai.conversation.DeleteConversationRequest;
import edu.zsc.ai.domain.model.dto.request.ai.conversation.GetConversationRequest;
import edu.zsc.ai.domain.model.dto.request.ai.conversation.UpdateConversationRequest;
import edu.zsc.ai.domain.model.dto.response.base.ApiResponse;
import edu.zsc.ai.domain.model.dto.response.ai.conversation.ConversationResponse;
import edu.zsc.ai.domain.model.dto.response.base.PageResponse;
import edu.zsc.ai.domain.service.ai.AiConversationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

/**
 * Controller for AI conversation operations
 *
 * @author zgq
 */
@Slf4j
@RestController
@RequestMapping("/api/ai/conversation")
@RequiredArgsConstructor
@Validated
public class AiConversationController {

    private final AiConversationService aiConversationService;

    /**
     * Create a new conversation
     *
     * @param request conversation creation request
     * @return created conversation response
     */
    @PostMapping("/create")
    @SaCheckLogin
    public ApiResponse<ConversationResponse> createConversation(@Valid @RequestBody CreateConversationRequest request) {
        log.info("Creating conversation with title: {}", request.getTitle());

        ConversationResponse response = aiConversationService.createConversation(request);

        return ApiResponse.success(response);
    }

    /**
     * Get conversation list with pagination
     *
     * @param request pagination and filter request
     * @return paginated conversation list
     */
    @GetMapping("/list")
    @SaCheckLogin
    public ApiResponse<PageResponse<ConversationResponse>> getConversationList(@Valid ConversationListRequest request) {
        log.info("Getting conversation list: page={}, size={}, title={}",
                request.getCurrent(), request.getSize(), request.getTitle());

        PageResponse<ConversationResponse> response = aiConversationService.getConversationList(request);

        return ApiResponse.success(response);
    }

    /**
     * Get conversation details
     *
     * @param request get conversation request
     * @return conversation details
     */
    @PostMapping("/get")
    @SaCheckLogin
    public ApiResponse<ConversationResponse> getConversationById(@Valid @RequestBody GetConversationRequest request) {
        log.info("Getting conversation details: id={}", request.getId());

        ConversationResponse response = aiConversationService.getConversationById(request);

        return ApiResponse.success(response);
    }

    /**
     * Update conversation
     *
     * @param request update request
     * @return updated conversation response
     */
    @PutMapping("/update")
    @SaCheckLogin
    public ApiResponse<ConversationResponse> updateConversation(@Valid @RequestBody UpdateConversationRequest request) {
        log.info("Updating conversation: id={}, title={}", request.getId(), request.getTitle());

        ConversationResponse response = aiConversationService.updateConversation(request);

        return ApiResponse.success(response);
    }

    /**
     * Delete conversation (soft delete)
     *
     * @param request delete request
     * @return success response
     */
    @DeleteMapping("/delete")
    @SaCheckLogin
    public ApiResponse<Void> deleteConversation(@Valid @RequestBody DeleteConversationRequest request) {
        log.info("Deleting conversation: id={}", request.getId());

        aiConversationService.deleteConversation(request);

        return ApiResponse.success();
    }
}