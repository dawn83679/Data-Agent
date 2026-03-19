package edu.zsc.ai.domain.service.permission.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import edu.zsc.ai.agent.tool.sql.approval.WriteExecutionApprovalStore;
import edu.zsc.ai.common.enums.permission.CatalogMatchMode;
import edu.zsc.ai.common.enums.permission.PermissionGrantPreset;
import edu.zsc.ai.common.enums.permission.PermissionScopeType;
import edu.zsc.ai.common.enums.permission.SchemaMatchMode;
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

    private final AiConversationService aiConversationService;
    private final DbConnectionService dbConnectionService;
    private final WriteExecutionApprovalStore approvalStore;

    @Override
    public List<PermissionRuleResponse> listForCurrentUser(Long conversationId) {
        Long userId = currentUserId();
        if (conversationId != null) {
            aiConversationService.checkAccess(userId, conversationId);
        }

        Map<Long, String> connectionNames = dbConnectionService.getAllConnections().stream()
                .collect(Collectors.toMap(ConnectionResponse::getId, ConnectionResponse::getName));

        return currentUserRules(userId).stream()
                .filter(entity -> entity.getScopeType() == PermissionScopeType.USER
                        || (conversationId != null
                        && entity.getScopeType() == PermissionScopeType.CONVERSATION
                        && Objects.equals(entity.getConversationId(), conversationId)))
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
            BusinessException.assertNotNull(scopeType, "scopeType is required when saving a default allow rule");
            BusinessException.assertNotNull(grantPreset, "grantPreset is required when saving a default allow rule");
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
        BusinessException.assertNotNull(entity, "Permission not found");
        BusinessException.assertTrue(Objects.equals(entity.getUserId(), userId), "Permission access denied");
        return entity;
    }

    private List<AiPermissionRule> currentUserRules(Long userId) {
        return list().stream()
                .filter(entity -> Objects.equals(entity.getUserId(), userId))
                .toList();
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
            BusinessException.assertNotNull(conversationId, "conversationId is required for conversation permissions");
            aiConversationService.checkAccess(userId, conversationId);
            return;
        }
        BusinessException.assertTrue(conversationId == null, "conversationId must be null for user permissions");
    }

    private Long currentUserId() {
        Long userId = RequestContext.getUserId();
        if (userId == null) {
            throw new IllegalStateException("No userId available in RequestContext");
        }
        return userId;
    }

    private PermissionRuleResponse toResponse(AiPermissionRule entity, String connectionName) {
        return PermissionRuleResponse.builder()
                .id(entity.getId())
                .scopeType(entity.getScopeType())
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
                .thenComparing(entity -> Objects.toString(entity.getCatalogName(), ""))
                .thenComparing(entity -> Objects.toString(entity.getSchemaName(), ""));
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
        BusinessException.assertNotNull(grantPreset, "grantPreset is required");
        return switch (grantPreset) {
            case EXACT_SCHEMA -> {
                BusinessException.assertTrue(StringUtils.isNotBlank(catalogName),
                        "catalogName is required for EXACT_SCHEMA");
                BusinessException.assertTrue(StringUtils.isNotBlank(schemaName),
                        "schemaName is required for EXACT_SCHEMA");
                yield new RuleDefinition(
                        catalogName,
                        CatalogMatchMode.EXACT,
                        schemaName,
                        SchemaMatchMode.EXACT
                );
            }
            case DATABASE_ALL_SCHEMAS -> {
                BusinessException.assertTrue(StringUtils.isNotBlank(catalogName),
                        "catalogName is required for DATABASE_ALL_SCHEMAS");
                BusinessException.assertTrue(StringUtils.isBlank(schemaName),
                        "schemaName must be empty for DATABASE_ALL_SCHEMAS");
                yield new RuleDefinition(
                        catalogName,
                        CatalogMatchMode.EXACT,
                        null,
                        SchemaMatchMode.ANY
                );
            }
            case CONNECTION_ALL_DATABASES -> {
                BusinessException.assertTrue(StringUtils.isBlank(catalogName),
                        "catalogName must be empty for CONNECTION_ALL_DATABASES");
                BusinessException.assertTrue(StringUtils.isBlank(schemaName),
                        "schemaName must be empty for CONNECTION_ALL_DATABASES");
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
            case EXACT_SCHEMA -> 3;
            case DATABASE_ALL_SCHEMAS -> 2;
            case CONNECTION_ALL_DATABASES -> 1;
        };
    }

    private record RuleDefinition(String catalogName,
                                  CatalogMatchMode catalogMatchMode,
                                  String schemaName,
                                  SchemaMatchMode schemaMatchMode) {
    }
}
