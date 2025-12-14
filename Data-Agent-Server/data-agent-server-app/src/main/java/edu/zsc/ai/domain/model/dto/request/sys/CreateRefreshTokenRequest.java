package edu.zsc.ai.domain.model.dto.request.sys;

import lombok.Data;

@Data
public class CreateRefreshTokenRequest {
    private Long userId;
    private Long sessionId;
}
