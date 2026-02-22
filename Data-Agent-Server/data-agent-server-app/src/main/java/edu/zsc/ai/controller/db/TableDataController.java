package edu.zsc.ai.controller.db;

import cn.dev33.satoken.stp.StpUtil;
import edu.zsc.ai.domain.model.dto.response.base.ApiResponse;
import edu.zsc.ai.domain.model.dto.response.db.TableDataResponse;
import edu.zsc.ai.domain.service.db.TableDataService;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Validated
@RestController
@RequestMapping("/api/table-data")
@RequiredArgsConstructor
public class TableDataController {

    private final TableDataService tableDataService;

    @GetMapping("/table")
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
        TableDataResponse response = tableDataService.getTableData(connectionId, catalog, schema, tableName, userId, currentPage, pageSize);
        return ApiResponse.success(response);
    }

    @GetMapping("/view")
    public ApiResponse<TableDataResponse> getViewData(
            @RequestParam @NotNull(message = "connectionId is required") Long connectionId,
            @RequestParam @NotNull(message = "viewName is required") String viewName,
            @RequestParam(required = false) String catalog,
            @RequestParam(required = false) String schema,
            @RequestParam(defaultValue = "1") Integer currentPage,
            @RequestParam(defaultValue = "100") Integer pageSize) {
        log.info("Getting view data: connectionId={}, viewName={}, catalog={}, schema={}, currentPage={}, pageSize={}",
                connectionId, viewName, catalog, schema, currentPage, pageSize);
        long userId = StpUtil.getLoginIdAsLong();
        TableDataResponse response = tableDataService.getViewData(connectionId, catalog, schema, viewName, userId, currentPage, pageSize);
        return ApiResponse.success(response);
    }
}
