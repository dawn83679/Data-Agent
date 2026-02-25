package edu.zsc.ai.controller.db;

import cn.dev33.satoken.stp.StpUtil;
import edu.zsc.ai.domain.model.dto.request.db.AgentExecuteSqlRequest;
import edu.zsc.ai.domain.model.dto.request.db.ExecuteSqlRequest;
import edu.zsc.ai.domain.model.dto.response.base.ApiResponse;
import edu.zsc.ai.domain.model.dto.response.db.ExecuteSqlResponse;
import edu.zsc.ai.domain.service.db.SqlExecutionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Validated
@RestController
@RequestMapping("/api/db/sql")
@RequiredArgsConstructor
public class SqlExecutionController {

    private final SqlExecutionService sqlExecutionService;

    @PostMapping("/execute")
    public ApiResponse<ExecuteSqlResponse> executeSql(@Valid @RequestBody ExecuteSqlRequest request) {
        log.info("Executing SQL: connectionId={}, databaseName={}, schemaName={}",
                request.getConnectionId(), request.getDatabaseName(), request.getSchemaName());

        long userId = StpUtil.getLoginIdAsLong();

        AgentExecuteSqlRequest agentRequest = AgentExecuteSqlRequest.builder()
                .conversationId(request.getConversationId())
                .connectionId(request.getConnectionId())
                .databaseName(request.getDatabaseName())
                .schemaName(request.getSchemaName())
                .sql(request.getSql())
                .userId(userId)
                .build();

        ExecuteSqlResponse response = sqlExecutionService.executeSql(agentRequest);
        return ApiResponse.success(response);
    }
}
