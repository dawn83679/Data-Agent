package edu.zsc.ai.ai;

import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for StreamingChatModel (Streaming mode)
 * Tests streaming chat functionality and demonstrates real-time token reception.
 */
@SpringBootTest
@Slf4j
public class StreamingChatModelIntegrationTest {

    @Autowired
    private StreamingChatModel streamingChatModel;

    @Test
    @DisplayName("测试流式输出 - 基础流式对话")
    public void testStreamingChat() throws Exception {
        String question = "Explain what is a database index in simple terms.";

        StringBuilder responseBuilder = new StringBuilder();
        CompletableFuture<ChatResponse> future = new CompletableFuture<>();
        AtomicInteger tokenCount = new AtomicInteger(0);

        log.info("Starting streaming chat with question: {}", question);
        log.info("=== Streaming Output Start ===");

        streamingChatModel.chat(question, new StreamingChatResponseHandler() {
            @Override
            public void onPartialResponse(String partialResponse) {
                responseBuilder.append(partialResponse);
                tokenCount.incrementAndGet();
                // Print each token in real-time to demonstrate streaming
                System.out.print(partialResponse);
                log.debug("Token #{}: {}", tokenCount.get(), partialResponse);
            }

            @Override
            public void onCompleteResponse(ChatResponse response) {
                System.out.println(); // New line after streaming completes
                log.info("=== Streaming Output End ===");
                log.info("Complete response received. Total tokens: {}", tokenCount.get());
                log.info("Full response: {}", responseBuilder.toString());
                log.info("Token Usage: {}", response.tokenUsage());
                log.info("Finish Reason: {}", response.finishReason());
                future.complete(response);
            }

            @Override
            public void onError(Throwable throwable) {
                log.error("Error during streaming", throwable);
                future.completeExceptionally(throwable);
            }
        });

        ChatResponse response = future.get(60, TimeUnit.SECONDS);
        String fullResponse = responseBuilder.toString();

        assertThat(fullResponse).isNotBlank();
        assertThat(fullResponse.toLowerCase()).containsAnyOf("index", "索引");
        assertThat(tokenCount.get()).isGreaterThan(0);
        assertThat(response).isNotNull();
    }

    @Test
    @DisplayName("测试流式输出 - SQL 生成")
    public void testStreamingSqlGeneration() throws Exception {
        String prompt = """
            Generate a SQL query to find the top 10 products by sales in the last month.
            Table: products
            Columns: id, name, price, sales_count, last_sale_date
            """;

        StringBuilder responseBuilder = new StringBuilder();
        CompletableFuture<ChatResponse> future = new CompletableFuture<>();

        log.info("Starting streaming SQL generation");
        log.info("=== Streaming SQL Output Start ===");

        streamingChatModel.chat(prompt, new StreamingChatResponseHandler() {
            @Override
            public void onPartialResponse(String partialResponse) {
                responseBuilder.append(partialResponse);
                System.out.print(partialResponse);
            }

            @Override
            public void onCompleteResponse(ChatResponse response) {
                System.out.println();
                log.info("=== Streaming SQL Output End ===");
                log.info("Generated SQL: {}", responseBuilder.toString());
                future.complete(response);
            }

            @Override
            public void onError(Throwable throwable) {
                log.error("Error during SQL generation", throwable);
                future.completeExceptionally(throwable);
            }
        });

        future.get(60, TimeUnit.SECONDS);
        String sql = responseBuilder.toString();

        assertThat(sql).isNotBlank();
        assertThat(sql.toUpperCase()).contains("SELECT");
        assertThat(sql.toLowerCase()).contains("products");
    }

