package edu.zsc.ai.api.controller.db;

import cn.dev33.satoken.stp.StpUtil;
import edu.zsc.ai.domain.model.dto.request.db.DeleteTriggerRequest;
import edu.zsc.ai.domain.model.dto.response.base.ApiResponse;
import edu.zsc.ai.domain.service.db.TriggerService;
import edu.zsc.ai.plugin.model.metadata.TriggerMetadata;
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
@RequestMapping("/api/triggers")
@RequiredArgsConstructor
public class TriggerController {

    private final TriggerService triggerService;

    @GetMapping
    public ApiResponse<List<TriggerMetadata>> listTriggers(
            @RequestParam @NotNull(message = "connectionId is required") Long connectionId,
            @RequestParam(required = false) String catalog,
            @RequestParam(required = false) String schema,
            @RequestParam(required = false) String tableName) {
        log.info("Listing triggers: connectionId={}, catalog={}, schema={}, tableName={}",
                connectionId, catalog, schema, tableName);
        long userId = StpUtil.getLoginIdAsLong();
        List<TriggerMetadata> triggers = triggerService.getTriggers(connectionId, catalog, schema, tableName, userId);
        return ApiResponse.success(triggers);
    }

    @GetMapping("/ddl")
    public ApiResponse<String> getTriggerDdl(
            @RequestParam @NotNull(message = "connectionId is required") Long connectionId,
            @RequestParam @NotNull(message = "triggerName is required") String triggerName,
            @RequestParam(required = false) String catalog,
            @RequestParam(required = false) String schema) {
        log.info("Getting trigger DDL: connectionId={}, triggerName={}, catalog={}, schema={}",
                connectionId, triggerName, catalog, schema);
        long userId = StpUtil.getLoginIdAsLong();
        String ddl = triggerService.getTriggerDdl(connectionId, catalog, schema, triggerName, userId);
        return ApiResponse.success(ddl);
    }

    @DeleteMapping
    public ApiResponse<Void> deleteTrigger(@Valid @RequestBody DeleteTriggerRequest request) {
        log.info("Deleting trigger: connectionId={}, triggerName={}, catalog={}, schema={}",
                request.getConnectionId(), request.getTriggerName(), request.getCatalog(), request.getSchema());
        long userId = StpUtil.getLoginIdAsLong();
        triggerService.deleteTrigger(request.getConnectionId(), request.getCatalog(),
                request.getSchema(), request.getTriggerName(), userId);
        return ApiResponse.success(null);
    }
}
