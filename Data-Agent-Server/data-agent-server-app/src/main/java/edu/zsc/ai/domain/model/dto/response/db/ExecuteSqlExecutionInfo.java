package edu.zsc.ai.domain.model.dto.response.db;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecuteSqlExecutionInfo {
    private String executionId;
    private Long startTime;
    private Long endTime;
    private Long durationMs;
    private Long executionMs;
    private Long fetchingMs;
    private Integer affectedRows;
    private Integer fetchRows;
    private Boolean truncated;
    private Boolean limitApplied;
}