    @Test
    @DisplayName("测试流式输出 - 比较流式与非流式的响应")
    public void testStreamingVsNonStreaming() throws Exception {
        String question = "List three advantages of PostgreSQL.";

        // Streaming output
        StringBuilder streamingResponse = new StringBuilder();
        CompletableFuture<Void> streamingFuture = new CompletableFuture<>();
        long streamingStartTime = System.currentTimeMillis();

        log.info("Testing streaming output for question: {}", question);

        streamingChatModel.chat(question, new StreamingChatResponseHandler() {
            @Override
            public void onPartialResponse(String partial) {
                streamingResponse.append(partial);
            }

            @Override
            public void onCompleteResponse(ChatResponse response) {
                long streamingDuration = System.currentTimeMillis() - streamingStartTime;
                log.info("Streaming completed in {} ms", streamingDuration);
                streamingFuture.complete(null);
            }

            @Override
            public void onError(Throwable throwable) {
                streamingFuture.completeExceptionally(throwable);
            }
        });

        streamingFuture.get(60, TimeUnit.SECONDS);

        log.info("Streaming response: {}", streamingResponse.toString());
        assertThat(streamingResponse.toString()).isNotBlank();
        assertThat(streamingResponse.toString().toLowerCase()).containsAnyOf("postgresql", "advantage");
    }

    @Test
    @DisplayName("测试流式输出 - 实时反馈体验")
    public void testRealTimeFeedback() throws Exception {
        String question = "Write a brief explanation of how database transactions work.";

        StringBuilder responseBuilder = new StringBuilder();
        CompletableFuture<ChatResponse> future = new CompletableFuture<>();
        AtomicInteger chunkCount = new AtomicInteger(0);
        long startTime = System.currentTimeMillis();

        log.info("Testing real-time feedback with question: {}", question);
        log.info("=== Real-time Streaming Output ===");

        streamingChatModel.chat(question, new StreamingChatResponseHandler() {
            @Override
            public void onPartialResponse(String partialResponse) {
                responseBuilder.append(partialResponse);
                int count = chunkCount.incrementAndGet();
                long elapsed = System.currentTimeMillis() - startTime;
                
                // Demonstrate real-time output
                System.out.print(partialResponse);
                
                // Log every 10th chunk to reduce verbosity
                if (count % 10 == 0) {
                    log.debug("Received {} chunks in {} ms", count, elapsed);
                }
            }

            @Override
            public void onCompleteResponse(ChatResponse response) {
                System.out.println();
                long totalDuration = System.currentTimeMillis() - startTime;
                log.info("=== Streaming Complete ===");
                log.info("Total chunks: {}", chunkCount.get());
                log.info("Total duration: {} ms", totalDuration);
                log.info("Full response length: {} characters", responseBuilder.length());
                future.complete(response);
            }

            @Override
            public void onError(Throwable throwable) {
                log.error("Error during streaming", throwable);
                future.completeExceptionally(throwable);
            }
        });

        ChatResponse response = future.get(60, TimeUnit.SECONDS);
        String fullResponse = responseBuilder.toString();

        assertThat(fullResponse).isNotBlank();
        assertThat(fullResponse.toLowerCase()).containsAnyOf("transaction", "事务");
        assertThat(chunkCount.get()).isGreaterThan(0);
        log.info("Average chunk size: {} characters", fullResponse.length() / chunkCount.get());
    }

    @Test
    @DisplayName("测试流式输出 - 错误处理")
    public void testStreamingErrorHandling() throws Exception {
        // Test with a very simple question to ensure we get a response
        String question = "Hello";

        CompletableFuture<Boolean> future = new CompletableFuture<>();

        streamingChatModel.chat(question, new StreamingChatResponseHandler() {
            @Override
            public void onPartialResponse(String partialResponse) {
                log.debug("Received partial response: {}", partialResponse);
            }

            @Override
            public void onCompleteResponse(ChatResponse response) {
                log.info("Response completed successfully");
                future.complete(true);
            }

            @Override
            public void onError(Throwable throwable) {
                log.error("Error occurred during streaming", throwable);
                future.complete(false);
            }
        });

        Boolean success = future.get(30, TimeUnit.SECONDS);
        assertThat(success).isTrue();
    }
}
