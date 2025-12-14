package edu.zsc.ai.domain.model.dto.request.sys;

import lombok.Data;

@Data
public class FindSessionByTokenRequest {
    private String accessToken;
    private Long userId;
}
