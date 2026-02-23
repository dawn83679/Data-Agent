package edu.zsc.ai.controller.db;

import cn.dev33.satoken.stp.StpUtil;
import edu.zsc.ai.domain.model.dto.request.db.CreateDatabaseRequest;
import edu.zsc.ai.domain.model.dto.request.db.CreateFunctionRequest;
import edu.zsc.ai.domain.model.dto.request.db.CreateProcedureRequest;
import edu.zsc.ai.domain.model.dto.request.db.CreateTableRequest;
import edu.zsc.ai.domain.model.dto.request.db.CreateTriggerRequest;
import edu.zsc.ai.domain.model.dto.request.db.CreateViewRequest;
import edu.zsc.ai.domain.model.dto.request.db.DeleteDatabaseRequest;
import edu.zsc.ai.domain.model.dto.response.base.ApiResponse;
import edu.zsc.ai.domain.service.db.DatabaseService;
import edu.zsc.ai.plugin.capability.DatabaseProvider.ColumnDefinition;
import edu.zsc.ai.plugin.capability.DatabaseProvider.CreateRoutineOptions;
import edu.zsc.ai.plugin.capability.DatabaseProvider.CreateTableOptions;
import edu.zsc.ai.plugin.capability.DatabaseProvider.CreateTriggerOptions;
import edu.zsc.ai.plugin.capability.DatabaseProvider.CreateViewOptions;
import edu.zsc.ai.plugin.capability.DatabaseProvider.ForeignKeyDefinition;
import edu.zsc.ai.plugin.capability.DatabaseProvider.IndexDefinition;
import edu.zsc.ai.plugin.capability.DatabaseProvider.ParameterDefinition;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
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

    @PostMapping("/delete")
    public ApiResponse<Void> deleteDatabase(@RequestBody @Validated DeleteDatabaseRequest request) {
        log.info("Deleting database: connectionId={}, databaseName={}", request.getConnectionId(), request.getDatabaseName());
        long userId = StpUtil.getLoginIdAsLong();
        databaseService.deleteDatabase(request.getConnectionId(), request.getDatabaseName(), userId);
        return ApiResponse.success(null);
    }

    @GetMapping("/charsets")
    public ApiResponse<List<String>> getCharacterSets(
            @RequestParam @NotNull(message = "connectionId is required") Long connectionId) {
        log.info("Getting character sets: connectionId={}", connectionId);
        List<String> charsets = databaseService.getCharacterSets(connectionId);
        return ApiResponse.success(charsets);
    }

    @GetMapping("/collations")
    public ApiResponse<List<String>> getCollations(
            @RequestParam @NotNull(message = "connectionId is required") Long connectionId,
            @RequestParam @NotNull(message = "charset is required") String charset) {
        log.info("Getting collations: connectionId={}, charset={}", connectionId, charset);
        List<String> collations = databaseService.getCollations(connectionId, charset);
        return ApiResponse.success(collations);
    }

    @PostMapping("/create")
    public ApiResponse<Void> createDatabase(@RequestBody @Validated CreateDatabaseRequest request) {
        log.info("Creating database: connectionId={}, databaseName={}, charset={}, collation={}",
                request.getConnectionId(), request.getDatabaseName(), request.getCharset(), request.getCollation());
        long userId = StpUtil.getLoginIdAsLong();
        databaseService.createDatabase(request.getConnectionId(), request.getDatabaseName(),
                request.getCharset(), request.getCollation(), userId);
        return ApiResponse.success(null);
    }

    @GetMapping("/exists")
    public ApiResponse<Boolean> databaseExists(
            @RequestParam @NotNull(message = "connectionId is required") Long connectionId,
            @RequestParam @NotNull(message = "databaseName is required") String databaseName) {
        log.info("Checking database existence: connectionId={}, databaseName={}", connectionId, databaseName);
        long userId = StpUtil.getLoginIdAsLong();
        boolean exists = databaseService.databaseExists(connectionId, databaseName, userId);
        return ApiResponse.success(exists);
    }

    @GetMapping("/engines")
    public ApiResponse<List<String>> getTableEngines(
            @RequestParam @NotNull(message = "connectionId is required") Long connectionId) {
        log.info("Getting table engines: connectionId={}", connectionId);
        List<String> engines = databaseService.getTableEngines(connectionId);
        return ApiResponse.success(engines);
    }

    @PostMapping("/tables/create")
    public ApiResponse<Void> createTable(@RequestBody @Validated CreateTableRequest request) {
        log.info("Creating table: connectionId={}, databaseName={}, tableName={}",
                request.getConnectionId(), request.getDatabaseName(), request.getTableName());

        // Convert DTO ColumnDefinition to Provider ColumnDefinition
        List<ColumnDefinition> columns = request.getColumns().stream()
                .map(col -> {
                    ColumnDefinition definition = new ColumnDefinition();
                    definition.setName(col.getName());
                    definition.setType(col.getType());
                    definition.setLength(col.getLength());
                    definition.setDecimals(col.getDecimals());
                    definition.setNullable(col.isNullable());
                    definition.setKeyType(col.getKeyType());
                    definition.setDefaultValue(col.getDefaultValue());
                    definition.setComment(col.getComment());
                    definition.setAutoIncrement(col.isAutoIncrement());
                    return definition;
                })
                .toList();

        // Build CreateTableOptions
        CreateTableOptions options = new CreateTableOptions();
        options.setEngine(request.getEngine());
        options.setCharset(request.getCharset());
        options.setCollation(request.getCollation());
        options.setComment(request.getComment());
        options.setPrimaryKey(request.getPrimaryKey());

        // Convert indexes
        if (request.getIndexes() != null) {
            List<IndexDefinition> indexes = request.getIndexes().stream()
                    .map(idx -> {
                        IndexDefinition definition = new IndexDefinition();
                        definition.setName(idx.getName());
                        definition.setColumns(idx.getColumns());
                        definition.setType(idx.getType());
                        return definition;
                    })
                    .toList();
            options.setIndexes(indexes);
        }

        // Convert foreignKeys
        if (request.getForeignKeys() != null) {
            List<ForeignKeyDefinition> foreignKeys = request.getForeignKeys().stream()
                    .map(fk -> {
                        ForeignKeyDefinition definition = new ForeignKeyDefinition();
                        definition.setName(fk.getName());
                        definition.setColumn(fk.getColumn());
                        definition.setReferencedTable(fk.getReferencedTable());
                        definition.setReferencedColumn(fk.getReferencedColumn());
                        definition.setOnDelete(fk.getOnDelete());
                        definition.setOnUpdate(fk.getOnUpdate());
                        return definition;
                    })
                    .toList();
            options.setForeignKeys(foreignKeys);
        }

        options.setConstraints(request.getConstraints());

        long userId = StpUtil.getLoginIdAsLong();
        databaseService.createTable(request.getConnectionId(), request.getDatabaseName(), request.getTableName(),
                columns, options, userId);
        return ApiResponse.success(null);
    }

    @PostMapping("/views/create")
    public ApiResponse<Void> createView(@RequestBody @Validated CreateViewRequest request) {
        log.info("Creating view: connectionId={}, databaseName={}, viewName={}",
                request.getConnectionId(), request.getDatabaseName(), request.getViewName());

        // Build CreateViewOptions
        CreateViewOptions options = new CreateViewOptions();
        options.setAlgorithm(request.getAlgorithm());
        options.setDefiner(request.getDefiner());
        options.setSqlSecurity(request.getSqlSecurity());
        options.setCheckOption(request.getCheckOption());

        long userId = StpUtil.getLoginIdAsLong();
        databaseService.createView(request.getConnectionId(), request.getDatabaseName(), request.getViewName(),
                request.getQuery(), options, userId);
        return ApiResponse.success(null);
    }

    @PostMapping("/triggers/create")
    public ApiResponse<Void> createTrigger(@RequestBody @Validated CreateTriggerRequest request) {
        log.info("Creating trigger: connectionId={}, databaseName={}, triggerName={}",
                request.getConnectionId(), request.getDatabaseName(), request.getTriggerName());

        long userId = StpUtil.getLoginIdAsLong();
        databaseService.createTrigger(request.getConnectionId(), request.getDatabaseName(), request.getSchemaName(),
                request.getTriggerName(), request.getTableName(), request.getTiming(), request.getEvent(),
                request.getBody(), null, userId);
        return ApiResponse.success(null);
    }

    @PostMapping("/procedures/create")
    public ApiResponse<Void> createProcedure(@RequestBody @Validated CreateProcedureRequest request) {
        log.info("Creating procedure: connectionId={}, databaseName={}, procedureName={}",
                request.getConnectionId(), request.getDatabaseName(), request.getProcedureName());

        // Convert parameters
        List<ParameterDefinition> parameters = null;
        if (request.getParameters() != null) {
            parameters = request.getParameters().stream()
                    .map(param -> {
                        ParameterDefinition definition = new ParameterDefinition();
                        definition.setName(param.getName());
                        definition.setType(param.getType());
                        definition.setMode(param.getMode());
                        return definition;
                    })
                    .toList();
        }

        long userId = StpUtil.getLoginIdAsLong();
        databaseService.createProcedure(request.getConnectionId(), request.getDatabaseName(), request.getSchemaName(),
                request.getProcedureName(), parameters, request.getBody(), null, userId);
        return ApiResponse.success(null);
    }

    @PostMapping("/functions/create")
    public ApiResponse<Void> createFunction(@RequestBody @Validated CreateFunctionRequest request) {
        log.info("Creating function: connectionId={}, databaseName={}, functionName={}",
                request.getConnectionId(), request.getDatabaseName(), request.getFunctionName());

        // Convert parameters
        List<ParameterDefinition> parameters = null;
        if (request.getParameters() != null) {
            parameters = request.getParameters().stream()
                    .map(param -> {
                        ParameterDefinition definition = new ParameterDefinition();
                        definition.setName(param.getName());
                        definition.setType(param.getType());
                        // Functions only support IN mode
                        definition.setMode("IN");
                        return definition;
                    })
                    .toList();
        }

        long userId = StpUtil.getLoginIdAsLong();
        databaseService.createFunction(request.getConnectionId(), request.getDatabaseName(), request.getSchema(),
                request.getFunctionName(), parameters, request.getReturns(), request.getBody(), null, userId);
        return ApiResponse.success(null);
    }
}
