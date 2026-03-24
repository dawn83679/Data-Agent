package edu.zsc.ai.domain.service.agent.prompt.strategy;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import edu.zsc.ai.common.constant.PromptConstant;
import edu.zsc.ai.domain.service.agent.prompt.PromptTextUtil;
import edu.zsc.ai.domain.service.agent.prompt.UserPromptAssemblyContext;

final class UserPromptBlockSupport {

    private UserPromptBlockSupport() {
    }

    static boolean isChinese(UserPromptAssemblyContext context) {
        String language = StringUtils.defaultString(context.getLanguage()).trim().toLowerCase(Locale.ROOT);
        return language.startsWith("zh");
    }

    static String title(UserPromptAssemblyContext context, String zhTitle, String enTitle) {
        return isChinese(context) ? zhTitle : enTitle;
    }

    static String renderBlock(UserPromptAssemblyContext context, String zhTitle, String enTitle, List<String> lines) {
        return title(context, zhTitle, enTitle) + "\n" + renderBullets(lines);
    }

    static String renderBullets(List<String> lines) {
        if (lines == null || lines.isEmpty()) {
            return "- " + PromptConstant.NONE;
        }
        return lines.stream()
                .map(line -> "- " + PromptTextUtil.escape(StringUtils.defaultString(line)))
                .collect(Collectors.joining("\n"));
    }
}
