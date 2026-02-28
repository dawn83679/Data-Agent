package edu.zsc.ai.tool;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import edu.zsc.ai.model.Todo;
import edu.zsc.ai.tool.model.AgentToolResult;
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
        "[WHAT] Update the task progress list displayed to the user in real time (full overwrite on each call).",
        "[WHEN] Use for any operation involving 3 or more steps to keep the user informed of the plan and progress.",
        "IMPORTANT — MUST call at the START of any multi-step operation to outline the full plan before taking any action. Update after each step completes.",
        "[HOW] Pass the same todoId for incremental updates; use a new id only after the current list is fully complete or cleared. Pass an empty list to clear."
    })
    public AgentToolResult updateTodoList(
            @P("Id of the todo list (e.g. 'list-1'). Use the same id for updates; use a new id only when creating a new list after the current one is all completed or cleared.")
            String todoId,
            @P("The complete list of todo tasks; each element has title and optional description, priority, status.")
            List<Todo> items) {
        log.info("[Tool] updateTodoList, todoId={}, itemsSize={}", todoId, items != null ? items.size() : 0);
        List<Todo> list = items != null && !items.isEmpty() ? items : List.of();
        log.info("[Tool done] updateTodoList, todoId={}, count={}", todoId, list.size());
        return AgentToolResult.success(buildTodoPayload(todoId, list));
    }

    /** Response format: { "todoId": string, "items": Todo[] }. Frontend uses todoId for single-box logic. */
    private static String buildTodoPayload(String todoId, List<Todo> items) {
        Map<String, Object> out = new HashMap<>();
        out.put("todoId", todoId != null ? todoId : "");
        out.put("items", items != null ? items : List.of());
        return JsonUtil.object2json(out);
    }
}
