package edu.zsc.ai.plugin.model.command.view;

import edu.zsc.ai.plugin.model.command.CommandResult;

import java.util.List;

/**
 * View command result.
 * Represents the result of a view operation.
 */
public class ViewCommandResult implements CommandResult {

    /**
     * View name
     */
    private String viewName;

    /**
     * View definition SQL
     */
    private String viewDefinition;

    /**
     * List of views (for LIST operation)
     */
    private List<ViewInfo> viewList;

    /**
     * Operation success flag
     */
    private boolean success;

    /**
     * Result message
     */
    private String message;

    /**
     * Number of affected rows (for CREATE/ALTER/DROP operations)
     */
    private int affectedRows;

    /**
     * Execution time in milliseconds
     */
    private long executionTime;

    /**
     * Default constructor
     */
    public ViewCommandResult() {
    }

    /**
     * Constructor for successful operations
     */
    public ViewCommandResult(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    /**
     * Constructor for view query result
     */
    public ViewCommandResult(String viewName, String viewDefinition, boolean success) {
        this.viewName = viewName;
        this.viewDefinition = viewDefinition;
        this.success = success;
    }

    /**
     * Constructor for view list result
     */
    public ViewCommandResult(List<ViewInfo> viewList, boolean success) {
        this.viewList = viewList;
        this.success = success;
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

    public List<ViewInfo> getViewList() {
        return viewList;
    }

    public void setViewList(List<ViewInfo> viewList) {
        this.viewList = viewList;
    }

    @Override
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getAffectedRows() {
        return affectedRows;
    }

    public void setAffectedRows(int affectedRows) {
        this.affectedRows = affectedRows;
    }

    @Override
    public long getExecutionTime() {
        return executionTime;
    }

    public void setExecutionTime(long executionTime) {
        this.executionTime = executionTime;
    }

    @Override
    public String toString() {
        return "ViewCommandResult{" +
                "viewName='" + viewName + '\'' +
                ", success=" + success +
                ", message='" + message + '\'' +
                ", affectedRows=" + affectedRows +
                ", viewListSize=" + (viewList != null ? viewList.size() : 0) +
                '}';
    }
}