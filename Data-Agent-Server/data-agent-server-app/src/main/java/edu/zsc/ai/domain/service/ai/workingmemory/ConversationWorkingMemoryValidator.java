package edu.zsc.ai.domain.service.ai.workingmemory;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import edu.zsc.ai.domain.exception.BusinessException;
import edu.zsc.ai.domain.service.ai.workingmemory.ConversationWorkingMemoryDraft.ActiveScope;
import edu.zsc.ai.domain.service.ai.workingmemory.ConversationWorkingMemoryDraft.CurrentTask;
import edu.zsc.ai.domain.service.ai.workingmemory.ConversationWorkingMemoryDraft.DecisionPriority;
import edu.zsc.ai.domain.service.ai.workingmemory.ConversationWorkingMemoryDraft.HighPriorityCandidate;
import edu.zsc.ai.domain.service.ai.workingmemory.ConversationWorkingMemoryDraft.OpenQuestion;
import edu.zsc.ai.domain.service.ai.workingmemory.ConversationWorkingMemoryDraft.ResolvedMilestone;
import edu.zsc.ai.domain.service.ai.workingmemory.ConversationWorkingMemoryDraft.UserConfirmedFact;
import edu.zsc.ai.domain.service.ai.workingmemory.ConversationWorkingMemoryDraft.VerifiedFinding;

@Component
public class ConversationWorkingMemoryValidator {

    private static final Set<String> PRIORITIES = Set.of("P0", "P1", "P2");
    private static final Set<String> TASK_STATUS = Set.of("ACTIVE", "DONE", "BLOCKED");
    private static final Set<String> SCOPE_CONFIDENCE = Set.of("CONFIRMED", "WORKING", "AMBIGUOUS");
    private static final Set<String> CANDIDATE_TYPES = Set.of("CONNECTION", "DATABASE", "SCHEMA", "OBJECT", "FIELD_SEMANTICS");
    private static final Set<String> VERIFIED_FROM = Set.of("SQL_RESULT", "TOOL_RESULT", "EXPLICIT_CONTEXT");
    private static final List<String> FORBIDDEN_EXACT_VALUE_TOKENS = List.of("~", "约", "大约", "可能", "左右");
    private static final List<String> LOW_VALUE_PHRASES = List.of("用户满意", "任务已完成可继续分析", "可选后续建议");

    public ConversationWorkingMemoryDraft validateAndNormalize(ConversationWorkingMemoryDraft draft) {
        BusinessException.assertNotNull(draft, "conversation working memory draft is required");
        validateCurrentTask(draft.getCurrentTask());
        validateActiveScope(draft.getActiveScope());

        List<ResolvedMilestone> resolvedMilestones = cropByPriority(
                normalizeList(draft.getResolvedMilestones()),
                ResolvedMilestone::getPriority,
                3);
        resolvedMilestones.forEach(this::validateResolvedMilestone);

        List<HighPriorityCandidate> candidates = cropByPriority(
                normalizeList(draft.getHighPriorityCandidates()),
                HighPriorityCandidate::getPriority,
                3);
        candidates.forEach(this::validateHighPriorityCandidate);

        List<UserConfirmedFact> confirmedFacts = normalizeList(draft.getUserConfirmedFacts());
        confirmedFacts.forEach(this::validateUserConfirmedFact);

        List<VerifiedFinding> findings = normalizeList(draft.getVerifiedFindings());
        findings.forEach(this::validateVerifiedFinding);

        List<DecisionPriority> decisionPriorities = normalizeList(draft.getDecisionPriorities());
        decisionPriorities.forEach(this::validateDecisionPriority);

        List<OpenQuestion> openQuestions = normalizeList(draft.getOpenQuestions());
        openQuestions.forEach(this::validateOpenQuestion);

        return ConversationWorkingMemoryDraft.builder()
                .currentTask(normalizeCurrentTask(draft.getCurrentTask()))
                .activeScope(normalizeActiveScope(draft.getActiveScope()))
                .resolvedMilestones(resolvedMilestones)
                .highPriorityCandidates(candidates)
                .userConfirmedFacts(confirmedFacts)
                .verifiedFindings(findings)
                .decisionPriorities(decisionPriorities)
                .openQuestions(openQuestions)
                .build();
    }

