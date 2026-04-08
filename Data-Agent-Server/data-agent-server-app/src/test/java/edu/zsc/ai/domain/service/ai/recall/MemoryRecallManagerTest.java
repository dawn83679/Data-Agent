package edu.zsc.ai.domain.service.ai.recall;

<<<<<<< HEAD
=======
import static org.junit.jupiter.api.Assertions.assertEquals;
>>>>>>> 55de6b9b235ffd91a8c266a1c07a27b7fb059793
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

<<<<<<< HEAD
=======
import java.util.ArrayList;
>>>>>>> 55de6b9b235ffd91a8c266a1c07a27b7fb059793
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

<<<<<<< HEAD
=======
import edu.zsc.ai.common.constant.MemoryRecallLogConstant;
import edu.zsc.ai.observability.AgentLogEvent;
import edu.zsc.ai.observability.AgentLogService;

>>>>>>> 55de6b9b235ffd91a8c266a1c07a27b7fb059793
@ExtendWith(MockitoExtension.class)
class MemoryRecallManagerTest {

    @Mock
    private MemoryRecallQueryPlanner queryPlanner;

    @Mock
    private MemoryRecallHandlerChain handlerChain;

    @Mock
    private MemoryRecallPostProcessor postProcessor;

    @Test
    void recall_plansQueriesDispatchesEachQueryAndPostProcessesMergedItems() {
<<<<<<< HEAD
        MemoryRecallManager manager = new MemoryRecallManager(queryPlanner, handlerChain, postProcessor);
=======
        CaptureAgentLogService agentLogService = new CaptureAgentLogService();
        MemoryRecallManager manager = new MemoryRecallManager(queryPlanner, handlerChain, postProcessor, agentLogService);
>>>>>>> 55de6b9b235ffd91a8c266a1c07a27b7fb059793
        MemoryRecallContext context = MemoryRecallContext.builder()
                .conversationId(7L)
                .queryText("find preference")
                .recallMode(MemoryRecallMode.PROMPT)
                .build();
        MemoryRecallQuery userQuery = new MemoryRecallQuery("user", "planned_user", "USER", 7L, "find preference", null, null, null,
                MemoryRecallMode.PROMPT, MemoryRecallQueryStrategy.HYBRID, 0);
        MemoryRecallQuery conversationQuery = new MemoryRecallQuery("conversation", "planned_conversation", "CONVERSATION", 7L, "find preference", null, null, null,
                MemoryRecallMode.PROMPT, MemoryRecallQueryStrategy.BROWSE, 1);
        MemoryRecallItem userItem = MemoryRecallItem.builder().id(1L).scope("USER").content("User memory").build();
        MemoryRecallItem conversationItem = MemoryRecallItem.builder().id(2L).scope("CONVERSATION").content("Conversation memory").build();
        MemoryRecallResult expected = MemoryRecallResult.builder().items(List.of(userItem, conversationItem)).build();

        when(queryPlanner.plan(eq(context))).thenReturn(List.of(userQuery, conversationQuery));
        when(handlerChain.handle(userQuery)).thenReturn(List.of(userItem));
        when(handlerChain.handle(conversationQuery)).thenReturn(List.of(conversationItem));
        when(postProcessor.process(eq(context), anyList())).thenReturn(expected);

        MemoryRecallResult result = manager.recall(context);

        assertSame(expected, result);
        verify(queryPlanner).plan(context);
        verify(handlerChain).handle(userQuery);
        verify(handlerChain).handle(conversationQuery);
        verify(postProcessor).process(eq(context), eq(List.of(userItem, conversationItem)));
<<<<<<< HEAD
=======
        assertEquals(List.of(
                        MemoryRecallLogConstant.EVENT_RECALL_START,
                        MemoryRecallLogConstant.EVENT_RECALL_PLANNED,
                        MemoryRecallLogConstant.EVENT_RECALL_QUERY_DISPATCH,
                        MemoryRecallLogConstant.EVENT_RECALL_QUERY_DISPATCH,
                        MemoryRecallLogConstant.EVENT_RECALL_POST_PROCESS,
                        MemoryRecallLogConstant.EVENT_RECALL_COMPLETE),
                agentLogService.eventNames());
    }

    private static final class CaptureAgentLogService implements AgentLogService {

        private final List<AgentLogEvent> events = new ArrayList<>();

        @Override
        public void record(AgentLogEvent event) {
            events.add(event);
        }

        private List<String> eventNames() {
            return events.stream()
                    .map(event -> event.getPayload().get("eventName"))
                    .map(String::valueOf)
                    .toList();
        }
>>>>>>> 55de6b9b235ffd91a8c266a1c07a27b7fb059793
    }
}
