package edu.zsc.ai.plugin.model.metadata;

import java.util.List;

public record ProcedureMetadata(String name, List<ParameterInfo> parameters) {

    public ProcedureMetadata(String name) {
        this(name, null);
    }
}