    private void validateCurrentTask(CurrentTask currentTask) {
        BusinessException.assertNotNull(currentTask, "currentTask is required");
        assertHasText(currentTask.getGoal(), "currentTask.goal is required");
        assertAllowed(normalizeEnum(currentTask.getStatus()), TASK_STATUS, "currentTask.status");
        assertHasText(currentTask.getSummary(), "currentTask.summary is required");
        assertNoLowValuePhrase(currentTask.getGoal(), "currentTask.goal");
        assertNoLowValuePhrase(currentTask.getSummary(), "currentTask.summary");
    }

    private void validateActiveScope(ActiveScope activeScope) {
        BusinessException.assertNotNull(activeScope, "activeScope is required");
        assertAllowed(normalizeEnum(activeScope.getScopeConfidence()), SCOPE_CONFIDENCE, "activeScope.scopeConfidence");
        assertNoLowValuePhrase(activeScope.getConnection(), "activeScope.connection");
        assertNoLowValuePhrase(activeScope.getDatabase(), "activeScope.database");
        assertNoLowValuePhrase(activeScope.getSchema(), "activeScope.schema");
        for (String primaryObject : normalizeList(activeScope.getPrimaryObjects())) {
            assertNoLowValuePhrase(primaryObject, "activeScope.primaryObjects");
        }
    }

    private void validateResolvedMilestone(ResolvedMilestone item) {
        BusinessException.assertNotNull(item, "resolvedMilestones item is required");
        assertAllowed(normalizePriority(item.getPriority()), PRIORITIES, "priority");
        assertHasText(item.getResolvedItem(), "resolvedMilestones.resolvedItem is required");
        assertHasText(item.getResolution(), "resolvedMilestones.resolution is required");
        assertHasText(item.getWhyItStillMatters(), "resolvedMilestones.whyItStillMatters is required");
        assertNoLowValuePhrase(item.getResolvedItem(), "resolvedMilestones.resolvedItem");
        assertNoLowValuePhrase(item.getResolution(), "resolvedMilestones.resolution");
        assertNoLowValuePhrase(item.getWhyItStillMatters(), "resolvedMilestones.whyItStillMatters");
    }

    private void validateHighPriorityCandidate(HighPriorityCandidate item) {
        BusinessException.assertNotNull(item, "highPriorityCandidates item is required");
        assertAllowed(normalizePriority(item.getPriority()), PRIORITIES, "priority");
        assertHasText(item.getCandidate(), "highPriorityCandidates.candidate is required");
        assertAllowed(normalizeEnum(item.getCandidateType()), CANDIDATE_TYPES, "highPriorityCandidates.candidateType");
        assertHasText(item.getScopeRef(), "highPriorityCandidates.scopeRef is required");
        assertHasText(item.getWhyRelevant(), "highPriorityCandidates.whyRelevant is required");
        assertHasText(item.getWhyNotConfirmed(), "highPriorityCandidates.whyNotConfirmed is required");
        assertNoLowValuePhrase(item.getCandidate(), "highPriorityCandidates.candidate");
        assertNoLowValuePhrase(item.getWhyRelevant(), "highPriorityCandidates.whyRelevant");
        assertNoLowValuePhrase(item.getWhyNotConfirmed(), "highPriorityCandidates.whyNotConfirmed");
    }

    private void validateUserConfirmedFact(UserConfirmedFact item) {
        BusinessException.assertNotNull(item, "userConfirmedFacts item is required");
        assertAllowed(normalizePriority(item.getPriority()), PRIORITIES, "priority");
        assertHasText(item.getFact(), "userConfirmedFacts.fact is required");
        assertHasText(item.getScopeRef(), "userConfirmedFacts.scopeRef is required");
        assertHasText(item.getConfirmedByUser(), "userConfirmedFacts.confirmedByUser is required");
        assertNoLowValuePhrase(item.getFact(), "userConfirmedFacts.fact");
        assertNoLowValuePhrase(item.getConfirmedByUser(), "userConfirmedFacts.confirmedByUser");
    }

