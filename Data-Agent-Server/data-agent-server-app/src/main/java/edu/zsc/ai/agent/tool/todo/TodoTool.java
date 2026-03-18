package edu.zsc.ai.agent.tool.todo;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import edu.zsc.ai.agent.annotation.AgentTool;
import edu.zsc.ai.agent.tool.message.ToolMessageSupport;
import edu.zsc.ai.agent.tool.todo.model.Todo;
import edu.zsc.ai.agent.tool.model.AgentToolResult;
import edu.zsc.ai.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@AgentTool
@Slf4j
public class TodoTool {

    @Tool({
        "Value: keeps multi-step progress visible so the user can see what is happening and what remains.",
        "Use When: call for tasks with 3 or more meaningful steps, or whenever progress updates improve transparency.",
        "After Success: continue the main task and keep the same todoId synchronized at each milestone.",
        "After Failure: continue the main task flow without pretending the todo state is the business result. Retry only if progress tracking still matters.",
        "Relation: CREATE at the start, UPDATE after meaningful progress, and DELETE when the task is done. This tracks workflow only and does not replace the real result."
    })
    public AgentToolResult todoWrite(
            @P("Action type: CREATE, UPDATE, DELETE.")
            TodoActionEnum action,
            @P("Unique id for this todo list (e.g. 'task-1').")
            String todoId,
            @P(value = "The complete task list for CREATE/UPDATE. Omit when action=DELETE.", required = false)
            List<Todo> items) {
        int itemSize = CollectionUtils.size(items);
        log.info("[Tool] todo_write, action={}, todoId={}, itemsSize={}", action, todoId, itemSize);
        return switch (action) {
            case CREATE, UPDATE -> {
                List<Todo> list = CollectionUtils.isEmpty(items) ? List.of() : items;
                log.info("[Tool done] todo_write, action={}, todoId={}, count={}", action, todoId, list.size());
                yield AgentToolResult.success(
                        buildTodoPayload(todoId, list),
                        buildTodoMessage(action, todoId, list.size())
                );
            }
            case DELETE -> {
                log.info("[Tool done] todo_write, action={}, todoId={}", action, todoId);
                yield AgentToolResult.success(
                        buildTodoPayload(todoId, List.of()),
                        buildTodoMessage(action, todoId, 0)
                );
            }
        };
    }

    /** Response format: { "todoId": string, "items": Todo[] }. Frontend uses todoId for single-box logic. */
    private static String buildTodoPayload(String todoId, List<Todo> items) {
        Map<String, Object> out = new HashMap<>();
        out.put("todoId", todoId != null ? todoId : "");
        out.put("items", items != null ? items : List.of());
        return JsonUtil.object2json(out);
    }

    private static String buildTodoMessage(TodoActionEnum action, String todoId, int itemCount) {
        return switch (action) {
            case CREATE -> ToolMessageSupport.sentence(
                    "Todo list " + (todoId != null ? todoId : "") + " was created with " + itemCount + " item(s).",
                    "Keep this todo list updated as the task progresses.",
                    "Do not treat the todo state as the business result."
            );
            case UPDATE -> ToolMessageSupport.sentence(
                    "Todo list " + (todoId != null ? todoId : "") + " was updated with " + itemCount + " item(s).",
                    "Continue the main task and sync this todo list again after the next milestone."
            );
            case DELETE -> ToolMessageSupport.sentence(
                    "Todo list " + (todoId != null ? todoId : "") + " was cleared.",
                    "Continue with the actual task flow and do not treat this todo operation as the final user-facing result."
            );
        };
    }
}
