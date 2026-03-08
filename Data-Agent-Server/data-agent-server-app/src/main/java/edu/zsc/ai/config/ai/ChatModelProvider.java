package edu.zsc.ai.config.ai;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;

import java.util.Map;

public interface ChatModelProvider {

    Map<String, StreamingChatModel> streamingChatModels();

    default Map<String, ChatModel> chatModels() {
        return Map.of();
    }
}
