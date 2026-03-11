package edu.zsc.ai.common.enums.ai;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AgentRoleEnum {

    ORCHESTRATOR("orchestrator"),
    SCHEMA_EXPLORER("schema_explorer"),
    DATA_ANALYST("data_analyst"),
    DATA_WRITER("data_writer");

    private final String code;
}
