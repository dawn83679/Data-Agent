package edu.zsc.ai.util;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;

public class ChatResponseHandler {

    public static int getPromptToken(ChatResponse response) {
        ChatResponseMetadata metadata = response.getMetadata();
        Usage usage = metadata.getUsage();
        return usage.getPromptTokens();
    }

    public static int getCompletionToken(ChatResponse response) {
        ChatResponseMetadata metadata = response.getMetadata();
        Usage usage = metadata.getUsage();
        return usage.getCompletionTokens();
    }

    public static int getTotalToken(ChatResponse response) {
        ChatResponseMetadata metadata = response.getMetadata();
        Usage usage = metadata.getUsage();
        return usage.getTotalTokens();
    }

    public static String getResponseText(ChatResponse response) {
       Generation generation =  response.getResult();
       AssistantMessage assistantMessage = generation.getOutput();
       return  assistantMessage.getText();
    }
}
