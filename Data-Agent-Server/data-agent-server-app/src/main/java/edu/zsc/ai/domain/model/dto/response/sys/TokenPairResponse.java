package edu.zsc.ai.domain.model.dto.response.sys;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TokenPairResponse {
    private String accessToken;
    private String refreshToken;
}
