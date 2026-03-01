package edu.zsc.ai.agent.tool;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import edu.zsc.ai.agent.tool.model.Todo;
import edu.zsc.ai.agent.tool.model.AgentToolResult;
import edu.zsc.ai.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class TodoTool {

    @Tool({
        "[WHAT] Create a real-time step-by-step task list visible to the user.",
        "[WHEN] Call at the START of any operation with 3 or more steps, BEFORE taking any other action.",
        "[HOW] Pass a unique todoId and the full list of planned steps (first step IN_PROGRESS, rest NOT_STARTED)."
    })
    public AgentToolResult todo_create(
            @P("Unique id for this todo list (e.g. 'task-1').")
            String todoId,
            @P("The complete list of planned tasks; first step IN_PROGRESS, others NOT_STARTED.")
            List<Todo> items) {
        log.info("[Tool] todo_create, todoId={}, itemsSize={}", todoId, items != null ? items.size() : 0);
        List<Todo> list = items != null && !items.isEmpty() ? items : List.of();
        log.info("[Tool done] todo_create, todoId={}, count={}", todoId, list.size());
        return AgentToolResult.success(buildTodoPayload(todoId, list));
    }

    @Tool({
        "[WHAT] Update the task progress list after a step completes.",
        "[WHEN] Call after each step finishes to reflect current progress.",
        "[HOW] Use the same todoId as todo_create; pass the full updated items list with revised statuses."
    })
    public AgentToolResult todo_update(
            @P("Same todoId used in todo_create.")
            String todoId,
            @P("The complete updated list with revised statuses.")
            List<Todo> items) {
        log.info("[Tool] todo_update, todoId={}, itemsSize={}", todoId, items != null ? items.size() : 0);
        List<Todo> list = items != null && !items.isEmpty() ? items : List.of();
        log.info("[Tool done] todo_update, todoId={}, count={}", todoId, list.size());
        return AgentToolResult.success(buildTodoPayload(todoId, list));
    }

    @Tool({
        "[WHAT] Clear the task list when all steps are fully done.",
        "[WHEN] Call after all tasks are COMPLETED to dismiss the progress box.",
        "[HOW] Pass the todoId of the list to clear; returns an empty list."
    })
    public AgentToolResult todo_delete(
            @P("The todoId of the list to clear.")
            String todoId) {
        log.info("[Tool] todo_delete, todoId={}", todoId);
        log.info("[Tool done] todo_delete, todoId={}", todoId);
        return AgentToolResult.success(buildTodoPayload(todoId, List.of()));
    }

    /** Response format: { "todoId": string, "items": Todo[] }. Frontend uses todoId for single-box logic. */
    private static String buildTodoPayload(String todoId, List<Todo> items) {
        Map<String, Object> out = new HashMap<>();
        out.put("todoId", todoId != null ? todoId : "");
        out.put("items", items != null ? items : List.of());
        return JsonUtil.object2json(out);
    }
}
