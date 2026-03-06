package edu.zsc.ai.agent.tool.todo;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import edu.zsc.ai.agent.tool.annotation.AgentTool;
import edu.zsc.ai.agent.tool.model.Todo;
import edu.zsc.ai.agent.tool.model.AgentToolResult;
import edu.zsc.ai.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;


import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@AgentTool
@Slf4j
public class TodoTool {

    @Tool({
        "[GOAL] Maintain one user-visible todo list for complex workflows.",
        "[WHEN] Use for CREATE (initialize), UPDATE (milestone sync), DELETE (clear after completion).",
        "[INPUT] action must be CREATE/UPDATE/DELETE. todoId is required for all actions. items are required for CREATE/UPDATE and ignored for DELETE.",
        "[AFTER] Keep statuses aligned with real execution progress before user-facing summaries."
    })
    public AgentToolResult todoWrite(
            @P("Action type: CREATE, UPDATE, DELETE.")
            String action,
            @P("Unique id for this todo list (e.g. 'task-1').")
            String todoId,
            @P(value = "The complete task list for CREATE/UPDATE. Omit when action=DELETE.", required = false)
            List<Todo> items) {
        String normalizedAction = StringUtils.trimToEmpty(action).toUpperCase(Locale.ROOT);
        int itemSize = items != null ? items.size() : 0;
        log.info("[Tool] todo_write, action={}, todoId={}, itemsSize={}", normalizedAction, todoId, itemSize);
        try {
            return switch (normalizedAction) {
                case "CREATE", "UPDATE" -> {
                    List<Todo> list = items != null && !items.isEmpty() ? items : List.of();
                    log.info("[Tool done] todo_write, action={}, todoId={}, count={}", normalizedAction, todoId, list.size());
                    yield AgentToolResult.success(buildTodoPayload(todoId, list));
                }
                case "DELETE" -> {
                    log.info("[Tool done] todo_write, action={}, todoId={}", normalizedAction, todoId);
                    yield AgentToolResult.success(buildTodoPayload(todoId, List.of()));
                }
                default -> AgentToolResult.fail(new IllegalArgumentException(
                        "Unsupported action for todo_write: " + action + ". Allowed values: CREATE, UPDATE, DELETE"));
            };
        } catch (Exception e) {
            log.error("[Tool error] todo_write, action={}, todoId={}", normalizedAction, todoId, e);
            return AgentToolResult.fail(e);
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
