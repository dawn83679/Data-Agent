package edu.zsc.ai.controller.db;

import cn.dev33.satoken.stp.StpUtil;
import edu.zsc.ai.domain.model.dto.request.db.ExecuteSqlApiRequest;
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

/**
 * REST controller for executing ad-hoc SQL statements on a user-owned connection.
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/db")
@RequiredArgsConstructor
public class SqlExecutionController {

    private final SqlExecutionService sqlExecutionService;

    /**
     * Execute a single SQL statement on the given connection / database / schema.
     *
     * <p>User id is resolved from Sa-Token, not from request body.</p>
     *
     * @param body request body containing execution context and SQL
     * @return wrapped {@link ExecuteSqlResponse}
     */
    @PostMapping("/sql/execute")
    public ApiResponse<ExecuteSqlResponse> executeSql(@Valid @RequestBody ExecuteSqlApiRequest body) {
        long userId = StpUtil.getLoginIdAsLong();

        log.info("Executing SQL via REST API: connectionId={}, databaseName={}, schemaName={}, sqlLength={}",
                body.getConnectionId(),
                body.getDatabaseName(),
                body.getSchemaName(),
                body.getSql() != null ? body.getSql().length() : 0);

        ExecuteSqlRequest request = ExecuteSqlRequest.builder()
                .connectionId(body.getConnectionId())
                .databaseName(body.getDatabaseName())
                .schemaName(body.getSchemaName())
                .sql(body.getSql())
                .userId(userId)
                .build();

        ExecuteSqlResponse response = sqlExecutionService.executeSql(request);
        return ApiResponse.success(response);
    }
}
