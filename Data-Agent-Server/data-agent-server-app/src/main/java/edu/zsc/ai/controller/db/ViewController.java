package edu.zsc.ai.controller.db;

import java.util.List;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import edu.zsc.ai.domain.model.dto.response.base.ApiResponse;
import edu.zsc.ai.domain.service.db.ViewService;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
        List<String> views = viewService.listViews(connectionId, catalog, schema);
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
        String ddl = viewService.getViewDdl(connectionId, catalog, schema, viewName);
        return ApiResponse.success(ddl);
    }
}
