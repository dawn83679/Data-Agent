package edu.zsc.ai.domain.service.db.impl;

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

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DatabaseObjectServiceImpl implements DatabaseObjectService {

    private final TableService tableService;
    private final ViewService viewService;
    private final FunctionService functionService;
    private final ProcedureService procedureService;
    private final TriggerService triggerService;

    @Override
    public List<String> getObjectNames(DatabaseObjectTypeEnum objectType,
                                       Long connectionId,
                                       String catalog,
                                       String schema,
                                       String tableName,
                                       Long userId) {
        return switch (objectType) {
            case TABLE -> tableService.getTables(connectionId, catalog, schema, userId);
            case VIEW -> viewService.getViews(connectionId, catalog, schema, userId);
            case FUNCTION -> functionService.getFunctions(connectionId, catalog, schema, userId).stream()
                    .map(FunctionMetadata::name)
                    .collect(Collectors.toList());
            case PROCEDURE -> procedureService.getProcedures(connectionId, catalog, schema, userId).stream()
                    .map(ProcedureMetadata::name)
                    .collect(Collectors.toList());
            case TRIGGER -> {
                validateTriggerTableName(tableName);
                yield triggerService.getTriggers(connectionId, catalog, schema, tableName, userId).stream()
                        .map(TriggerMetadata::name)
                        .collect(Collectors.toList());
            }
            default -> throw new IllegalArgumentException("Unsupported objectType: " + objectType);
        };
    }

    @Override
    public List<String> searchObjects(DatabaseObjectTypeEnum objectType,
                                      String namePattern,
                                      Long connectionId,
                                      String catalog,
                                      String schema,
                                      String tableName,
                                      Long userId) {
        return switch (objectType) {
            case TABLE -> tableService.searchTables(connectionId, catalog, schema, namePattern, userId);
            case VIEW -> viewService.searchViews(connectionId, catalog, schema, namePattern, userId);
            case FUNCTION -> functionService.searchFunctions(connectionId, catalog, schema, namePattern, userId).stream()
                    .map(FunctionMetadata::name)
                    .collect(Collectors.toList());
            case PROCEDURE -> procedureService.searchProcedures(connectionId, catalog, schema, namePattern, userId).stream()
                    .map(ProcedureMetadata::name)
                    .collect(Collectors.toList());
            case TRIGGER -> {
                validateTriggerTableName(tableName);
                yield triggerService.searchTriggers(connectionId, catalog, schema, tableName, namePattern, userId).stream()
                        .map(TriggerMetadata::name)
                        .collect(Collectors.toList());
            }
            default -> throw new IllegalArgumentException("Unsupported objectType: " + objectType);
        };
    }

    @Override
    public String getObjectDdl(DatabaseObjectTypeEnum objectType,
                               String objectName,
                               Long connectionId,
                               String catalog,
                               String schema,
                               Long userId) {
        return switch (objectType) {
            case TABLE -> tableService.getTableDdl(connectionId, catalog, schema, objectName, userId);
            case VIEW -> viewService.getViewDdl(connectionId, catalog, schema, objectName, userId);
            case FUNCTION -> functionService.getFunctionDdl(connectionId, catalog, schema, objectName, userId);
            case PROCEDURE -> procedureService.getProcedureDdl(connectionId, catalog, schema, objectName, userId);
            case TRIGGER -> triggerService.getTriggerDdl(connectionId, catalog, schema, objectName, userId);
            default -> throw new IllegalArgumentException("Unsupported objectType: " + objectType);
        };
    }

    private void validateTriggerTableName(String tableName) {
        if (StringUtils.isBlank(tableName)) {
            throw new IllegalArgumentException("tableName is required when objectType=TRIGGER");
        }
    }
}
