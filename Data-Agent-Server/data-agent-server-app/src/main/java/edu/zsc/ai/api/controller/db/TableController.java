package edu.zsc.ai.api.controller.db;

import cn.dev33.satoken.stp.StpUtil;
import edu.zsc.ai.domain.model.dto.request.db.DeleteTableRequest;
import edu.zsc.ai.domain.model.dto.response.base.ApiResponse;
import edu.zsc.ai.domain.model.dto.response.db.TableDataResponse;
import edu.zsc.ai.domain.service.db.TableService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@Validated
@RestController
@RequestMapping("/api/tables")
@RequiredArgsConstructor
public class TableController {

    private final TableService tableService;

    @GetMapping
    public ApiResponse<List<String>> listTables(
            @RequestParam @NotNull(message = "connectionId is required") Long connectionId,
            @RequestParam(required = false) String catalog,
            @RequestParam(required = false) String schema) {
        log.info("Listing tables: connectionId={}, catalog={}, schema={}", connectionId, catalog, schema);
        long userId = StpUtil.getLoginIdAsLong();
        List<String> tables = tableService.getTables(connectionId, catalog, schema, userId);
        return ApiResponse.success(tables);
    }

    @GetMapping("/ddl")
    public ApiResponse<String> getTableDdl(
            @RequestParam @NotNull(message = "connectionId is required") Long connectionId,
            @RequestParam @NotNull(message = "tableName is required") String tableName,
            @RequestParam(required = false) String catalog,
            @RequestParam(required = false) String schema) {
        log.info("Getting table DDL: connectionId={}, tableName={}, catalog={}, schema={}",
                connectionId, tableName, catalog, schema);
        long userId = StpUtil.getLoginIdAsLong();
        String ddl = tableService.getTableDdl(connectionId, catalog, schema, tableName, userId);
        return ApiResponse.success(ddl);
    }

    @DeleteMapping
    public ApiResponse<Void> deleteTable(@Valid @RequestBody DeleteTableRequest request) {
        log.info("Deleting table: connectionId={}, tableName={}, catalog={}, schema={}",
                request.getConnectionId(), request.getTableName(), request.getCatalog(), request.getSchema());
        long userId = StpUtil.getLoginIdAsLong();
        tableService.deleteTable(request.getConnectionId(), request.getCatalog(),
                request.getSchema(), request.getTableName(), userId);
        return ApiResponse.success(null);
    }

    @GetMapping("/data")
    public ApiResponse<TableDataResponse> getTableData(
            @RequestParam @NotNull(message = "connectionId is required") Long connectionId,
            @RequestParam @NotNull(message = "tableName is required") String tableName,
            @RequestParam(required = false) String catalog,
            @RequestParam(required = false) String schema,
            @RequestParam(defaultValue = "1") Integer currentPage,
            @RequestParam(defaultValue = "100") Integer pageSize) {
        log.info("Getting table data: connectionId={}, tableName={}, catalog={}, schema={}, currentPage={}, pageSize={}",
                connectionId, tableName, catalog, schema, currentPage, pageSize);
        long userId = StpUtil.getLoginIdAsLong();
        TableDataResponse response = tableService.getTableData(connectionId, catalog, schema, tableName, userId, currentPage, pageSize);
        return ApiResponse.success(response);
    }
}
