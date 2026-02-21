package edu.zsc.ai.controller.db;

import cn.dev33.satoken.stp.StpUtil;
import edu.zsc.ai.domain.model.dto.response.base.ApiResponse;
import edu.zsc.ai.domain.service.db.ProcedureService;
import edu.zsc.ai.plugin.model.metadata.ProcedureMetadata;
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
@RequestMapping("/api/procedures")
@RequiredArgsConstructor
public class ProcedureController {

    private final ProcedureService procedureService;

    @GetMapping
    public ApiResponse<List<ProcedureMetadata>> listProcedures(
            @RequestParam @NotNull(message = "connectionId is required") Long connectionId,
            @RequestParam(required = false) String catalog,
            @RequestParam(required = false) String schema) {
        log.info("Listing procedures: connectionId={}, catalog={}, schema={}", connectionId, catalog, schema);
        long userId = StpUtil.getLoginIdAsLong();
        List<ProcedureMetadata> procedures = procedureService.listProcedures(connectionId, catalog, schema, userId);
        return ApiResponse.success(procedures);
    }

    @GetMapping("/ddl")
    public ApiResponse<String> getProcedureDdl(
            @RequestParam @NotNull(message = "connectionId is required") Long connectionId,
            @RequestParam @NotNull(message = "procedureName is required") String procedureName,
            @RequestParam(required = false) String catalog,
            @RequestParam(required = false) String schema) {
        log.info("Getting procedure DDL: connectionId={}, procedureName={}, catalog={}, schema={}",
                connectionId, procedureName, catalog, schema);
        long userId = StpUtil.getLoginIdAsLong();
        String ddl = procedureService.getProcedureDdl(connectionId, catalog, schema, procedureName, userId);
        return ApiResponse.success(ddl);
    }

    @PostMapping("/delete")
    public ApiResponse<Void> deleteProcedure(
            @RequestParam @NotNull(message = "connectionId is required") Long connectionId,
            @RequestParam @NotNull(message = "procedureName is required") String procedureName,
            @RequestParam(required = false) String catalog,
            @RequestParam(required = false) String schema) {
        log.info("Deleting procedure: connectionId={}, procedureName={}, catalog={}, schema={}",
                connectionId, procedureName, catalog, schema);
        long userId = StpUtil.getLoginIdAsLong();
        procedureService.deleteProcedure(connectionId, catalog, schema, procedureName, userId);
        return ApiResponse.success(null);
    }
}
