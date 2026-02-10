package edu.zsc.ai.tool;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.invocation.InvocationParameters;
import edu.zsc.ai.common.constant.RequestContextConstant;
import edu.zsc.ai.domain.model.entity.ai.AiTodoTask;
import edu.zsc.ai.domain.service.ai.AiTodoTaskService;
import edu.zsc.ai.model.Todo;
import edu.zsc.ai.model.TodoList;
import edu.zsc.ai.common.enums.ai.TodoPriorityEnum;
import edu.zsc.ai.common.enums.ai.TodoStatusEnum;
import edu.zsc.ai.model.request.TodoRequest;
import edu.zsc.ai.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;


@Component
@Slf4j
@RequiredArgsConstructor
public class TodoTool {

    private final AiTodoTaskService aiTodoTaskService;


    @Tool({
            "Retrieve the entire current list of tasks for this conversation.",
            "Call this before updating the list to see existing tasks."
    })
    public String getTodoList(InvocationParameters parameters) {
        log.info("[Tool before] getTodoList");
        Long userId = resolveUserId(parameters);
        if (userId == null) {
            return "User context missing.";
        }
        Long conversationId = parameters.get(RequestContextConstant.CONVERSATION_ID);
        AiTodoTask task = aiTodoTaskService.getByConversationId(conversationId, userId);
        String result = (task == null || task.getContent() == null) ? "[]" : task.getContent();
        log.info("[Tool done] getTodoList, conversationId={}", conversationId);
        return result;
    }


    @Tool({
            "Update the entire todo list for this conversation (full overwrite).",
            "Pass a list of tasks; each task's fields are described in the parameter.",
            "Pass an empty list to clear all tasks."
    })
    public String updateTodoList(@P("The complete list of todo tasks; each element has title and optional description, "
            + "priority; status is for updates only") List<TodoRequest> requests, InvocationParameters parameters) {
        log.info("[Tool before] updateTodoList, requestsSize={}", requests != null ? requests.size() : 0);
        Long userId = resolveUserId(parameters);
        if (userId == null) {
            return "User context missing.";
        }
        Long cid = parameters.get(RequestContextConstant.CONVERSATION_ID);
        if (requests == null || requests.isEmpty()) {
            aiTodoTaskService.removeByConversationId(cid, userId);
            log.info("[Tool done] updateTodoList -> Cleared.");
            return "Cleared.";
        }

        List<Todo> todos = requests.stream().map(req -> Todo.builder()
                .title(req.getTitle())
                .description(req.getDescription())
                .status(req.getStatus() != null ? req.getStatus() : TodoStatusEnum.NOT_STARTED.name())
                .priority(req.getPriority() != null ? req.getPriority() : TodoPriorityEnum.MEDIUM.name())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build()).toList();

        String content = JsonUtil.object2json(TodoList.builder().conversationId(cid).todos(todos).updatedAt(LocalDateTime.now()).build());
        AiTodoTask task = AiTodoTask.builder().conversationId(cid).content(content).build();

        if (aiTodoTaskService.getByConversationId(cid, userId) == null) {
            aiTodoTaskService.saveByConversationId(task);
        } else {
            aiTodoTaskService.updateByConversationId(task, userId);
        }

        log.info("[Tool done] updateTodoList, conversationId={}, todosCount={}", cid, todos.size());
        return "Operation completed successfully.";
    }

    private static Long resolveUserId(InvocationParameters parameters) {
        Object v = parameters.get(RequestContextConstant.USER_ID);
        if (v == null) {
            return null;
        }
        if (v instanceof Long l) {
            return l;
        }
        if (v instanceof Number n) {
            return n.longValue();
        }
        return null;
    }
}
