package edu.zsc.ai.agent.subagent.contract;

import org.apache.commons.collections4.CollectionUtils;

import java.util.Comparator;
import java.util.List;

/**
 * Normalizes and sorts explorer object relevance scores.
 */
public final class ExploreObjectScoreSupport {

    public static final int DEFAULT_RELEVANCE_SCORE = 50;
    public static final int MIN_RELEVANCE_SCORE = 0;
    public static final int MAX_RELEVANCE_SCORE = 100;
    public static final int HIGH_RELEVANCE_THRESHOLD = 80;
    public static final int MEDIUM_RELEVANCE_THRESHOLD = 50;

    private static final Comparator<ExploreObject> BY_RELEVANCE_SCORE_DESC =
            Comparator.comparingInt(ExploreObjectScoreSupport::safeScore).reversed();

    private ExploreObjectScoreSupport() {
    }

    public static int normalizeScore(Integer relevanceScore) {
        if (relevanceScore == null) {
            return DEFAULT_RELEVANCE_SCORE;
        }
        return Math.max(MIN_RELEVANCE_SCORE, Math.min(MAX_RELEVANCE_SCORE, relevanceScore));
    }

    public static void normalizeInPlace(ExploreObject object) {
        if (object == null) {
            return;
        }
        object.setRelevanceScore(normalizeScore(object.getRelevanceScore()));
    }

    public static List<ExploreObject> normalizeAndSort(List<ExploreObject> objects) {
        if (CollectionUtils.isEmpty(objects)) {
            return objects;
        }
        objects.forEach(ExploreObjectScoreSupport::normalizeInPlace);
        objects.sort(BY_RELEVANCE_SCORE_DESC);
        return objects;
    }

    private static int safeScore(ExploreObject object) {
        return object == null ? DEFAULT_RELEVANCE_SCORE : normalizeScore(object.getRelevanceScore());
    }
}
