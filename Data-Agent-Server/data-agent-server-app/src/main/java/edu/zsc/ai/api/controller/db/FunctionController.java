package edu.zsc.ai.api.controller.db;

import edu.zsc.ai.domain.model.context.DbContext;
import edu.zsc.ai.domain.model.dto.request.db.DeleteFunctionRequest;
import edu.zsc.ai.domain.model.dto.response.base.ApiResponse;
import edu.zsc.ai.domain.service.db.FunctionService;
import edu.zsc.ai.plugin.model.metadata.FunctionMetadata;
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
        DbContext db = new DbContext(connectionId, catalog, schema);
        List<FunctionMetadata> functions = functionService.getFunctions(db);
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
        DbContext db = new DbContext(connectionId, catalog, schema);
        String ddl = functionService.getFunctionDdl(db, functionName);
        return ApiResponse.success(ddl);
    }

    @DeleteMapping
    public ApiResponse<Void> deleteFunction(@Valid @RequestBody DeleteFunctionRequest request) {
        log.info("Deleting function: connectionId={}, functionName={}, catalog={}, schema={}",
                request.getConnectionId(), request.getFunctionName(), request.getCatalog(), request.getSchema());
        DbContext db = new DbContext(request.getConnectionId(), request.getCatalog(), request.getSchema());
        functionService.deleteFunction(db, request.getFunctionName());
        return ApiResponse.success(null);
    }
}
