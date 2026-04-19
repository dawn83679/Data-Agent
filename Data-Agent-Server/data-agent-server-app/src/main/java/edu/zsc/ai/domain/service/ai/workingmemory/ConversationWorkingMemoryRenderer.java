package edu.zsc.ai.domain.service.ai.workingmemory;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import edu.zsc.ai.domain.service.ai.workingmemory.ConversationWorkingMemoryDraft.ActiveScope;
import edu.zsc.ai.domain.service.ai.workingmemory.ConversationWorkingMemoryDraft.CurrentTask;
import edu.zsc.ai.domain.service.ai.workingmemory.ConversationWorkingMemoryDraft.DecisionPriority;
import edu.zsc.ai.domain.service.ai.workingmemory.ConversationWorkingMemoryDraft.HighPriorityCandidate;
import edu.zsc.ai.domain.service.ai.workingmemory.ConversationWorkingMemoryDraft.OpenQuestion;
import edu.zsc.ai.domain.service.ai.workingmemory.ConversationWorkingMemoryDraft.ResolvedMilestone;
import edu.zsc.ai.domain.service.ai.workingmemory.ConversationWorkingMemoryDraft.UserConfirmedFact;
import edu.zsc.ai.domain.service.ai.workingmemory.ConversationWorkingMemoryDraft.VerifiedFinding;

@Component
public class ConversationWorkingMemoryRenderer {

    public String render(ConversationWorkingMemoryDraft draft) {
        StringBuilder builder = new StringBuilder();
        renderCurrentTask(builder, draft.getCurrentTask());
        renderActiveScope(builder, draft.getActiveScope());
        renderResolvedMilestones(builder, draft.getResolvedMilestones());
        renderHighPriorityCandidates(builder, draft.getHighPriorityCandidates());
        renderUserConfirmedFacts(builder, draft.getUserConfirmedFacts());
        renderVerifiedFindings(builder, draft.getVerifiedFindings());
        renderDecisionPriorities(builder, draft.getDecisionPriorities());
        renderOpenQuestions(builder, draft.getOpenQuestions());
        return builder.toString().stripTrailing();
    }

    private void renderCurrentTask(StringBuilder builder, CurrentTask currentTask) {
        builder.append("# 当前任务\n");
        builder.append("- 目标：").append(currentTask.getGoal()).append('\n');
        builder.append("- 状态：").append(currentTask.getStatus()).append('\n');
        builder.append("- 摘要：").append(currentTask.getSummary()).append("\n\n");
    }

    private void renderActiveScope(StringBuilder builder, ActiveScope activeScope) {
        builder.append("## 当前作用域\n");
        builder.append("- 连接：").append(orNone(activeScope.getConnection())).append('\n');
        builder.append("- 数据库：").append(orNone(activeScope.getDatabase())).append('\n');
        builder.append("- Schema：").append(orNone(activeScope.getSchema())).append('\n');
        builder.append("- 主要对象：").append(renderInlineList(activeScope.getPrimaryObjects())).append('\n');
        builder.append("- 作用域置信度：").append(activeScope.getScopeConfidence()).append("\n\n");
    }

    private void renderResolvedMilestones(StringBuilder builder, List<ResolvedMilestone> items) {
        builder.append("## 已解决里程碑\n");
        if (items.isEmpty()) {
            builder.append("- 无\n\n");
            return;
        }
        for (ResolvedMilestone item : items) {
            builder.append("- [").append(item.getPriority()).append("] ").append(item.getResolvedItem()).append('\n');
            builder.append("  解决方式：").append(item.getResolution()).append('\n');
            builder.append("  持续重要性：").append(item.getWhyItStillMatters()).append('\n');
        }
        builder.append('\n');
    }

    private void renderHighPriorityCandidates(StringBuilder builder, List<HighPriorityCandidate> items) {
        builder.append("## 高优先级候选\n");
        if (items.isEmpty()) {
            builder.append("- 无\n\n");
            return;
        }
        for (HighPriorityCandidate item : items) {
            builder.append("- [").append(item.getPriority()).append("] ").append(item.getCandidate()).append('\n');
            builder.append("  类型：").append(item.getCandidateType()).append('\n');
            builder.append("  作用域：").append(item.getScopeRef()).append('\n');
            builder.append("  相关原因：").append(item.getWhyRelevant()).append('\n');
            builder.append("  未确认原因：").append(item.getWhyNotConfirmed()).append('\n');
        }
        builder.append('\n');
    }

    private void renderUserConfirmedFacts(StringBuilder builder, List<UserConfirmedFact> items) {
        builder.append("## 用户已确认事实\n");
        if (items.isEmpty()) {
            builder.append("- 无\n\n");
            return;
        }
        for (UserConfirmedFact item : items) {
            builder.append("- [").append(item.getPriority()).append("] ").append(item.getFact()).append('\n');
            builder.append("  作用域：").append(item.getScopeRef()).append('\n');
            builder.append("  用户确认依据：").append(item.getConfirmedByUser()).append('\n');
        }
        builder.append('\n');
    }

    private void renderVerifiedFindings(StringBuilder builder, List<VerifiedFinding> items) {
        builder.append("## 已验证结论\n");
        if (items.isEmpty()) {
            builder.append("- 无\n\n");
            return;
        }
        for (VerifiedFinding item : items) {
            builder.append("- [").append(item.getPriority()).append("] ").append(item.getFinding()).append('\n');
            builder.append("  精确值：").append(item.getExactValue()).append('\n');
            builder.append("  作用域：").append(item.getScopeRef()).append('\n');
            builder.append("  验证来源：").append(item.getVerifiedFrom()).append('\n');
        }
        builder.append('\n');
    }

    private void renderDecisionPriorities(StringBuilder builder, List<DecisionPriority> items) {
        builder.append("## 决策优先级\n");
        if (items.isEmpty()) {
            builder.append("- 无\n\n");
            return;
        }
        for (DecisionPriority item : items) {
            builder.append("- [").append(item.getPriority()).append("] ").append(item.getRule()).append('\n');
            builder.append("  适用条件：").append(item.getAppliesWhen()).append('\n');
        }
        builder.append('\n');
    }

    private void renderOpenQuestions(StringBuilder builder, List<OpenQuestion> items) {
        builder.append("## 未决问题 / 待确认范围\n");
        if (items.isEmpty()) {
            builder.append("- 无\n");
            return;
        }
        for (OpenQuestion item : items) {
            builder.append("- [").append(item.getPriority()).append("] ").append(item.getQuestion()).append('\n');
            builder.append("  是否阻塞：").append(Boolean.TRUE.equals(item.getBlocking()) ? "是" : "否").append('\n');
        }
    }

    private String orNone(String value) {
        return StringUtils.defaultIfBlank(value, "无");
    }

    private String renderInlineList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "无";
        }
        List<String> normalized = values.stream()
                .filter(StringUtils::isNotBlank)
                .toList();
        return normalized.isEmpty() ? "无" : String.join("、", normalized);
    }
}
