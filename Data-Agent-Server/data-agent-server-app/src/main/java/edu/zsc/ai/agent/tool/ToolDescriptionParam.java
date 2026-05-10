package edu.zsc.ai.agent.tool;

public final class ToolDescriptionParam {

    private ToolDescriptionParam() {
    }

    public static final String UI_STEP_DESCRIPTION =
            "必填。面向用户展示的简短说明，用来说明你调用这个工具要做什么；例如：查询可用数据库、检查订单表结构、执行汇总查询。"
                    + "不要写内部实现细节，不得影响工具执行。";
}
