package edu.zsc.ai.config.ai;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class ModelRegistry {

    private final List<ChatModelProvider> providers;

    @Bean
    public Map<String, StreamingChatModel> modelsByName() {
        Map<String, StreamingChatModel> merged = new LinkedHashMap<>();
        for (ChatModelProvider provider : providers) {
            merged.putAll(provider.streamingChatModels());
        }
        return Collections.unmodifiableMap(merged);
    }

    @Bean
    public Map<String, ChatModel> chatModelsByName() {
        Map<String, ChatModel> merged = new LinkedHashMap<>();
        for (ChatModelProvider provider : providers) {
            merged.putAll(provider.chatModels());
        }
        return Collections.unmodifiableMap(merged);
    }
}
