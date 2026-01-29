package edu.zsc.ai.controller;

import edu.zsc.ai.domain.model.dto.response.sys.SessionResponse;
import edu.zsc.ai.domain.service.sys.SysSessionsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@Validated
@RequestMapping("/api/session")
public class SessionController {

    @Autowired
    private SysSessionsService sysSessionsService;

    @GetMapping("/list")
    public List<SessionResponse> listActiveSessions() {
        return sysSessionsService.listActiveSessionsByUserId();
    }

    @DeleteMapping("/{sessionId}")
    public Boolean revokeSession(@PathVariable Long sessionId) {
        return sysSessionsService.revokeSessionById(sessionId);
    }

}
