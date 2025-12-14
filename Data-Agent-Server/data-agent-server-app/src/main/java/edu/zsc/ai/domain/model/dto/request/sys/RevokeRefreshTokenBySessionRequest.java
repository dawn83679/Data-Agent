package edu.zsc.ai.domain.model.dto.request.sys;

import lombok.Data;

@Data
public class RevokeRefreshTokenBySessionRequest {
    private Long sessionId;
}
