package edu.zsc.ai.common.constant;

public final class SkillPromptTagConstant {

    public static final String TAG_PREFIX = "<skill_name=";
    public static final String TAG_CLOSE_PREFIX = "</skill_name=";
    public static final String TAG_SUFFIX = ">";

    private SkillPromptTagConstant() {
    }

    public static String open(String skillName) {
        return TAG_PREFIX + skillName + TAG_SUFFIX;
    }

    public static String close(String skillName) {
        return TAG_CLOSE_PREFIX + skillName + TAG_SUFFIX;
    }
}
