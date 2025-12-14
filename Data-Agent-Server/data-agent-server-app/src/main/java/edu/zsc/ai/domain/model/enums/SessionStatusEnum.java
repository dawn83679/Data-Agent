package edu.zsc.ai.domain.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum SessionStatusEnum {
    INACTIVE(0, "Inactive"),
    ACTIVE(1, "Active");

    private final Integer value;
    private final String description;
}
