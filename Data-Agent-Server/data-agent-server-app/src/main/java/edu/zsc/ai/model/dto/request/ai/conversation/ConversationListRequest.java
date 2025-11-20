package edu.zsc.ai.model.dto.request.ai.conversation;

import edu.zsc.ai.model.dto.request.base.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Request DTO for conversation list query with pagination
 *
 * @author zgq
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class ConversationListRequest extends PageRequest {

    /**
     * Filter by conversation title (optional)
     */
    private String title;
}