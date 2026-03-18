package edu.zsc.ai.observability;

import java.util.LinkedHashMap;
import java.util.Map;

public final class AgentLogFields {

    private AgentLogFields() {
    }

    public static Map<String, Object> of(Object... keyValues) {
        LinkedHashMap<String, Object> fields = new LinkedHashMap<>();
        if (keyValues == null) {
            return fields;
        }
        for (int index = 0; index + 1 < keyValues.length; index += 2) {
            fields.put(String.valueOf(keyValues[index]), keyValues[index + 1]);
        }
        if (keyValues.length % 2 != 0) {
            fields.put("_fieldsWarning", "odd number of field arguments");
        }
        return fields;
    }
}
