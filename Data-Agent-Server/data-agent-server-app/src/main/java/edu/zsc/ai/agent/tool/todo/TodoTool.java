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
        "价值：让多步骤任务的进度可见，用户能看到正在做什么和还剩什么。",
        "使用时机：任务包含多个有意义步骤，或进度可见性本身重要。",
        "前置条件：action 必须是 CREATE、UPDATE 或 DELETE。",
        "结果：前端 todo 状态。",
        "边界：todo 状态只是进度追踪，不是业务结果。"
    })
    public AgentToolResult todoWrite(
            @P("操作类型：CREATE、UPDATE、DELETE。")
            TodoActionEnum action,
            @P("这组 todo 的唯一 ID，例如 task-1。")
            String todoId,
            @P(value = "CREATE 或 UPDATE 时传入完整任务列表；action=DELETE 时省略。", required = false)
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
                    "todo 列表 " + (todoId != null ? todoId : "") + " 已创建，包含 " + itemCount + " 项。",
                    "任务推进时持续更新这组 todo。",
                    "不要把 todo 状态当成业务结果。"
            );
            case UPDATE -> ToolMessageSupport.sentence(
                    "todo 列表 " + (todoId != null ? todoId : "") + " 已更新，包含 " + itemCount + " 项。",
                    "继续主任务，并在下一个里程碑后同步更新这组 todo。"
            );
            case DELETE -> ToolMessageSupport.sentence(
                    "todo 列表 " + (todoId != null ? todoId : "") + " 已清空。",
                    "继续实际任务流程，不要把这次 todo 操作当成面向用户的最终结果。"
            );
        };
    }
}
