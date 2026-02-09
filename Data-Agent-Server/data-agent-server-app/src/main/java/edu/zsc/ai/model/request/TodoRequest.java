package edu.zsc.ai.model.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.langchain4j.model.output.structured.Description;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request model for a single todo task, used as an element in the list passed to the updateTodoList tool.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Description({
    "A single todo task.",
    "Used as an element in the list when updating the todo list."
})
public class TodoRequest {

    @Description("Title of the task. Required.")
    private String title;

    @Description("Optional detailed description of the task.")
    @JsonProperty(required = false)
    private String description;

    @Description("Optional priority. One of: LOW, MEDIUM, HIGH. Defaults to MEDIUM if omitted.")
    @JsonProperty(required = false)
    private String priority;

    @Description({
        "Optional. Not needed when creating new tasks; use when updating existing tasks to set progress.",
        "One of: NOT_STARTED, IN_PROGRESS, PAUSED, COMPLETED. Defaults to NOT_STARTED if omitted."
    })
    @JsonProperty(required = false)
    private String status;
}
