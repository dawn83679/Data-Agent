package edu.zsc.ai.domain.service.permission;

import com.baomidou.mybatisplus.extension.service.IService;
import edu.zsc.ai.common.enums.permission.PermissionGrantPreset;
import edu.zsc.ai.common.enums.permission.PermissionScopeType;
import edu.zsc.ai.domain.model.dto.response.permission.PermissionRuleResponse;
import edu.zsc.ai.domain.model.entity.ai.AiPermissionRule;

import java.util.List;

public interface PermissionRuleService extends IService<AiPermissionRule> {

    List<PermissionRuleResponse> listForCurrentUser(PermissionScopeType scopeType, Long conversationId);

    PermissionRuleResponse upsertForCurrentUser(PermissionScopeType scopeType,
                                                Long conversationId,
                                                Long connectionId,
                                                PermissionGrantPreset grantPreset,
                                                String catalogName,
                                                String schemaName,
                                                boolean enabled);

    void toggleForCurrentUser(Long id, boolean enabled);

    void deleteForCurrentUser(Long id);

    boolean matchesEnabledRule(Long connectionId, String catalogName, String schemaName);

    void approveWriteExecution(Long conversationId,
                               Long connectionId,
                               String catalogName,
                               String schemaName,
                               String sql,
                               PermissionScopeType scopeType,
                               PermissionGrantPreset grantPreset);
}
