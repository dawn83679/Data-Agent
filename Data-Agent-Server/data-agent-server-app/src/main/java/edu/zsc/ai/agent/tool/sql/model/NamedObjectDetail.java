package edu.zsc.ai.agent.tool.sql.model;

public record NamedObjectDetail(String objectName, String objectType, boolean success,
                                String error, ObjectDetail detail) {}
