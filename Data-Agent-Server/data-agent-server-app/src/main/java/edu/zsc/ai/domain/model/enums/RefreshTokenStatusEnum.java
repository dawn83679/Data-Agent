package edu.zsc.ai.domain.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum RefreshTokenStatusEnum {
    NOT_REVOKED(0),
    REVOKED(1);

    private final Integer value;
}
