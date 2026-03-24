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

@Component
public class UserMentionPromptStrategy extends AbstractUserPromptHandler {

    @Override
    protected UserPromptSection targetSection() {
        return UserPromptSection.EXPLICIT_REFERENCES;
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
            return UserPromptBlockSupport.renderBlock(
                    context,
                    "本轮用户显式引用：",
                    "The user explicitly referenced:",
                    List.of(PromptConstant.NONE));
        }

        return UserPromptBlockSupport.renderBlock(
                context,
                "本轮用户显式引用：",
                "The user explicitly referenced:",
                mentions.stream()
                .map(this::toPayload)
                .map(this::toLine)
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

    private String toLine(Map<String, Object> payload) {
        return "token: " + StringUtils.defaultString((String) payload.get("token"))
                + "; object_type: " + StringUtils.defaultString((String) payload.get("objectType"))
                + "; connection: " + StringUtils.defaultString((String) payload.get("connectionName"))
                + " (id=" + payload.get("connectionId") + ")"
                + "; catalog: " + StringUtils.defaultString((String) payload.get("catalogName"))
                + "; schema: " + StringUtils.defaultString((String) payload.get("schemaName"))
                + "; object: " + StringUtils.defaultString((String) payload.get("objectName"));
    }
}
