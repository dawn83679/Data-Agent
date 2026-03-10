package edu.zsc.ai.common.enums.ai;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AgentRoleEnum {

    ORCHESTRATOR("orchestrator"),
    SCHEMA_ANALYST("schema_analyst"),
    SQL_PLANNER("sql_planner"),
    SQL_EXECUTOR("sql_executor"),
    RESULT_ANALYST("result_analyst");

    private final String code;
}
