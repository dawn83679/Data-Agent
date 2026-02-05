package edu.zsc.ai.config.ai;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * LangChain4j Configuration
 * Ensures ChatModel and StreamingChatModel beans are properly configured.
 */
@Configuration
@Slf4j
public class LangChain4jConfig {

    /**
     * Chat Model Bean
     * This bean is auto-configured by langchain4j-community-dashscope-spring-boot-starter.
     * We provide this bean definition to ensure it's available and add logging.
     */
    @Bean
    @ConditionalOnMissingBean
    public ChatModel chatModel(
            @Autowired(required = false) ChatModel autowiredChatModel) {
        if (autowiredChatModel != null) {
            log.info("Using auto-configured ChatModel: {}",
                    autowiredChatModel.getClass().getSimpleName());
            return autowiredChatModel;
        }
        throw new IllegalStateException(
                "No ChatModel configured. Check langchain4j.community.dashscope.chat-model configuration in application.yml");
    }

    /**
     * Streaming Chat Model Bean
     * This bean is auto-configured by langchain4j-community-dashscope-spring-boot-starter.
     * We provide this bean definition to ensure it's available and add logging.
     */
    @Bean
    @ConditionalOnMissingBean
    public StreamingChatModel streamingChatModel(
            @Autowired(required = false) StreamingChatModel autowiredStreamingChatModel) {
        if (autowiredStreamingChatModel != null) {
            log.info("Using auto-configured StreamingChatModel: {}",
                    autowiredStreamingChatModel.getClass().getSimpleName());
            return autowiredStreamingChatModel;
        }
        throw new IllegalStateException(
                "No StreamingChatModel configured. Check langchain4j.community.dashscope.streaming-chat-model configuration in application.yml");
    }
}
