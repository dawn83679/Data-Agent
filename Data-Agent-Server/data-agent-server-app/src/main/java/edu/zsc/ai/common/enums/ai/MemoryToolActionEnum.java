package edu.zsc.ai.common.enums.ai;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MemoryToolActionEnum {

    CREATED("CREATED"),
    UPDATED("UPDATED"),
    DELETED("DELETED"),
    UNKNOWN("UNKNOWN");

    private final String code;
}
