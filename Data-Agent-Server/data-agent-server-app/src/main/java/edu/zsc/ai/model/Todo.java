package edu.zsc.ai.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.langchain4j.model.output.structured.Description;
import edu.zsc.ai.common.enums.ai.TodoPriorityEnum;
import edu.zsc.ai.common.enums.ai.TodoStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Single todo item. Used as domain model and as tool parameter (LLM passes title and optional fields).
 * Server fills createdAt/updatedAt when persisting.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Description("A single todo task. Used as an element in the list when updating the todo list.")
public class Todo {

    @Description("Title of the task. Required.")
    private String title;

    @Description("Optional detailed description of the task.")
    @JsonProperty(required = false)
    private String description;

    @Description("Optional. One of: NOT_STARTED, IN_PROGRESS, PAUSED, COMPLETED. Defaults to NOT_STARTED if omitted.")
    @JsonProperty(required = false)
    @Builder.Default
    private String status = TodoStatusEnum.NOT_STARTED.name();

    @Description("Optional. One of: LOW, MEDIUM, HIGH. Defaults to MEDIUM if omitted.")
    @JsonProperty(required = false)
    @Builder.Default
    private String priority = TodoPriorityEnum.MEDIUM.name();

    /** Set by server when persisting; not required from LLM. */
    private LocalDateTime createdAt;

    /** Set by server when persisting; not required from LLM. */
    private LocalDateTime updatedAt;
}
