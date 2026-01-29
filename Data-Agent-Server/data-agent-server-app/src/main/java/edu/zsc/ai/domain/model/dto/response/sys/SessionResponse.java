package edu.zsc.ai.domain.model.dto.response.sys;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionResponse {
    private Long id;
    private String deviceInfo;
    private String ipAddress;
    private String userAgent;
    private Boolean isCurrent;
    private LocalDateTime lastRefreshAt;
    private LocalDateTime createdAt;
}
