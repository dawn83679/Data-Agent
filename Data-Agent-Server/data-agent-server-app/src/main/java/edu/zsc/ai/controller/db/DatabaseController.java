package edu.zsc.ai.controller.db;

import edu.zsc.ai.domain.model.dto.response.base.ApiResponse;
import edu.zsc.ai.domain.service.db.DatabaseService;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Database Controller
 * Provides REST API for listing databases on an active connection.
 *
 * @author Data-Agent
 * @since 0.0.1
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/databases")
@RequiredArgsConstructor
public class DatabaseController {

    private final DatabaseService databaseService;

    /**
     * List databases available on the data source for the given connection.
     *
     * @param connectionId unique connection identifier (required)
     * @return list of database names
     */
    @GetMapping
    public ApiResponse<List<String>> listDatabases(
            @RequestParam @NotBlank(message = "connectionId is required") String connectionId) {
        log.info("Listing databases: connectionId={}", connectionId);
        List<String> databases = databaseService.listDatabases(connectionId);
        return ApiResponse.success(databases);
    }
}
