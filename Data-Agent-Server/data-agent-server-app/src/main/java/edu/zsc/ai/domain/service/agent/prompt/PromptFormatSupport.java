package edu.zsc.ai.domain.service.agent.prompt;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import edu.zsc.ai.common.constant.PromptConstant;

public final class PromptFormatSupport {

    private PromptFormatSupport() {
    }

    public static boolean isChinese(String language) {
        return StringUtils.defaultString(language).trim().toLowerCase(Locale.ROOT).startsWith("zh");
    }

    public static String title(String language, String zhTitle, String enTitle) {
        return isChinese(language) ? zhTitle : enTitle;
    }

    public static String renderBlock(String language, String zhTitle, String enTitle, List<String> lines) {
        return title(language, zhTitle, enTitle) + "\n" + renderBullets(lines);
    }

    public static String renderBullets(List<String> lines) {
        if (lines == null || lines.isEmpty()) {
            return "- " + PromptConstant.NONE;
        }
        return lines.stream()
                .map(line -> "- " + PromptTextUtil.escape(StringUtils.defaultString(line)))
                .collect(Collectors.joining("\n"));
    }
}
