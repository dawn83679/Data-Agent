package edu.zsc.ai.domain.service.ai;

import dev.langchain4j.data.message.ChatMessage;
import edu.zsc.ai.domain.service.ai.model.CompressionResult;

import java.util.List;

public interface CompressionService {

    /**
     * Compresses a list of chat messages into a structured summary string.
     */
    CompressionResult compress(List<ChatMessage> messages);
}
