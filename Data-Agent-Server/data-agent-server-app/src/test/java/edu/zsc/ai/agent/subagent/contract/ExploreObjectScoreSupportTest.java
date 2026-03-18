package edu.zsc.ai.agent.subagent.contract;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExploreObjectScoreSupportTest {

    @Test
    void normalizeAndSort_clampsAndOrdersByScoreDescending() {
        List<ExploreObject> objects = new ArrayList<>(List.of(
                ExploreObject.builder().objectName("low").relevanceScore(-1).build(),
                ExploreObject.builder().objectName("default").build(),
                ExploreObject.builder().objectName("high").relevanceScore(999).build()
        ));

        ExploreObjectScoreSupport.normalizeAndSort(objects);

        assertEquals(List.of("high", "default", "low"),
                objects.stream().map(ExploreObject::getObjectName).toList());
        assertEquals(100, objects.get(0).getRelevanceScore());
        assertEquals(50, objects.get(1).getRelevanceScore());
        assertEquals(0, objects.get(2).getRelevanceScore());
    }
}
