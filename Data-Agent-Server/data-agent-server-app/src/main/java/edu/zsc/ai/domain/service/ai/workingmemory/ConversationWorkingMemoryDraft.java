package edu.zsc.ai.domain.service.ai.workingmemory;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationWorkingMemoryDraft {

    private CurrentTask currentTask;

    private ActiveScope activeScope;

    @Builder.Default
    private List<ResolvedMilestone> resolvedMilestones = List.of();

    @Builder.Default
    private List<HighPriorityCandidate> highPriorityCandidates = List.of();

    @Builder.Default
    private List<UserConfirmedFact> userConfirmedFacts = List.of();

    @Builder.Default
    private List<VerifiedFinding> verifiedFindings = List.of();

    @Builder.Default
    private List<DecisionPriority> decisionPriorities = List.of();

    @Builder.Default
    private List<OpenQuestion> openQuestions = List.of();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CurrentTask {
        private String goal;
        private String status;
        private String summary;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActiveScope {
        private String connection;
        private String database;
        private String schema;
        @Builder.Default
        private List<String> primaryObjects = List.of();
        private String scopeConfidence;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResolvedMilestone {
        private String priority;
        private String resolvedItem;
        private String resolution;
        private String whyItStillMatters;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HighPriorityCandidate {
        private String priority;
        private String candidate;
        private String candidateType;
        private String scopeRef;
        private String whyRelevant;
        private String whyNotConfirmed;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserConfirmedFact {
        private String priority;
        private String fact;
        private String scopeRef;
        private String confirmedByUser;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VerifiedFinding {
        private String priority;
        private String finding;
        private String exactValue;
        private String scopeRef;
        private String verifiedFrom;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DecisionPriority {
        private String priority;
        private String rule;
        private String appliesWhen;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OpenQuestion {
        private String priority;
        private String question;
        private Boolean blocking;
    }
}
