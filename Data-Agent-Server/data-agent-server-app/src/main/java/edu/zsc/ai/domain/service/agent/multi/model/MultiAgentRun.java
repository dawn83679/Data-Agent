package edu.zsc.ai.domain.service.agent.multi.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MultiAgentRun {

    private Long runId;
    private Long conversationId;
    private Long userId;
    private String mode;
    private String overallGoal;
    private String status;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;

    @Builder.Default
    private List<MultiAgentTask> tasks = new ArrayList<>();
}
