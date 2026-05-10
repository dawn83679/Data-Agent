package edu.zsc.ai.agent.tool.plan;

import java.util.List;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.ReturnBehavior;
import dev.langchain4j.agent.tool.Tool;
import edu.zsc.ai.agent.annotation.AgentTool;
import edu.zsc.ai.agent.tool.message.ToolMessageSupport;
import edu.zsc.ai.agent.tool.plan.model.PlanStep;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;

@AgentTool
@Slf4j
public class ExitPlanModeTool {

    @Tool(
            value = {
                    "价值：在规划流程结束时，把最终结构化计划交付给用户。",
                    "使用时机：规划已经完成，且计划已经具体到无需新的设计工作即可执行。",
                    "成功后：等待用户决定，再执行计划或改变范围。",
                    "失败后：继续在计划模式中完善计划，不要带着不完整计划切回执行。",
                    "等待用户：计划展示后，用户回应前不要执行，也不要扩大范围。",
                    "结果消费：工具参数承载计划标题和有序步骤，前端会展示给用户。"
            },
            returnBehavior = ReturnBehavior.IMMEDIATE
    )
    public String exitPlanMode(
            @P("计划标题或摘要") String title,
            @P("计划步骤列表，每项包含 order、description、SQL 和 objectName") List<PlanStep> steps) {

        int stepCount = CollectionUtils.size(steps);
        log.info("[Tool] exitPlanMode, title='{}', steps={}", title, stepCount);

        return ToolMessageSupport.sentence(
                "计划 `" + title + "` 已展示给用户，包含 " + stepCount + " 个步骤。",
                "执行计划或改变范围前，先等待用户决定。"
        );
    }
}
