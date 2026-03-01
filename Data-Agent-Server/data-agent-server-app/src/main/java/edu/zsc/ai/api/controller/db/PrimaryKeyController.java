package edu.zsc.ai.api.controller.db;

import cn.dev33.satoken.stp.StpUtil;
import edu.zsc.ai.domain.model.dto.response.base.ApiResponse;
import edu.zsc.ai.domain.service.db.PrimaryKeyService;
import edu.zsc.ai.plugin.model.metadata.PrimaryKeyMetadata;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@Validated
@RestController
@RequestMapping("/api/primary-keys")
@RequiredArgsConstructor
public class PrimaryKeyController {

    private final PrimaryKeyService primaryKeyService;

    @GetMapping
    public ApiResponse<List<PrimaryKeyMetadata>> listPrimaryKeys(
            @RequestParam @NotNull(message = "connectionId is required") Long connectionId,
            @RequestParam @NotNull(message = "tableName is required") String tableName,
            @RequestParam(required = false) String catalog,
            @RequestParam(required = false) String schema) {
        log.info("Listing primary keys: connectionId={}, catalog={}, schema={}, tableName={}",
                connectionId, catalog, schema, tableName);
        long userId = StpUtil.getLoginIdAsLong();
        List<PrimaryKeyMetadata> primaryKeys = primaryKeyService.listPrimaryKeys(connectionId, catalog, schema, tableName, userId);
        return ApiResponse.success(primaryKeys);
    }
}
