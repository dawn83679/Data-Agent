package edu.zsc.ai.domain.service.db.impl;

import edu.zsc.ai.domain.model.context.DbContext;
import edu.zsc.ai.domain.service.db.DatabaseObjectService;
import edu.zsc.ai.domain.service.db.FunctionService;
import edu.zsc.ai.domain.service.db.ProcedureService;
import edu.zsc.ai.domain.service.db.TableService;
import edu.zsc.ai.domain.service.db.TriggerService;
import edu.zsc.ai.domain.service.db.ViewService;
import edu.zsc.ai.plugin.constant.DatabaseObjectTypeEnum;
import edu.zsc.ai.plugin.model.metadata.FunctionMetadata;
import edu.zsc.ai.plugin.model.metadata.ProcedureMetadata;
import edu.zsc.ai.plugin.model.metadata.TriggerMetadata;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DatabaseObjectServiceImpl implements DatabaseObjectService {

    private static final EnumSet<DatabaseObjectTypeEnum> COUNT_SUPPORTED_TYPES = EnumSet.of(
            DatabaseObjectTypeEnum.TABLE,
            DatabaseObjectTypeEnum.VIEW,
            DatabaseObjectTypeEnum.FUNCTION,
            DatabaseObjectTypeEnum.PROCEDURE
    );
    private static final EnumSet<DatabaseObjectTypeEnum> ROW_COUNT_SUPPORTED_TYPES = EnumSet.of(
            DatabaseObjectTypeEnum.TABLE,
            DatabaseObjectTypeEnum.VIEW
    );

    private final TableService tableService;
    private final ViewService viewService;
    private final FunctionService functionService;
    private final ProcedureService procedureService;
    private final TriggerService triggerService;

    @Override
    public List<String> getObjectNames(DatabaseObjectTypeEnum objectType, DbContext db, String tableName) {
        return switch (objectType) {
            case TABLE -> tableService.getTables(db);
            case VIEW -> viewService.getViews(db);
            case FUNCTION -> functionService.getFunctions(db).stream()
                    .map(FunctionMetadata::name)
                    .collect(Collectors.toList());
            case PROCEDURE -> procedureService.getProcedures(db).stream()
                    .map(ProcedureMetadata::name)
                    .collect(Collectors.toList());
            case TRIGGER -> {
                validateTriggerTableName(tableName);
                yield triggerService.getTriggers(db, tableName).stream()
                        .map(TriggerMetadata::name)
                        .collect(Collectors.toList());
            }
            default -> throw new IllegalArgumentException("Unsupported objectType: " + objectType);
        };
    }

    @Override
    public List<String> searchObjects(DatabaseObjectTypeEnum objectType, String namePattern,
                                      DbContext db, String tableName) {
        return switch (objectType) {
            case TABLE -> tableService.searchTables(db, namePattern);
            case VIEW -> viewService.searchViews(db, namePattern);
            case FUNCTION -> functionService.searchFunctions(db, namePattern).stream()
                    .map(FunctionMetadata::name)
                    .collect(Collectors.toList());
            case PROCEDURE -> procedureService.searchProcedures(db, namePattern).stream()
                    .map(ProcedureMetadata::name)
                    .collect(Collectors.toList());
            case TRIGGER -> {
                validateTriggerTableName(tableName);
                yield triggerService.searchTriggers(db, tableName, namePattern).stream()
                        .map(TriggerMetadata::name)
                        .collect(Collectors.toList());
            }
            default -> throw new IllegalArgumentException("Unsupported objectType: " + objectType);
        };
    }

    @Override
    public long countObjects(DatabaseObjectTypeEnum objectType, String namePattern,
                             DbContext db, String tableName) {
        if (!COUNT_SUPPORTED_TYPES.contains(objectType)) {
            throw new IllegalArgumentException("Unsupported objectType for countObjects: " + objectType);
        }
        return switch (objectType) {
            case TABLE -> tableService.countTables(db, namePattern);
            case VIEW -> viewService.countViews(db, namePattern);
            case FUNCTION -> functionService.countFunctions(db, namePattern);
            case PROCEDURE -> procedureService.countProcedures(db, namePattern);
            default -> throw new IllegalArgumentException("Unsupported objectType for countObjects: " + objectType);
        };
    }

    @Override
    public long countObjectRows(DatabaseObjectTypeEnum objectType, DbContext db, String objectName) {
        if (!ROW_COUNT_SUPPORTED_TYPES.contains(objectType)) {
            throw new IllegalArgumentException("Unsupported objectType for countObjectRows: " + objectType);
        }
        return switch (objectType) {
            case TABLE -> tableService.countTableRows(db, objectName);
            case VIEW -> viewService.countViewRows(db, objectName);
            default -> throw new IllegalArgumentException("Unsupported objectType for countObjectRows: " + objectType);
        };
    }

    @Override
    public String getObjectDdl(DatabaseObjectTypeEnum objectType, String objectName, DbContext db) {
        return switch (objectType) {
            case TABLE -> tableService.getTableDdl(db, objectName);
            case VIEW -> viewService.getViewDdl(db, objectName);
            case FUNCTION -> functionService.getFunctionDdl(db, objectName);
            case PROCEDURE -> procedureService.getProcedureDdl(db, objectName);
            case TRIGGER -> triggerService.getTriggerDdl(db, objectName);
            default -> throw new IllegalArgumentException("Unsupported objectType: " + objectType);
        };
    }

    private void validateTriggerTableName(String tableName) {
        if (StringUtils.isBlank(tableName)) {
            throw new IllegalArgumentException("tableName is required when objectType=TRIGGER");
        }
    }
}
