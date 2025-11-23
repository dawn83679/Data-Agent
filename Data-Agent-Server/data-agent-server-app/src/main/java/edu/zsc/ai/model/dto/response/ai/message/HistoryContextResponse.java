package edu.zsc.ai.model.dto.response.ai.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.ai.chat.messages.Message;

import java.util.List;

/**
 * Optimized history context response
 *
 * @author zgq
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HistoryContextResponse {

    private List<Message> messages;

    private int totalTokenCount;

}