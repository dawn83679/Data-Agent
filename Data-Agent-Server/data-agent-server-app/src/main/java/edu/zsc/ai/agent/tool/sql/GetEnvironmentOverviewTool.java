package edu.zsc.ai.agent.tool.sql;

import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.invocation.InvocationParameters;
import edu.zsc.ai.agent.annotation.AgentTool;
import edu.zsc.ai.agent.tool.model.AgentToolResult;
import edu.zsc.ai.common.constant.InvocationContextConstant;
import edu.zsc.ai.agent.tool.sql.model.ConnectionOverview;
import edu.zsc.ai.domain.service.db.DiscoveryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Returns the full connection/catalog/schema landscape.
 * MainAgent uses this to list connections before delegating to Explorer.
 */
@AgentTool
@Slf4j
@Component
@RequiredArgsConstructor
public class GetEnvironmentOverviewTool {

    private final DiscoveryService discoveryService;

    @Tool({
            "Value: maps the available connections, catalogs, and schemas in one call so you can choose a valid discovery scope.",
            "Use When: useful when you want a broad picture of the available connections and catalogs, especially before narrowing scope or preparing a clarification question.",
            "After Success: the returned overview can support follow-up questions, cross-connection comparison, or focused discovery in a narrower scope.",
            "After Partial Success: some connections may be usable while others still need attention; interpret the overview accordingly.",
            "After Failure: the environment information is not currently available, so the next step may be clarification, retry, or stopping with an explicit limitation.",
            "Relation: often helpful before clarification or discovery, but not automatically required for every database request.",
            "PostgreSQL includes non-system schemas. MySQL returns empty schema arrays. If elapsedMs is high, narrow scope in later discovery."
    })
    public AgentToolResult getEnvironmentOverview(InvocationParameters parameters) {
        log.info("[Tool] getEnvironmentOverview");
        List<ConnectionOverview> overview = discoveryService.getEnvironmentOverview();
        boolean chinese = isChinese(parameters);
        if (CollectionUtils.isEmpty(overview)) {
            log.info("[Tool done] getEnvironmentOverview -> empty");
            return AgentToolResult.empty(chinese
                    ? "环境概览未返回可用连接。请先检查连接配置或稍后重试；在连接恢复可用之前，不要继续依赖该连接的检索、规划或执行。"
                    : "Environment overview returned no available connections. Check the connection configuration or retry later. Do not continue discovery, planning, or execution until a usable connection is available.");
        }
        log.info("[Tool done] getEnvironmentOverview, connections={}", overview.size());
        return AgentToolResult.success(overview, buildOverviewMessage(overview, chinese));
    }

    private String buildOverviewMessage(List<ConnectionOverview> overview, boolean chinese) {
        List<ConnectionOverview> unavailableConnections = overview.stream()
                .filter(connection -> StringUtils.isNotBlank(connection.error()))
                .toList();
        if (CollectionUtils.isEmpty(unavailableConnections)) {
            if (chinese) {
                return "当前环境中有 " + overview.size()
                        + " 个可用连接。只有当当前指令、mention、connectionId、catalog 或 schema 仍不足以唯一定位目标时，才比较这些连接。若现有约束已足够，请继续在当前范围内检索或执行。";
            }
            return "Environment overview is available for " + overview.size()
                    + " connection(s). Compare these connections only when the current instruction, mention, connectionId, catalog, or schema still does not identify a unique target. If the current grounding is already sufficient, continue within that scope instead of asking the user to reconfirm it.";
        }

        String unavailableSummary = unavailableConnections.stream()
                .map(connection -> String.format("%s(id=%s): %s",
                        connection.name(),
                        connection.id(),
                        connection.error()))
                .collect(Collectors.joining("; "));

        long availableCount = overview.size() - unavailableConnections.size();
        if (availableCount > 0) {
            if (chinese) {
                return "当前环境仅部分可用。失败的连接有：" + unavailableSummary
                        + "。请继续使用其余可用连接；只有当当前任务依赖失败连接且无法替代时，再询问用户是否切换到可用连接或稍后重试。";
            }
            return "Environment overview is only partially available. Failed connections: " + unavailableSummary
                    + ". Continue with the remaining available connections. Ask the user whether to switch to an available connection or retry later only when the task still depends on an unavailable connection.";
        }

        if (chinese) {
            return "当前环境未找到可用连接。失败的连接有：" + unavailableSummary
                    + "。请询问用户是稍后重试还是检查连接配置；在恢复可用连接之前，不要继续依赖连接的对象检索。";
        }
        return "Environment overview could not find any usable connection. Failed connections: " + unavailableSummary
                + ". Ask the user whether to retry later or check the connection configuration. Do not continue object discovery until a usable connection is available.";
    }

    private boolean isChinese(InvocationParameters parameters) {
        if (parameters == null) {
            return false;
        }
        String language = parameters.getOrDefault(InvocationContextConstant.LANGUAGE, "en");
        return StringUtils.startsWithIgnoreCase(language, "zh");
    }
}
