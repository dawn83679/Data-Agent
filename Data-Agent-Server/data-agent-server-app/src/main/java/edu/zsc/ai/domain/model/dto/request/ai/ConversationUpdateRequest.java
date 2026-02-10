package edu.zsc.ai.domain.model.dto.request.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Conversation update request (e.g. title)
 *
 * @author Data-Agent
 * @since 0.0.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationUpdateRequest {

    /**
     * New conversation title
     */
    @NotBlank(message = "Title must not be blank")
    @Size(max = 255)
    private String title;
}
