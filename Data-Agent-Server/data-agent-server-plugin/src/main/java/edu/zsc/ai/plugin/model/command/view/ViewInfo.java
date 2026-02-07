package edu.zsc.ai.plugin.model.command.view;

import java.time.LocalDateTime;

/**
 * View information entity.
 * Represents metadata and definition of a database view.
 */
public class ViewInfo {

    /**
     * View name
     */
    private String viewName;

    /**
     * View definition SQL
     */
    private String viewDefinition;

    /**
     * Database name
     */
    private String database;

    /**
     * Schema name
     */
    private String schema;

    /**
     * View creation time
     */
    private LocalDateTime createTime;

    /**
     * View last update time
     */
    private LocalDateTime updateTime;

    /**
     * View description
     */
    private String description;

    /**
     * Default constructor
     */
    public ViewInfo() {
    }

    /**
     * Constructor with essential fields
     */
    public ViewInfo(String viewName, String viewDefinition, String database, String schema) {
        this.viewName = viewName;
        this.viewDefinition = viewDefinition;
        this.database = database;
        this.schema = schema;
    }

    // Getters and Setters
    public String getViewName() {
        return viewName;
    }

    public void setViewName(String viewName) {
        this.viewName = viewName;
    }

    public String getViewDefinition() {
        return viewDefinition;
    }

    public void setViewDefinition(String viewDefinition) {
        this.viewDefinition = viewDefinition;
    }

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return "ViewInfo{" +
                "viewName='" + viewName + '\'' +
                ", database='" + database + '\'' +
                ", schema='" + schema + '\'' +
                ", createTime=" + createTime +
                ", updateTime=" + updateTime +
                ", description='" + description + '\'' +
                '}';
    }
}