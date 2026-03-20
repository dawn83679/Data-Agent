package edu.zsc.ai.agent.tool.ask;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import dev.langchain4j.invocation.InvocationParameters;
import edu.zsc.ai.agent.tool.ask.model.UserQuestion;

class AskUserQuestionToolTest {

    private final AskUserQuestionTool tool = new AskUserQuestionTool();

    @Test
    void returnsEnglishSummaryByDefault() {
        String result = tool.askUserQuestion(List.of(
                new UserQuestion("Choose a connection", List.of("test1", "test2"), null, false)
        ), InvocationParameters.from(Map.of()));

        assertTrue(result.contains("1 question(s) were sent to the user."));
        assertTrue(result.contains("Wait for the user's reply"));
    }

    @Test
    void returnsChineseSummaryWhenInvocationLanguageIsChinese() {
        String result = tool.askUserQuestion(List.of(
                new UserQuestion("请选择连接", List.of("test1", "test2"), null, false)
        ), InvocationParameters.from(Map.of("language", "zh")));

        assertTrue(result.contains("已向用户发送 1 个问题"));
        assertTrue(result.contains("等待用户回复后"));
    }
}
