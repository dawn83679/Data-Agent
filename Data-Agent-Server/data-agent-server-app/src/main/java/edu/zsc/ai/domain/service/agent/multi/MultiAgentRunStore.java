package edu.zsc.ai.domain.service.agent.multi;

import edu.zsc.ai.common.enums.ai.AgentModeEnum;
import edu.zsc.ai.common.enums.ai.AgentRoleEnum;
import edu.zsc.ai.domain.service.agent.multi.model.MultiAgentRun;
import edu.zsc.ai.domain.service.agent.multi.model.MultiAgentTask;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class MultiAgentRunStore {

    private final AtomicLong runSequence = new AtomicLong(1000);
    private final AtomicLong taskSequence = new AtomicLong(10000);
    private final Map<Long, MultiAgentRun> runs = new ConcurrentHashMap<>();

    public MultiAgentRun createRun(Long conversationId, Long userId, String overallGoal) {
        Long runId = runSequence.incrementAndGet();
        MultiAgentRun run = MultiAgentRun.builder()
                .runId(runId)
                .conversationId(conversationId)
                .userId(userId)
                .mode(AgentModeEnum.MULTI_AGENT.getCode())
                .overallGoal(overallGoal)
                .status("RUNNING")
                .startedAt(LocalDateTime.now())
                .build();
        runs.put(runId, run);
        return run;
    }

    public MultiAgentRun getRun(Long runId) {
        return runs.get(runId);
    }

    public MultiAgentTask createTask(Long runId, AgentRoleEnum role, String title, String goal, int sequence) {
        MultiAgentTask task = MultiAgentTask.builder()
                .taskId(taskSequence.incrementAndGet())
                .runId(runId)
                .agentRole(role)
                .title(title)
                .goal(goal)
                .status("PENDING")
                .sequence(sequence)
                .summary("")
                .build();
        runs.computeIfAbsent(runId, ignored -> MultiAgentRun.builder().build()).getTasks().add(task);
        return task;
    }

    public MultiAgentTask createTask(Long runId, AgentRoleEnum role, String title, String goal) {
        MultiAgentRun run = runs.computeIfAbsent(runId, ignored -> MultiAgentRun.builder().build());
        MultiAgentTask task = MultiAgentTask.builder()
                .taskId(taskSequence.incrementAndGet())
                .runId(runId)
                .agentRole(role)
                .title(title)
                .goal(goal)
                .status("PENDING")
                .sequence(run.getTasks().size() + 1)
                .summary("")
                .build();
        run.getTasks().add(task);
        return task;
    }

    public void markTask(Long runId, Long taskId, String status, String summary) {
        markTask(runId, taskId, status, summary, null);
    }

    public void markTask(Long runId, Long taskId, String status, String summary, String details) {
        MultiAgentRun run = runs.get(runId);
        if (run == null) {
            return;
        }
        run.getTasks().stream()
                .filter(task -> taskId.equals(task.getTaskId()))
                .findFirst()
                .ifPresent(task -> {
                    task.setStatus(status);
                    task.setSummary(summary);
                    if (details != null) {
                        task.setDetails(details);
                    }
                });
    }

    public void completeRun(Long runId, String status) {
        MultiAgentRun run = runs.get(runId);
        if (run == null) {
            return;
        }
        run.setStatus(status);
        run.setFinishedAt(LocalDateTime.now());
    }
}
