package edu.zsc.ai.agent.tool.multi;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.invocation.InvocationParameters;
import edu.zsc.ai.agent.annotation.AgentTool;
import edu.zsc.ai.agent.tool.ToolContext;
import edu.zsc.ai.agent.tool.model.AgentToolResult;
import edu.zsc.ai.common.enums.ai.AgentRoleEnum;
import edu.zsc.ai.domain.service.agent.multi.model.SubAgentDelegationResult;
import edu.zsc.ai.domain.service.agent.multi.MultiAgentDelegationService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@AgentTool
@RequiredArgsConstructor
public class MultiAgentDelegationTool {

    private static final Set<String> VALID_ROLES = Arrays.stream(AgentRoleEnum.values())
            .filter(r -> r != AgentRoleEnum.ORCHESTRATOR)
            .map(AgentRoleEnum::getCode)
            .collect(Collectors.toSet());

    private final MultiAgentDelegationService delegationService;

    @Tool("Delegate a task to a specialized sub-agent. Available roles: schema_explorer (discovers database structure), data_analyst (designs and executes read-only SQL), data_writer (executes write operations with safety checks).")
    public AgentToolResult delegate(
            @P("Target role: schema_explorer | data_analyst | data_writer") String role,
            @P("Short title for this delegated task") String title,
            @P("Concrete instructions for the sub-agent. Include all necessary context (schema reports, connection details) directly in the instructions.") String instructions,
            InvocationParameters parameters) {
        try (var ctx = ToolContext.from(parameters)) {
            AgentRoleEnum roleEnum = resolveRole(role);
            if (roleEnum == null) {
                return AgentToolResult.fail("Invalid role '" + role + "'. Valid roles: " + VALID_ROLES);
            }
            return ctx.timed(toToolResult(
                    delegationService.delegate(roleEnum, title, instructions, parameters)));
        }
    }

    private AgentRoleEnum resolveRole(String role) {
        if (StringUtils.isBlank(role)) return null;
        String normalized = role.trim().toLowerCase();
        for (AgentRoleEnum r : AgentRoleEnum.values()) {
            if (r.getCode().equals(normalized) && r != AgentRoleEnum.ORCHESTRATOR) {
                return r;
            }
        }
        return null;
    }

    private AgentToolResult toToolResult(SubAgentDelegationResult result) {
        if (result == null) {
            return AgentToolResult.fail("Sub-agent delegation returned no result.");
        }
        if ("failed".equalsIgnoreCase(result.getStatus())) {
            return AgentToolResult.fail(StringUtils.defaultIfBlank(
                    result.getSummary(),
                    "Sub-agent task failed."));
        }
        return AgentToolResult.success(result);
    }
}
