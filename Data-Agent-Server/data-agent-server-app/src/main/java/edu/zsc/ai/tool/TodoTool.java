package edu.zsc.ai.tool;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import edu.zsc.ai.context.RequestContext;
import edu.zsc.ai.domain.model.entity.ai.AiTodoTask;
import edu.zsc.ai.domain.service.ai.AiTodoTaskService;
import edu.zsc.ai.model.Todo;
import edu.zsc.ai.model.TodoList;
import edu.zsc.ai.common.enums.ai.TodoPriorityEnum;
import edu.zsc.ai.common.enums.ai.TodoStatusEnum;
import edu.zsc.ai.model.request.TodoRequest;
import edu.zsc.ai.util.JsonUtil;
import edu.zsc.ai.util.ToolResultFormatter;
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


    @Tool("Retrieve the entire current list of tasks. Use this to see all tasks before making updates.")
    public String getTodoList() {
        AiTodoTask task = aiTodoTaskService.getByConversationId(RequestContext.getConversationId());
        return (task == null || task.getContent() == null) ? ToolResultFormatter.empty("[]") : ToolResultFormatter.success(task.getContent());
    }


    @Tool("Update the entire todo list (Full Overwrite). Valid status: NOT_STARTED, IN_PROGRESS, PAUSED, COMPLETED. Valid priority: LOW, MEDIUM, HIGH.")
    public String updateTodoList(@P("The complete list of tasks") List<TodoRequest> requests) {
        Long cid = RequestContext.getConversationId();
        if (requests == null || requests.isEmpty()) {
            aiTodoTaskService.removeByConversationId(cid);
            return ToolResultFormatter.success("Cleared.");
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

        if (aiTodoTaskService.getByConversationId(cid) == null) {
            aiTodoTaskService.saveByConversationId(task);
        } else {
            aiTodoTaskService.updateByConversationId(task);
        }

        return ToolResultFormatter.success();
    }
}
