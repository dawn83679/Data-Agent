package edu.zsc.ai.agent.subagent;

import edu.zsc.ai.util.ConnectionIdUtil;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

public class SubAgentPromptBuilder {

    private String instruction;
    private Long connectionId;
    private List<Long> allowedConnectionIds;
    private String context;

    private SubAgentPromptBuilder() {
    }

    public static SubAgentPromptBuilder builder() {
        return new SubAgentPromptBuilder();
    }

    public SubAgentPromptBuilder instruction(String instruction) {
        this.instruction = instruction;
        return this;
    }

    public SubAgentPromptBuilder connectionId(Long connectionId) {
        this.connectionId = connectionId;
        return this;
    }

    public SubAgentPromptBuilder allowedConnectionIds(List<Long> allowedConnectionIds) {
        this.allowedConnectionIds = allowedConnectionIds;
        return this;
    }

    public SubAgentPromptBuilder context(String context) {
        this.context = context;
        return this;
    }

    public String build() {
        StringBuilder sb = new StringBuilder();
        sb.append("指令：").append(StringUtils.defaultString(instruction)).append("\n");
        sb.append("连接：").append(connectionId == null ? StringUtils.EMPTY : connectionId).append("\n");
        if (CollectionUtils.isNotEmpty(allowedConnectionIds)) {
            sb.append("允许连接：").append(ConnectionIdUtil.toCsv(allowedConnectionIds)).append("\n");
        }

        if (StringUtils.isNotBlank(context)) {
            sb.append("\n上下文：\n").append(context).append("\n");
        }

        return sb.toString();
    }
}
