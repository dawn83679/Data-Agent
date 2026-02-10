package edu.zsc.ai.model.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Request for submitting user answer to an askUserQuestion tool and continuing the conversation.
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class SubmitToolAnswerRequest extends BaseRequest {

    /** LangChain4j ToolExecutionRequest.id; used to locate the askUserQuestion tool result. */
    @NotBlank(message = "toolCallId cannot be empty")
    private String toolCallId;

    /** The user's answer. */
    @NotBlank(message = "answer cannot be empty")
    private String answer;

    /** Model name (optional); defaults to server default when blank. */
    private String model;
}
