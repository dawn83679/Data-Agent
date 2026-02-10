package edu.zsc.ai.tool;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import edu.zsc.ai.model.Todo;
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
        "Update the entire todo list (full overwrite).",
        "Pass todoId to identify the list (e.g. use a new id when starting a new list after the previous one is done or cleared).",
        "Pass a list of tasks; each task has title and optional description, priority, status. Pass an empty list to clear. Data is not persisted."
    })
    public String updateTodoList(
            @P("Id of the todo list (e.g. 'list-1'). Use the same id for updates; use a new id only when creating a new list after the current one is all completed or cleared.")
            String todoId,
            @P("The complete list of todo tasks; each element has title and optional description, priority, status.")
            List<Todo> items) {
        log.info("[Tool before] updateTodoList, todoId={}, itemsSize={}", todoId, items != null ? items.size() : 0);
        List<Todo> list = items != null && !items.isEmpty() ? items : List.of();
        log.info("[Tool done] updateTodoList, todoId={}, count={}", todoId, list.size());
        return buildTodoResponse(todoId, list);
    }

    /** Response format: { "todoId": string, "items": Todo[] }. Frontend uses todoId for single-box logic. */
    private static String buildTodoResponse(String todoId, List<Todo> items) {
        Map<String, Object> out = new HashMap<>();
        out.put("todoId", todoId != null ? todoId : "");
        out.put("items", items != null ? items : List.of());
        return JsonUtil.object2json(out);
    }
}
