package edu.zsc.ai.context;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import edu.zsc.ai.common.constant.InvocationContextConstant;

class RequestContextTest {

    @AfterEach
    void tearDown() {
        RequestContext.clear();
    }

    @Test
    void toMap_includesConnectionCatalogAndSchemaForInvocationContext() {
        RequestContext.set(RequestContextInfo.builder()
                .userId(7L)
                .conversationId(42L)
                .connectionId(5L)
                .catalog("sales")
                .schema("public")
                .build());

        Map<String, Object> map = RequestContext.toMap();

        assertEquals(7L, map.get(InvocationContextConstant.USER_ID));
        assertEquals(42L, map.get(InvocationContextConstant.CONVERSATION_ID));
        assertEquals(5L, map.get(InvocationContextConstant.CONNECTION_ID));
        assertEquals("sales", map.get(InvocationContextConstant.DATABASE_NAME));
        assertEquals("public", map.get(InvocationContextConstant.SCHEMA_NAME));
    }

    @Test
    void getters_stillExposeScopeForNonAgentCallers() {
        RequestContext.set(RequestContextInfo.builder()
                .connectionId(5L)
                .catalog("sales")
                .schema("public")
                .build());

        assertEquals(5L, RequestContext.getConnectionId());
        assertEquals("sales", RequestContext.getCatalog());
        assertEquals("public", RequestContext.getSchema());
        assertTrue(RequestContext.hasContext());
    }
}
