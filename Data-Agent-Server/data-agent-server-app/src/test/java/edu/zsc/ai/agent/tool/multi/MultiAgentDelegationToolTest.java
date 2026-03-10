package edu.zsc.ai.agent.tool.multi;

import dev.langchain4j.invocation.InvocationParameters;
import edu.zsc.ai.domain.service.agent.multi.MultiAgentDelegationService;
import edu.zsc.ai.domain.service.agent.multi.model.SubAgentDelegationResult;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MultiAgentDelegationToolTest {

    @Test
    void shouldReturnFailureToolResultWhenSubAgentFails() {
        MultiAgentDelegationService delegationService = mock(MultiAgentDelegationService.class);
        MultiAgentDelegationTool tool = new MultiAgentDelegationTool(delegationService);
        SubAgentDelegationResult failedResult = SubAgentDelegationResult.builder()
                .status("failed")
                .summary("Sub-agent failed: No StreamingChatModel configured for model=qwen3-max")
                .build();

        when(delegationService.delegate(any(), eq("title"), eq("instructions"), any()))
                .thenReturn(failedResult);

        var result = tool.delegateToSchemaAnalyst(
                "title",
                "instructions",
                InvocationParameters.from(Map.of()));

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("No StreamingChatModel configured"));
    }

    @Test
    void shouldReturnSuccessToolResultWhenSubAgentCompletes() {
        MultiAgentDelegationService delegationService = mock(MultiAgentDelegationService.class);
        MultiAgentDelegationTool tool = new MultiAgentDelegationTool(delegationService);
        SubAgentDelegationResult completedResult = SubAgentDelegationResult.builder()
                .status("completed")
                .summary("done")
                .build();

        when(delegationService.delegate(any(), eq("title"), eq("instructions"), any()))
                .thenReturn(completedResult);

        var result = tool.delegateToSchemaAnalyst(
                "title",
                "instructions",
                InvocationParameters.from(Map.of()));

        assertTrue(result.isSuccess());
        assertSame(completedResult, result.getResult());
    }
}
