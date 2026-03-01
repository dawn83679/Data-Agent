package edu.zsc.ai.api.controller.db;

import cn.dev33.satoken.stp.StpUtil;
import edu.zsc.ai.domain.model.dto.request.db.DeleteViewRequest;
import edu.zsc.ai.domain.model.dto.response.base.ApiResponse;
import edu.zsc.ai.domain.model.dto.response.db.TableDataResponse;
import edu.zsc.ai.domain.service.db.ViewService;
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
@RequestMapping("/api/views")
@RequiredArgsConstructor
public class ViewController {

    private final ViewService viewService;

    @GetMapping
    public ApiResponse<List<String>> listViews(
            @RequestParam @NotNull(message = "connectionId is required") Long connectionId,
            @RequestParam(required = false) String catalog,
            @RequestParam(required = false) String schema) {
        log.info("Listing views: connectionId={}, catalog={}, schema={}", connectionId, catalog, schema);
        long userId = StpUtil.getLoginIdAsLong();
        List<String> views = viewService.getViews(connectionId, catalog, schema, userId);
        return ApiResponse.success(views);
    }

    @GetMapping("/ddl")
    public ApiResponse<String> getViewDdl(
            @RequestParam @NotNull(message = "connectionId is required") Long connectionId,
            @RequestParam @NotNull(message = "viewName is required") String viewName,
            @RequestParam(required = false) String catalog,
            @RequestParam(required = false) String schema) {
        log.info("Getting view DDL: connectionId={}, viewName={}, catalog={}, schema={}",
                connectionId, viewName, catalog, schema);
        long userId = StpUtil.getLoginIdAsLong();
        String ddl = viewService.getViewDdl(connectionId, catalog, schema, viewName, userId);
        return ApiResponse.success(ddl);
    }

    @DeleteMapping
    public ApiResponse<Void> deleteView(@Valid @RequestBody DeleteViewRequest request) {
        log.info("Deleting view: connectionId={}, viewName={}, catalog={}, schema={}",
                request.getConnectionId(), request.getViewName(), request.getCatalog(), request.getSchema());
        long userId = StpUtil.getLoginIdAsLong();
        viewService.deleteView(request.getConnectionId(), request.getCatalog(),
                request.getSchema(), request.getViewName(), userId);
        return ApiResponse.success(null);
    }

    @GetMapping("/data")
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
        TableDataResponse response = viewService.getViewData(connectionId, catalog, schema, viewName, userId, currentPage, pageSize);
        return ApiResponse.success(response);
    }
}
