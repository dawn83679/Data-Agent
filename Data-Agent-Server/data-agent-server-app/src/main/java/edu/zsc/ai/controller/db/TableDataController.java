package edu.zsc.ai.controller.db;

import cn.dev33.satoken.stp.StpUtil;
import edu.zsc.ai.domain.model.dto.request.db.DeleteDataRequest;
import edu.zsc.ai.domain.model.dto.request.db.DeleteViewDataRequest;
import edu.zsc.ai.domain.model.dto.request.db.InsertDataRequest;
import edu.zsc.ai.domain.model.dto.request.db.InsertViewDataRequest;
import edu.zsc.ai.domain.model.dto.request.db.UpdateDataRequest;
import edu.zsc.ai.domain.model.dto.request.db.UpdateViewDataRequest;
import edu.zsc.ai.domain.model.dto.response.base.ApiResponse;
import edu.zsc.ai.domain.model.dto.response.db.DataModificationResponse;
import edu.zsc.ai.domain.model.dto.response.db.TableDataResponse;
import edu.zsc.ai.domain.service.db.TableDataService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

    @PostMapping("/insert")
    public ApiResponse<DataModificationResponse> insertData(@RequestBody @Valid InsertDataRequest request) {
        log.info("Inserting data: connectionId={}, databaseName={}, tableName={}",
                request.getConnectionId(), request.getDatabaseName(), request.getTableName());
        long userId = StpUtil.getLoginIdAsLong();
        DataModificationResponse response = tableDataService.insertData(
                request.getConnectionId(),
                request.getDatabaseName(),
                request.getSchemaName(),
                request.getTableName(),
                request.getColumns(),
                request.getValues(),
                userId
        );
        return ApiResponse.success(response);
    }

    @PostMapping("/update")
    public ApiResponse<DataModificationResponse> updateData(@RequestBody @Valid UpdateDataRequest request) {
        log.info("Updating data: connectionId={}, databaseName={}, tableName={}",
                request.getConnectionId(), request.getDatabaseName(), request.getTableName());
        long userId = StpUtil.getLoginIdAsLong();
        DataModificationResponse response = tableDataService.updateData(
                request.getConnectionId(),
                request.getDatabaseName(),
                request.getSchemaName(),
                request.getTableName(),
                request.getValues(),
                request.getWhereConditions(),
                userId
        );
        return ApiResponse.success(response);
    }

    @PostMapping("/delete")
    public ApiResponse<DataModificationResponse> deleteData(@RequestBody @Valid DeleteDataRequest request) {
        log.info("Deleting data: connectionId={}, databaseName={}, tableName={}",
                request.getConnectionId(), request.getDatabaseName(), request.getTableName());
        long userId = StpUtil.getLoginIdAsLong();
        DataModificationResponse response = tableDataService.deleteData(
                request.getConnectionId(),
                request.getDatabaseName(),
                request.getSchemaName(),
                request.getTableName(),
                request.getWhereConditions(),
                userId
        );
        return ApiResponse.success(response);
    }

    @PostMapping("/view/insert")
    public ApiResponse<DataModificationResponse> insertViewData(@RequestBody @Valid InsertViewDataRequest request) {
        log.info("Inserting data into view: connectionId={}, databaseName={}, viewName={}",
                request.getConnectionId(), request.getDatabaseName(), request.getViewName());
        long userId = StpUtil.getLoginIdAsLong();
        DataModificationResponse response = tableDataService.insertViewData(
                request.getConnectionId(),
                request.getDatabaseName(),
                request.getSchemaName(),
                request.getViewName(),
                request.getColumns(),
                request.getValues(),
                userId
        );
        return ApiResponse.success(response);
    }

    @PostMapping("/view/update")
    public ApiResponse<DataModificationResponse> updateViewData(@RequestBody @Valid UpdateViewDataRequest request) {
        log.info("Updating data in view: connectionId={}, databaseName={}, viewName={}",
                request.getConnectionId(), request.getDatabaseName(), request.getViewName());
        long userId = StpUtil.getLoginIdAsLong();
        DataModificationResponse response = tableDataService.updateViewData(
                request.getConnectionId(),
                request.getDatabaseName(),
                request.getSchemaName(),
                request.getViewName(),
                request.getValues(),
                request.getWhereConditions(),
                userId
        );
        return ApiResponse.success(response);
    }

    @PostMapping("/view/delete")
    public ApiResponse<DataModificationResponse> deleteViewData(@RequestBody @Valid DeleteViewDataRequest request) {
        log.info("Deleting data from view: connectionId={}, databaseName={}, viewName={}",
                request.getConnectionId(), request.getDatabaseName(), request.getViewName());
        long userId = StpUtil.getLoginIdAsLong();
        DataModificationResponse response = tableDataService.deleteViewData(
                request.getConnectionId(),
                request.getDatabaseName(),
                request.getSchemaName(),
                request.getViewName(),
                request.getWhereConditions(),
                userId
        );
        return ApiResponse.success(response);
    }
}
