package edu.zsc.ai.domain.service.ai;

import dev.langchain4j.data.message.ChatMessage;

import java.util.List;

public interface CompressionService {

    /**
     * Compresses a list of chat messages into a structured summary string.
     */
    String compress(List<ChatMessage> messages);
}
