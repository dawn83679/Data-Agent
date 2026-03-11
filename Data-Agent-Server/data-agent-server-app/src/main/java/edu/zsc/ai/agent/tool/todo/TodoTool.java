package edu.zsc.ai.agent.tool.todo;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import edu.zsc.ai.agent.annotation.AgentTool;
import edu.zsc.ai.agent.tool.todo.model.Todo;
import edu.zsc.ai.agent.tool.model.AgentToolResult;
import edu.zsc.ai.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@AgentTool
@Slf4j
public class TodoTool {

    @Tool({
        "Calling this tool greatly improves transparency and user trust on multi-step tasks — users see ",
        "progress and what's left instead of wondering what you're doing. ",
        "Tracks progress: CREATE (start), UPDATE (sync at milestones), DELETE (clean up when done).",
        "",
        "When to Use: for tasks with 3+ steps; keep progress in sync with workflow.",
        "When NOT to Use: for single-step or two-step tasks.",
        "Relation: CREATE at start of multi-step task; UPDATE when a step completes; DELETE when the task is fully done. Use same todoId for the life of the task."
    })
    public AgentToolResult todoWrite(
            @P("Action type: CREATE, UPDATE, DELETE.")
            TodoActionEnum action,
            @P("Unique id for this todo list (e.g. 'task-1').")
            String todoId,
            @P(value = "The complete task list for CREATE/UPDATE. Omit when action=DELETE.", required = false)
            List<Todo> items) {
        int itemSize = items != null ? items.size() : 0;
        log.info("[Tool] todo_write, action={}, todoId={}, itemsSize={}", action, todoId, itemSize);
        try {
            return switch (action) {
                case CREATE, UPDATE -> {
                    List<Todo> list = items != null && !items.isEmpty() ? items : List.of();
                    log.info("[Tool done] todo_write, action={}, todoId={}, count={}", action, todoId, list.size());
                    yield AgentToolResult.success(buildTodoPayload(todoId, list));
                }
                case DELETE -> {
                    log.info("[Tool done] todo_write, action={}, todoId={}", action, todoId);
                    yield AgentToolResult.success(buildTodoPayload(todoId, List.of()));
                }
            };
        } catch (Exception e) {
            log.error("[Tool error] todo_write, action={}, todoId={}", action, todoId, e);
            return AgentToolResult.fail("Failed to execute todoWrite (action=" + action
                    + ", todoId='" + todoId + "'): " + e.getMessage());
        }
    }

    /** Response format: { "todoId": string, "items": Todo[] }. Frontend uses todoId for single-box logic. */
    private static String buildTodoPayload(String todoId, List<Todo> items) {
        Map<String, Object> out = new HashMap<>();
        out.put("todoId", todoId != null ? todoId : "");
        out.put("items", items != null ? items : List.of());
        return JsonUtil.object2json(out);
    }
}
