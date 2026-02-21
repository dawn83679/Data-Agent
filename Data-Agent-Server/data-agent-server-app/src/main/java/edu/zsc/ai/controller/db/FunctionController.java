package edu.zsc.ai.controller.db;

import cn.dev33.satoken.stp.StpUtil;
import edu.zsc.ai.domain.model.dto.response.base.ApiResponse;
import edu.zsc.ai.domain.service.db.FunctionService;
import edu.zsc.ai.plugin.model.metadata.FunctionMetadata;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@Validated
@RestController
@RequestMapping("/api/functions")
@RequiredArgsConstructor
public class FunctionController {

    private final FunctionService functionService;

    @GetMapping
    public ApiResponse<List<FunctionMetadata>> listFunctions(
            @RequestParam @NotNull(message = "connectionId is required") Long connectionId,
            @RequestParam(required = false) String catalog,
            @RequestParam(required = false) String schema) {
        log.info("Listing functions: connectionId={}, catalog={}, schema={}", connectionId, catalog, schema);
        long userId = StpUtil.getLoginIdAsLong();
        List<FunctionMetadata> functions = functionService.listFunctions(connectionId, catalog, schema, userId);
        return ApiResponse.success(functions);
    }

    @GetMapping("/ddl")
    public ApiResponse<String> getFunctionDdl(
            @RequestParam @NotNull(message = "connectionId is required") Long connectionId,
            @RequestParam @NotNull(message = "functionName is required") String functionName,
            @RequestParam(required = false) String catalog,
            @RequestParam(required = false) String schema) {
        log.info("Getting function DDL: connectionId={}, functionName={}, catalog={}, schema={}",
                connectionId, functionName, catalog, schema);
        long userId = StpUtil.getLoginIdAsLong();
        String ddl = functionService.getFunctionDdl(connectionId, catalog, schema, functionName, userId);
        return ApiResponse.success(ddl);
    }

    @PostMapping("/delete")
    public ApiResponse<Void> deleteFunction(
            @RequestParam @NotNull(message = "connectionId is required") Long connectionId,
            @RequestParam @NotNull(message = "functionName is required") String functionName,
            @RequestParam(required = false) String catalog,
            @RequestParam(required = false) String schema) {
        log.info("Deleting function: connectionId={}, functionName={}, catalog={}, schema={}",
                connectionId, functionName, catalog, schema);
        long userId = StpUtil.getLoginIdAsLong();
        functionService.deleteFunction(connectionId, catalog, schema, functionName, userId);
        return ApiResponse.success(null);
    }
}
