package edu.zsc.ai.controller.db;

import cn.dev33.satoken.stp.StpUtil;
import edu.zsc.ai.domain.model.dto.response.base.ApiResponse;
import edu.zsc.ai.domain.service.db.IndexService;
import edu.zsc.ai.plugin.model.metadata.IndexMetadata;
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
@RequestMapping("/api/indexes")
@RequiredArgsConstructor
public class IndexController {

    private final IndexService indexService;

    @GetMapping
    public ApiResponse<List<IndexMetadata>> listIndexes(
            @RequestParam @NotNull(message = "connectionId is required") Long connectionId,
            @RequestParam @NotNull(message = "tableName is required") String tableName,
            @RequestParam(required = false) String catalog,
            @RequestParam(required = false) String schema) {
        log.info("Listing indexes: connectionId={}, catalog={}, schema={}, tableName={}",
                connectionId, catalog, schema, tableName);
        long userId = StpUtil.getLoginIdAsLong();
        List<IndexMetadata> indexes = indexService.getIndexes(connectionId, catalog, schema, tableName, userId);
        return ApiResponse.success(indexes);
    }
}
