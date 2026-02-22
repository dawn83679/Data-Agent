package edu.zsc.ai.controller.db;

import cn.dev33.satoken.stp.StpUtil;
import edu.zsc.ai.domain.model.dto.response.base.ApiResponse;
import edu.zsc.ai.domain.service.db.TriggerService;
import edu.zsc.ai.plugin.model.metadata.TriggerMetadata;
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
        List<TriggerMetadata> triggers = triggerService.listTriggers(connectionId, catalog, schema, tableName, userId);
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

    @PostMapping("/delete")
    public ApiResponse<Void> deleteTrigger(
            @RequestParam @NotNull(message = "connectionId is required") Long connectionId,
            @RequestParam @NotNull(message = "triggerName is required") String triggerName,
            @RequestParam(required = false) String catalog,
            @RequestParam(required = false) String schema) {
        log.info("Deleting trigger: connectionId={}, triggerName={}, catalog={}, schema={}",
                connectionId, triggerName, catalog, schema);
        long userId = StpUtil.getLoginIdAsLong();
        triggerService.deleteTrigger(connectionId, catalog, schema, triggerName, userId);
        return ApiResponse.success(null);
    }
}