    private void validateVerifiedFinding(VerifiedFinding item) {
        BusinessException.assertNotNull(item, "verifiedFindings item is required");
        assertAllowed(normalizePriority(item.getPriority()), PRIORITIES, "priority");
        assertHasText(item.getFinding(), "verifiedFindings.finding is required");
        assertHasText(item.getExactValue(), "verifiedFindings.exactValue is required");
        assertHasText(item.getScopeRef(), "verifiedFindings.scopeRef is required");
        assertAllowed(normalizeEnum(item.getVerifiedFrom()), VERIFIED_FROM, "verifiedFindings.verifiedFrom");
        FORBIDDEN_EXACT_VALUE_TOKENS.stream()
                .filter(token -> StringUtils.contains(item.getExactValue(), token))
                .findFirst()
                .ifPresent(token -> {
                    throw BusinessException.badRequest("verifiedFindings.exactValue must use precise values");
                });
        assertNoLowValuePhrase(item.getFinding(), "verifiedFindings.finding");
        assertNoLowValuePhrase(item.getExactValue(), "verifiedFindings.exactValue");
    }

    private void validateDecisionPriority(DecisionPriority item) {
        BusinessException.assertNotNull(item, "decisionPriorities item is required");
        assertAllowed(normalizePriority(item.getPriority()), PRIORITIES, "priority");
        assertHasText(item.getRule(), "decisionPriorities.rule is required");
        assertHasText(item.getAppliesWhen(), "decisionPriorities.appliesWhen is required");
        assertNoLowValuePhrase(item.getRule(), "decisionPriorities.rule");
        assertNoLowValuePhrase(item.getAppliesWhen(), "decisionPriorities.appliesWhen");
    }

    private void validateOpenQuestion(OpenQuestion item) {
        BusinessException.assertNotNull(item, "openQuestions item is required");
        assertAllowed(normalizePriority(item.getPriority()), PRIORITIES, "priority");
        assertHasText(item.getQuestion(), "openQuestions.question is required");
        BusinessException.assertNotNull(item.getBlocking(), "openQuestions.blocking is required");
        assertNoLowValuePhrase(item.getQuestion(), "openQuestions.question");
    }

    private CurrentTask normalizeCurrentTask(CurrentTask currentTask) {
        return CurrentTask.builder()
                .goal(StringUtils.trimToEmpty(currentTask.getGoal()))
                .status(normalizeEnum(currentTask.getStatus()))
                .summary(StringUtils.trimToEmpty(currentTask.getSummary()))
                .build();
    }

    private ActiveScope normalizeActiveScope(ActiveScope activeScope) {
        return ActiveScope.builder()
                .connection(StringUtils.trimToNull(activeScope.getConnection()))
                .database(StringUtils.trimToNull(activeScope.getDatabase()))
                .schema(StringUtils.trimToNull(activeScope.getSchema()))
                .primaryObjects(normalizeList(activeScope.getPrimaryObjects()).stream()
                        .map(StringUtils::trimToEmpty)
                        .filter(StringUtils::isNotBlank)
                        .toList())
                .scopeConfidence(normalizeEnum(activeScope.getScopeConfidence()))
                .build();
    }

    private <T> List<T> normalizeList(List<T> items) {
        return items == null ? List.of() : items.stream().filter(Objects::nonNull).toList();
    }

    private <T> List<T> cropByPriority(List<T> items, Function<T, String> priorityExtractor, int limit) {
        if (items.size() <= limit) {
            return items;
        }
        return items.stream()
                .sorted(Comparator.comparingInt(item -> priorityRank(priorityExtractor.apply(item))))
                .limit(limit)
                .toList();
    }

    private void assertHasText(String value, String message) {
        if (StringUtils.isBlank(value)) {
            throw BusinessException.badRequest(message);
        }
    }

    private void assertAllowed(String value, Set<String> allowed, String fieldName) {
        if (!allowed.contains(value)) {
            if ("priority".equals(fieldName)) {
                throw BusinessException.badRequest("priority must be one of: P0, P1, P2");
            }
            throw BusinessException.badRequest("%s must be one of: %s", fieldName, String.join(", ", allowed));
        }
    }

    private void assertNoLowValuePhrase(String value, String fieldName) {
        if (StringUtils.isBlank(value)) {
            return;
        }
        for (String phrase : LOW_VALUE_PHRASES) {
            if (StringUtils.contains(value, phrase)) {
                throw BusinessException.badRequest("%s contains low-value status text", fieldName);
            }
        }
    }

    private String normalizePriority(String value) {
        return normalizeEnum(value);
    }

    private String normalizeEnum(String value) {
        return StringUtils.trimToEmpty(value).toUpperCase(Locale.ROOT);
    }

    private int priorityRank(String priority) {
        return switch (normalizePriority(priority)) {
            case "P0" -> 0;
            case "P1" -> 1;
            case "P2" -> 2;
            default -> Integer.MAX_VALUE;
        };
    }
}
