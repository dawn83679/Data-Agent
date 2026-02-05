package edu.zsc.ai.ai;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for ChatModel (Non-streaming mode)
 * Tests basic chat functionality and SQL generation capabilities.
 */
@SpringBootTest
@Slf4j
public class ChatModelIntegrationTest {

    @Autowired
    private ChatModel chatModel;

    @Test
    @DisplayName("测试非流式输出 - 简单问答")
    public void testSimpleChat() {
        String question = "Hello";
        log.info("Sending question: {}", question);

        String response = chatModel.chat(question);

        log.info("Response: {}", response);
        assertThat(response).isNotBlank();

    }

    @Test
    @DisplayName("测试非流式输出 - SQL 生成")
    public void testSqlGeneration() {
        String prompt = """
            Generate a SQL query to find all users registered in the last 30 days.
            Table: users
            Columns: id, username, email, created_at
            """;

        log.info("Sending SQL generation prompt");

        ChatResponse response = chatModel.chat(ChatRequest.builder()
                .messages(UserMessage.from(prompt))
                .build());

        String sql = response.aiMessage().text();
        log.info("Generated SQL: {}", sql);

        assertThat(sql).isNotBlank();
        assertThat(sql.toUpperCase()).contains("SELECT");
        assertThat(sql.toLowerCase()).contains("users");
        assertThat(sql.toLowerCase()).contains("created_at");
    }

    @Test
    @DisplayName("测试非流式输出 - 数据库索引解释")
    public void testDatabaseConceptExplanation() {
        String question = "Explain what is a database index in one sentence.";
        log.info("Sending question: {}", question);

        String response = chatModel.chat(question);

        log.info("Response: {}", response);
        assertThat(response).isNotBlank();
        assertThat(response.toLowerCase()).containsAnyOf("index", "索引");
    }

    @Test
    @DisplayName("测试非流式输出 - 验证响应结构")
    public void testResponseStructure() {
        String question = "What is PostgreSQL?";
        log.info("Sending question: {}", question);

        ChatResponse response = chatModel.chat(ChatRequest.builder()
                .messages(UserMessage.from(question))
                .build());

        log.info("Response received");
        log.info("AI Message: {}", response.aiMessage().text());
        log.info("Token Usage: {}", response.tokenUsage());
        log.info("Finish Reason: {}", response.finishReason());

        assertThat(response).isNotNull();
        assertThat(response.aiMessage()).isNotNull();
        assertThat(response.aiMessage().text()).isNotBlank();
    }
}
