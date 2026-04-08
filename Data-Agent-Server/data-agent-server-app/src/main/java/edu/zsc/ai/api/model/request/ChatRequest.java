package edu.zsc.ai.api.model.request;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Chat Request DTO
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ChatRequest extends BaseRequest {

    @NotBlank(message = "Message cannot be empty")
    private String message;

    /**
     * Model name for chat (e.g. qwen3.5-plus, qwen3-max-2026-01-23, qwen3-max-thinking).
<<<<<<< HEAD
     * Optional; server defaults to ai.models.default-model when blank.
=======
     * Optional; server defaults to qwen3.5-plus when blank.
>>>>>>> 55de6b9b235ffd91a8c266a1c07a27b7fb059793
     */
    private String model;

    /**
     * Preferred prompt language, e.g. "en" or "zh".
     * Optional; server defaults to English prompt when blank/unknown.
     */
    private String language;

    /**
     * Agent mode, e.g. "Agent" or "Plan".
     * Optional; server defaults to Agent mode when blank/unknown.
     */
    private String agentType;

    /**
     * Database catalog name used by chat runtime context.
     */
    private String catalogName;

    /**
     * Structured @ mentions selected from ChatInput.
     * These are used to populate the runtime prompt's explicit references block.
     */
    private List<ChatUserMention> userMentions;

    @Override
    public String getCatalog() {
        return catalogName;
    }

    @Override
    public void setCatalog(String catalog) {
        this.catalogName = catalog;
    }
}
