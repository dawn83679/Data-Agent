package edu.zsc.ai.plugin.model.metadata;

import java.util.List;

public record FunctionMetadata(String name, List<ParameterInfo> parameters, String returnType) {

    public FunctionMetadata(String name) {
        this(name, null, null);
    }
}
