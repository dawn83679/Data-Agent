package edu.zsc.ai.config;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Objects;

@SpringBootTest
public class LLMConfigTest {

    @Autowired
    private ChatClient chatClient;

    @Test
    public void test() {
        ChatClient.CallResponseSpec callResponseSpec = chatClient.prompt("How old are you").call();
        String text = Objects.requireNonNull(callResponseSpec.chatResponse()).getResult().getOutput().getText();
        System.out.println("text = " + text);

    }

    @Test
    public void test2() {
        Flux<ChatResponse> chatResponseFlux = chatClient.prompt("How old are you")
                .stream()
                .chatResponse().doOnNext(chatResponse -> {
                    System.out.println("chatResponse.getResult().getOutput().getText() = " + chatResponse.getResult().getOutput().getText());
                }
        ).doOnError(System.err::println);
        Mono<ChatResponse> lastResponse = chatResponseFlux.last();
        ChatResponse blocked = lastResponse.block();
        ChatResponseMetadata metadata = null;
        if (blocked != null) {
            metadata = blocked.getMetadata();
            Usage usage = metadata.getUsage();
            System.out.println("usage.getPromptTokens() = " + usage.getPromptTokens());
            System.out.println("usage.getCompletionTokens() = " + usage.getCompletionTokens());
            System.out.println("usage.getTotalTokens() = " + usage.getTotalTokens());
        }


    }
}
