package edu.zsc.ai.plugin.model.sql;

public record SqlError(int line, int column, String message) {}
