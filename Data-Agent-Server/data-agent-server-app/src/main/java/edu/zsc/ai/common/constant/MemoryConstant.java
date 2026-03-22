package edu.zsc.ai.common.constant;

import edu.zsc.ai.common.enums.ai.MemoryScopeEnum;
import edu.zsc.ai.common.enums.ai.MemoryWorkspaceLevelEnum;

public final class MemoryConstant {

    public static final String DEFAULT_SCOPE = MemoryScopeEnum.USER.getCode();
    public static final String DEFAULT_WORKSPACE_CONTEXT_KEY = MemoryWorkspaceLevelEnum.GLOBAL.getCode();
    public static final String EMPTY_DETAIL_JSON = "{}";
    public static final String EMPTY_SOURCE_MESSAGE_IDS_JSON = "[]";
    public static final String UNTITLED_MEMORY = "Untitled memory";

    private MemoryConstant() {
    }
}
