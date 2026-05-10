package edu.zsc.ai.agent.tool.todo.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.langchain4j.model.output.structured.Description;
import edu.zsc.ai.common.enums.ai.TodoPriorityEnum;
import edu.zsc.ai.common.enums.ai.TodoStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Description("单个 todo 任务。更新 todo 列表时作为 items 中的元素。")
public class Todo {

    @Description("任务标题。必填。")
    private String title;

    @Description("可选的任务详细说明。")
    @JsonProperty(required = false)
    private String description;

    @Description("可选。取值：NOT_STARTED、IN_PROGRESS、PAUSED、COMPLETED。省略时默认 NOT_STARTED。")
    @JsonProperty(required = false)
    @Builder.Default
    private String status = TodoStatusEnum.NOT_STARTED.name();

    @Description("可选。取值：LOW、MEDIUM、HIGH。省略时默认 MEDIUM。")
    @JsonProperty(required = false)
    @Builder.Default
    private String priority = TodoPriorityEnum.MEDIUM.name();

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
