package edu.zsc.ai.domain.service.permission.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import edu.zsc.ai.agent.tool.sql.approval.WriteExecutionApprovalStore;
import edu.zsc.ai.common.enums.permission.CatalogMatchMode;
import edu.zsc.ai.common.enums.permission.PermissionGrantPreset;
import edu.zsc.ai.common.enums.permission.PermissionScopeType;
import edu.zsc.ai.common.enums.permission.SchemaMatchMode;
import edu.zsc.ai.common.enums.org.WorkspaceTypeEnum;
import edu.zsc.ai.context.RequestContext;
import edu.zsc.ai.domain.exception.BusinessException;
import edu.zsc.ai.domain.mapper.ai.AiPermissionRuleMapper;
import edu.zsc.ai.domain.model.context.DbContext;
import edu.zsc.ai.domain.model.dto.response.db.ConnectionResponse;
import edu.zsc.ai.domain.model.dto.response.permission.PermissionRuleResponse;
import edu.zsc.ai.domain.model.entity.ai.AiPermissionRule;
import edu.zsc.ai.domain.service.ai.AiConversationService;
import edu.zsc.ai.domain.service.db.DbConnectionService;
import edu.zsc.ai.domain.service.permission.PermissionRuleService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PermissionRuleServiceImpl extends ServiceImpl<AiPermissionRuleMapper, AiPermissionRule>
        implements PermissionRuleService {

    private static final String MESSAGE_SCOPE_TYPE_REQUIRED = "scopeType is required when saving a default allow rule";
    private static final String MESSAGE_GRANT_PRESET_REQUIRED_FOR_SAVE = "grantPreset is required when saving a default allow rule";
    private static final String MESSAGE_PERMISSION_NOT_FOUND = "Permission not found";
    private static final String MESSAGE_PERMISSION_ACCESS_DENIED = "Permission access denied";
    private static final String MESSAGE_CONVERSATION_REQUIRED = "conversationId is required for conversation permissions";
    private static final String MESSAGE_USER_CONVERSATION_MUST_BE_NULL = "conversationId must be null for user permissions";
    private static final String MESSAGE_REQUEST_CONTEXT_USER_ID_MISSING = "No userId available in RequestContext";
    private static final String MESSAGE_GRANT_PRESET_REQUIRED = "grantPreset is required";
    private static final String MESSAGE_EXACT_SCHEMA_CATALOG_REQUIRED = "catalogName is required for EXACT_SCHEMA";
    private static final String MESSAGE_EXACT_SCHEMA_SCHEMA_REQUIRED = "schemaName is required for EXACT_SCHEMA";
    private static final String MESSAGE_DATABASE_CATALOG_REQUIRED = "catalogName is required for DATABASE_ALL_SCHEMAS";
    private static final String MESSAGE_DATABASE_SCHEMA_MUST_BE_EMPTY = "schemaName must be empty for DATABASE_ALL_SCHEMAS";
    private static final String MESSAGE_CONNECTION_CATALOG_MUST_BE_EMPTY = "catalogName must be empty for CONNECTION_ALL_DATABASES";
    private static final String MESSAGE_CONNECTION_SCHEMA_MUST_BE_EMPTY = "schemaName must be empty for CONNECTION_ALL_DATABASES";
    private static final String EMPTY_TEXT = "";
    private static final int SPECIFICITY_EXACT_SCHEMA = 3;
    private static final int SPECIFICITY_DATABASE = 2;
    private static final int SPECIFICITY_CONNECTION = 1;

    private final AiConversationService aiConversationService;
    private final DbConnectionService dbConnectionService;
    private final WriteExecutionApprovalStore approvalStore;

    @Override
    public List<PermissionRuleResponse> listForCurrentUser(PermissionScopeType scopeType, Long conversationId) {
        Long userId = currentUserId();
        if (conversationId != null) {
            aiConversationService.checkAccess(userId, conversationId);
        }

        Map<Long, String> connectionNames = dbConnectionService.getAllConnections().stream()
                .collect(Collectors.toMap(ConnectionResponse::getId, ConnectionResponse::getName));

        return currentUserRules(userId).stream()
                .filter(entity -> matchesListScope(entity, scopeType, conversationId))
                .sorted(ruleComparator())
                .map(entity -> toResponse(entity, connectionNames.get(entity.getConnectionId())))
                .toList();
    }

    @Override
    public PermissionRuleResponse upsertForCurrentUser(PermissionScopeType scopeType,
                                                       Long conversationId,
                                                       Long connectionId,
                                                       PermissionGrantPreset grantPreset,
                                                       String catalogName,
                                                       String schemaName,
                                                       boolean enabled) {
        Long userId = currentUserId();
        validateScope(scopeType, conversationId, userId);
        RuleDefinition definition = normalizeDefinition(grantPreset, catalogName, schemaName);
        ConnectionResponse connection = dbConnectionService.getConnectionById(connectionId);

        List<AiPermissionRule> rules = currentUserRules(userId);
        AiPermissionRule existing = findExisting(
                rules,
                scopeType,
                scopeType == PermissionScopeType.CONVERSATION ? conversationId : null,
                connectionId,
                definition.catalogName(),
                definition.catalogMatchMode(),
                definition.schemaName(),
                definition.schemaMatchMode()
        );
        LocalDateTime now = LocalDateTime.now();
        if (existing == null) {
            AiPermissionRule entity = new AiPermissionRule();
            fillWorkspaceFields(entity);
            entity.setScopeType(scopeType);
            entity.setUserId(userId);
            entity.setConversationId(scopeType == PermissionScopeType.CONVERSATION ? conversationId : null);
            entity.setConnectionId(connectionId);
            entity.setCatalogName(definition.catalogName());
            entity.setCatalogMatchMode(definition.catalogMatchMode());
            entity.setSchemaName(definition.schemaName());
            entity.setSchemaMatchMode(definition.schemaMatchMode());
            entity.setEnabled(enabled);
            entity.setCreatedAt(now);
            entity.setUpdatedAt(now);
            save(entity);
            return toResponse(entity, connection.getName());
        }

        existing.setEnabled(enabled);
        existing.setUpdatedAt(now);
        updateById(existing);
        return toResponse(existing, connection.getName());
    }

    @Override
    public void toggleForCurrentUser(Long id, boolean enabled) {
        AiPermissionRule entity = getOwnedById(id);
        entity.setEnabled(enabled);
        entity.setUpdatedAt(LocalDateTime.now());
        updateById(entity);
    }

    @Override
    public void deleteForCurrentUser(Long id) {
        removeById(getOwnedById(id).getId());
    }

    @Override
    public boolean matchesEnabledRule(Long connectionId, String catalogName, String schemaName) {
        Long userId = RequestContext.getUserId();
        Long conversationId = RequestContext.getConversationId();
        if (userId == null) {
            return false;
        }

        List<AiPermissionRule> rules = currentUserRules(userId);
        AiPermissionRule matchedRule = findBestMatchingRule(
                rules,
                conversationId,
                connectionId,
                catalogName,
                schemaName
        );
        return matchedRule != null && Boolean.TRUE.equals(matchedRule.getEnabled());
    }

    @Override
    public void approveWriteExecution(Long conversationId,
                                      Long connectionId,
                                      String catalogName,
                                      String schemaName,
                                      String sql,
                                      PermissionScopeType scopeType,
                                      PermissionGrantPreset grantPreset) {
        Long userId = currentUserId();
        aiConversationService.checkAccess(userId, conversationId);
        dbConnectionService.getConnectionById(connectionId);

        if (scopeType != null || grantPreset != null) {
            BusinessException.assertNotNull(scopeType, MESSAGE_SCOPE_TYPE_REQUIRED);
            BusinessException.assertNotNull(grantPreset, MESSAGE_GRANT_PRESET_REQUIRED_FOR_SAVE);
            RuleDefinition definition = normalizeDefinition(grantPreset, catalogName, schemaName);
            upsertForCurrentUser(
                    scopeType,
                    scopeType == PermissionScopeType.CONVERSATION ? conversationId : null,
                    connectionId,
                    grantPreset,
                    definition.catalogName(),
                    definition.schemaName(),
                    true
            );
        }

        approvalStore.approve(userId, conversationId, new DbContext(connectionId, catalogName, schemaName), sql);
    }

    private AiPermissionRule getOwnedById(Long id) {
        Long userId = currentUserId();
        AiPermissionRule entity = getById(id);
        BusinessException.assertNotNull(entity, MESSAGE_PERMISSION_NOT_FOUND);
        BusinessException.assertTrue(Objects.equals(entity.getUserId(), userId), MESSAGE_PERMISSION_ACCESS_DENIED);
        BusinessException.assertTrue(matchesRequestWorkspace(entity), MESSAGE_PERMISSION_ACCESS_DENIED);
        return entity;
    }

    private List<AiPermissionRule> currentUserRules(Long userId) {
        return list().stream()
                .filter(entity -> Objects.equals(entity.getUserId(), userId))
                .filter(this::matchesRequestWorkspace)
                .toList();
    }

    private boolean matchesRequestWorkspace(AiPermissionRule entity) {
        WorkspaceTypeEnum ctx = RequestContext.getWorkspaceTypeOrPersonal();
        Long ctxOrgId = RequestContext.getOrgId();
        WorkspaceTypeEnum ew = entity.getWorkspaceType();
        if (ew == null) {
            return ctx == WorkspaceTypeEnum.PERSONAL;
        }
        if (ctx == WorkspaceTypeEnum.PERSONAL) {
            return ew == WorkspaceTypeEnum.PERSONAL && entity.getOrgId() == null;
        }
        return ew == WorkspaceTypeEnum.ORGANIZATION && Objects.equals(entity.getOrgId(), ctxOrgId);
    }

    private WorkspaceTypeEnum resolvedWorkspaceType(AiPermissionRule entity) {
        return entity.getWorkspaceType() != null ? entity.getWorkspaceType() : WorkspaceTypeEnum.PERSONAL;
    }

    private void fillWorkspaceFields(AiPermissionRule entity) {
        WorkspaceTypeEnum ws = RequestContext.getWorkspaceTypeOrPersonal();
        entity.setWorkspaceType(ws);
        if (ws == WorkspaceTypeEnum.PERSONAL) {
            entity.setOrgId(null);
            return;
        }
        Long orgId = RequestContext.getOrgId();
        if (orgId == null) {
            throw new IllegalStateException("ORGANIZATION workspace requires orgId in RequestContext");
        }
        entity.setOrgId(orgId);
    }

    private boolean matchesListScope(AiPermissionRule entity, PermissionScopeType scopeType, Long conversationId) {
        if (scopeType == PermissionScopeType.USER) {
            return entity.getScopeType() == PermissionScopeType.USER;
        }
        if (scopeType == PermissionScopeType.CONVERSATION) {
            return entity.getScopeType() == PermissionScopeType.CONVERSATION
                    && (conversationId == null || Objects.equals(entity.getConversationId(), conversationId));
        }
        return entity.getScopeType() == PermissionScopeType.USER
                || (conversationId != null
                && entity.getScopeType() == PermissionScopeType.CONVERSATION
                && Objects.equals(entity.getConversationId(), conversationId));
    }

    private AiPermissionRule findExisting(List<AiPermissionRule> rules,
                                          PermissionScopeType scopeType,
                                          Long conversationId,
                                          Long connectionId,
                                          String catalogName,
                                          CatalogMatchMode catalogMatchMode,
                                          String schemaName,
                                          SchemaMatchMode schemaMatchMode) {
        return rules.stream()
                .filter(entity -> entity.getScopeType() == scopeType)
                .filter(entity -> Objects.equals(entity.getConversationId(), conversationId))
                .filter(entity -> Objects.equals(entity.getConnectionId(), connectionId))
                .filter(entity -> Objects.equals(entity.getCatalogName(), catalogName))
                .filter(entity -> entity.getCatalogMatchMode() == catalogMatchMode)
                .filter(entity -> Objects.equals(entity.getSchemaName(), schemaName))
                .filter(entity -> entity.getSchemaMatchMode() == schemaMatchMode)
                .findFirst()
                .orElse(null);
    }

    private AiPermissionRule findBestMatchingRule(List<AiPermissionRule> rules,
                                                  Long conversationId,
                                                  Long connectionId,
                                                  String catalogName,
                                                  String schemaName) {
        return rules.stream()
                .filter(entity -> matchesAnyScopeRule(entity, conversationId, connectionId, catalogName, schemaName))
                .sorted(matchedRuleComparator())
                .findFirst()
                .orElse(null);
    }

    private void validateScope(PermissionScopeType scopeType, Long conversationId, Long userId) {
        if (scopeType == PermissionScopeType.CONVERSATION) {
            BusinessException.assertNotNull(conversationId, MESSAGE_CONVERSATION_REQUIRED);
            aiConversationService.checkAccess(userId, conversationId);
            return;
        }
        BusinessException.assertTrue(conversationId == null, MESSAGE_USER_CONVERSATION_MUST_BE_NULL);
    }

    private Long currentUserId() {
        Long userId = RequestContext.getUserId();
        if (userId == null) {
            throw new IllegalStateException(MESSAGE_REQUEST_CONTEXT_USER_ID_MISSING);
        }
        return userId;
    }

    private PermissionRuleResponse toResponse(AiPermissionRule entity, String connectionName) {
        return PermissionRuleResponse.builder()
                .id(entity.getId())
                .scopeType(entity.getScopeType())
                .workspaceType(resolvedWorkspaceType(entity))
                .orgId(entity.getOrgId())
                .conversationId(entity.getConversationId())
                .connectionId(entity.getConnectionId())
                .connectionName(connectionName)
                .grantPreset(toGrantPreset(entity))
                .catalogName(entity.getCatalogName())
                .schemaName(entity.getSchemaName())
                .enabled(entity.getEnabled())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    private Comparator<AiPermissionRule> ruleComparator() {
        return Comparator
                .comparing(AiPermissionRule::getConnectionId)
                .thenComparing(Comparator.comparingInt((AiPermissionRule entity) -> specificityRank(toGrantPreset(entity))).reversed())
                .thenComparingInt(this::scopePriority)
                .thenComparing(entity -> Objects.toString(entity.getCatalogName(), EMPTY_TEXT))
                .thenComparing(entity -> Objects.toString(entity.getSchemaName(), EMPTY_TEXT));
    }

    private Comparator<AiPermissionRule> matchedRuleComparator() {
        return Comparator
                .comparingInt((AiPermissionRule entity) -> specificityRank(toGrantPreset(entity)))
                .reversed()
                .thenComparingInt(this::scopePriority);
    }

    private int scopePriority(AiPermissionRule entity) {
        return entity.getScopeType() == PermissionScopeType.CONVERSATION ? 0 : 1;
    }

    private boolean matchesAnyScopeRule(AiPermissionRule entity,
                                        Long conversationId,
                                        Long connectionId,
                                        String catalogName,
                                        String schemaName) {
        if (entity.getScopeType() == PermissionScopeType.CONVERSATION) {
            return matchesRule(entity, PermissionScopeType.CONVERSATION, conversationId, connectionId, catalogName, schemaName);
        }
        if (entity.getScopeType() == PermissionScopeType.USER) {
            return matchesRule(entity, PermissionScopeType.USER, null, connectionId, catalogName, schemaName);
        }
        return false;
    }

    private boolean matchesRule(AiPermissionRule entity,
                                PermissionScopeType scopeType,
                                Long conversationId,
                                Long connectionId,
                                String catalogName,
                                String schemaName) {
        if (entity.getScopeType() != scopeType) {
            return false;
        }
        if (!Objects.equals(entity.getConnectionId(), connectionId)) {
            return false;
        }
        if (scopeType == PermissionScopeType.CONVERSATION && !Objects.equals(entity.getConversationId(), conversationId)) {
            return false;
        }
        if (scopeType == PermissionScopeType.USER && entity.getConversationId() != null) {
            return false;
        }
        if (!matchesCatalog(entity, catalogName)) {
            return false;
        }
        return matchesSchema(entity, schemaName);
    }

    private boolean matchesCatalog(AiPermissionRule entity, String catalogName) {
        if (entity.getCatalogMatchMode() == CatalogMatchMode.ANY) {
            return true;
        }
        return Objects.equals(entity.getCatalogName(), catalogName);
    }

    private boolean matchesSchema(AiPermissionRule entity, String schemaName) {
        if (entity.getSchemaMatchMode() == SchemaMatchMode.ANY) {
            return true;
        }
        return Objects.equals(entity.getSchemaName(), schemaName);
    }

    private RuleDefinition normalizeDefinition(PermissionGrantPreset grantPreset, String catalogName, String schemaName) {
        BusinessException.assertNotNull(grantPreset, MESSAGE_GRANT_PRESET_REQUIRED);
        return switch (grantPreset) {
            case EXACT_SCHEMA -> {
                BusinessException.assertTrue(StringUtils.isNotBlank(catalogName),
                        MESSAGE_EXACT_SCHEMA_CATALOG_REQUIRED);
                BusinessException.assertTrue(StringUtils.isNotBlank(schemaName),
                        MESSAGE_EXACT_SCHEMA_SCHEMA_REQUIRED);
                yield new RuleDefinition(
                        catalogName,
                        CatalogMatchMode.EXACT,
                        schemaName,
                        SchemaMatchMode.EXACT
                );
            }
            case DATABASE_ALL_SCHEMAS -> {
                BusinessException.assertTrue(StringUtils.isNotBlank(catalogName),
                        MESSAGE_DATABASE_CATALOG_REQUIRED);
                BusinessException.assertTrue(StringUtils.isBlank(schemaName),
                        MESSAGE_DATABASE_SCHEMA_MUST_BE_EMPTY);
                yield new RuleDefinition(
                        catalogName,
                        CatalogMatchMode.EXACT,
                        null,
                        SchemaMatchMode.ANY
                );
            }
            case CONNECTION_ALL_DATABASES -> {
                BusinessException.assertTrue(StringUtils.isBlank(catalogName),
                        MESSAGE_CONNECTION_CATALOG_MUST_BE_EMPTY);
                BusinessException.assertTrue(StringUtils.isBlank(schemaName),
                        MESSAGE_CONNECTION_SCHEMA_MUST_BE_EMPTY);
                yield new RuleDefinition(
                        null,
                        CatalogMatchMode.ANY,
                        null,
                        SchemaMatchMode.ANY
                );
            }
        };
    }

    private PermissionGrantPreset toGrantPreset(AiPermissionRule entity) {
        if (entity.getCatalogMatchMode() == CatalogMatchMode.ANY) {
            return PermissionGrantPreset.CONNECTION_ALL_DATABASES;
        }
        if (entity.getSchemaMatchMode() == SchemaMatchMode.ANY) {
            return PermissionGrantPreset.DATABASE_ALL_SCHEMAS;
        }
        return PermissionGrantPreset.EXACT_SCHEMA;
    }

    private int specificityRank(PermissionGrantPreset grantPreset) {
        return switch (grantPreset) {
            case EXACT_SCHEMA -> SPECIFICITY_EXACT_SCHEMA;
            case DATABASE_ALL_SCHEMAS -> SPECIFICITY_DATABASE;
            case CONNECTION_ALL_DATABASES -> SPECIFICITY_CONNECTION;
        };
    }

    private record RuleDefinition(String catalogName,
                                  CatalogMatchMode catalogMatchMode,
                                  String schemaName,
                                  SchemaMatchMode schemaMatchMode) {
    }
}
