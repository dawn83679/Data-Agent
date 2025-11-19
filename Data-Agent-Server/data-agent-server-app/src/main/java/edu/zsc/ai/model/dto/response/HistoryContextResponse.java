package edu.zsc.ai.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * History context response with messages and total token count
 *
 * @author zgq
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HistoryContextResponse {

    /**
     * List of history messages with token information
     */
    private List<HistoryMessage> messages;

    /**
     * Total token count for the entire context
     */
    private Integer totalTokenCount;
}