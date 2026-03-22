package edu.zsc.ai.domain.service.agent.prompt.strategy;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import edu.zsc.ai.api.model.request.ChatUserMention;
import edu.zsc.ai.common.constant.PromptConstant;
import edu.zsc.ai.domain.service.agent.prompt.AbstractUserPromptHandler;
import edu.zsc.ai.domain.service.agent.prompt.UserPromptAssemblyContext;
import edu.zsc.ai.domain.service.agent.prompt.UserPromptSection;
import edu.zsc.ai.util.JsonUtil;

@Component
public class UserMentionPromptStrategy extends AbstractUserPromptHandler {

    @Override
    protected UserPromptSection targetSection() {
        return UserPromptSection.USER_MENTION;
    }

    @Override
    protected String buildContent(UserPromptAssemblyContext context) {
        List<ChatUserMention> mentions = context.getUserMentions().stream()
                .filter(Objects::nonNull)
                .filter(mention -> StringUtils.isNotBlank(mention.getObjectType()))
                .filter(mention -> StringUtils.isNotBlank(mention.getConnectionName()))
                .filter(mention -> StringUtils.isNotBlank(mention.getObjectName()))
                .toList();

        if (mentions.isEmpty()) {
            return PromptConstant.NONE;
        }

        return JsonUtil.object2json(mentions.stream()
                .map(this::toPayload)
                .toList());
    }

    private Map<String, Object> toPayload(ChatUserMention mention) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("token", mention.getToken());
        payload.put("objectType", mention.getObjectType());
        payload.put("connectionId", mention.getConnectionId());
        payload.put("connectionName", mention.getConnectionName());
        payload.put("catalogName", mention.getCatalogName());
        payload.put("schemaName", mention.getSchemaName());
        payload.put("objectName", mention.getObjectName());
        return payload;
    }
}
