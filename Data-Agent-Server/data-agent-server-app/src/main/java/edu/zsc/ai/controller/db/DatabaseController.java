package edu.zsc.ai.controller.db;

import cn.dev33.satoken.stp.StpUtil;
import edu.zsc.ai.domain.model.dto.request.db.DeleteDatabaseRequest;
import edu.zsc.ai.domain.model.dto.response.base.ApiResponse;
import edu.zsc.ai.domain.service.db.DatabaseService;
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
@RequestMapping("/api/databases")
@RequiredArgsConstructor
public class DatabaseController {

    private final DatabaseService databaseService;

    @GetMapping
    public ApiResponse<List<String>> listDatabases(
            @RequestParam @NotNull(message = "connectionId is required") Long connectionId) {
        log.info("Listing databases: connectionId={}", connectionId);
        List<String> databases = databaseService.listDatabases(connectionId);
        return ApiResponse.success(databases);
    }

    @DeleteMapping
    public ApiResponse<Void> deleteDatabase(@Valid @RequestBody DeleteDatabaseRequest request) {
        log.info("Deleting database: connectionId={}, databaseName={}",
                request.getConnectionId(), request.getDatabaseName());
        long userId = StpUtil.getLoginIdAsLong();
        databaseService.deleteDatabase(request.getConnectionId(), request.getDatabaseName(), userId);
        return ApiResponse.success(null);
    }
}
