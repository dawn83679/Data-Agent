package edu.zsc.ai.api.controller.ai;

import cn.dev33.satoken.stp.StpUtil;
import edu.zsc.ai.agent.confirm.WriteConfirmationStore;
import edu.zsc.ai.api.model.request.WriteConfirmRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@RestController
@RequestMapping("/api/chat/write-confirm")
@RequiredArgsConstructor
public class WriteConfirmController {

    private final WriteConfirmationStore confirmationStore;

    /**
     * User clicked "Confirm & Execute": mark token CONFIRMED.
     * Frontend should then submit a chat message to trigger the next agent run.
     */
    @PostMapping("/confirm")
    public void confirm(@Valid @RequestBody WriteConfirmRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        if (!confirmationStore.confirm(request.getConfirmationToken(), userId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid, expired, or already-used confirmation token.");
        }
    }

    /**
     * User clicked "Cancel": invalidate the token so it can never be consumed.
     * Frontend should then submit a cancellation message to inform the agent.
     */
    @PostMapping("/cancel")
    public void cancel(@Valid @RequestBody WriteConfirmRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        if (!confirmationStore.cancel(request.getConfirmationToken(), userId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Token not found, expired, or not owned by current user.");
        }
    }
}
