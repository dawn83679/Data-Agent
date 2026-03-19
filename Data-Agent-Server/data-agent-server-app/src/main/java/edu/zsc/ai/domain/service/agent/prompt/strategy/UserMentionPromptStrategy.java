package edu.zsc.ai.domain.service.agent.prompt.strategy;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import edu.zsc.ai.api.model.request.ChatUserMention;
import edu.zsc.ai.common.constant.PromptConstant;
import edu.zsc.ai.domain.service.agent.prompt.AbstractUserPromptHandler;
import edu.zsc.ai.domain.service.agent.prompt.PromptTextUtil;
import edu.zsc.ai.domain.service.agent.prompt.UserPromptAssemblyContext;
import edu.zsc.ai.domain.service.agent.prompt.UserPromptSection;

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

        return mentions.stream()
                .map(this::formatMention)
                .collect(Collectors.joining("\n"));
    }

    private String formatMention(ChatUserMention mention) {
        String scope = Stream.of(
                        mention.getConnectionName(),
                        mention.getCatalogName(),
                        mention.getSchemaName(),
                        mention.getObjectName())
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.joining("."));
        return PromptTextUtil.escape(mention.getObjectType()) + ":" + PromptTextUtil.escape(scope);
    }
}
