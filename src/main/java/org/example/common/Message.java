package org.example.common;

import java.io.Serializable;
import java.util.Date;

public class Message implements Serializable {

    private static final long serialVersionUID = 1L;

    /* =========================
       Message type
       ========================= */
    public enum Type {
        LIST,
        FILTER,
        CREATE,
        DELETE,
        UPLOAD_FILE,
        DOWNLOAD_FILE,
        RESPONSE,
        ERROR,
        CLOSE
    }

    private Type type;

    /* =========================
       Optional data fields
       ========================= */

    // Task-related
    private Task task;
    private Integer taskId;

    // Filters
    private Boolean completed;
    private Integer priority;
    private Date maxDueDate;

    // File transfer
    private String fileName;
    private byte[] dataBlock;
    private boolean lastBlock;

    // Responses
    private Object result;
    private String errorMessage;

    /* =========================
       Constructors
       ========================= */

    public Message(Type type) {
        this.type = type;
    }

    /* =========================
       Getters and setters
       ========================= */

    public Type getType() {
        return type;
    }

    public Task getTask() {
        return task;
    }

    public void setTask(Task task) {
        this.task = task;
    }

    public Integer getTaskId() {
        return taskId;
    }

    public void setTaskId(Integer taskId) {
        this.taskId = taskId;
    }

    public Boolean isCompleted() {
        return completed;
    }

    public void setCompleted(Boolean completed) {
        this.completed = completed;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public Date getMaxDueDate() {
        return maxDueDate;
    }

    public void setMaxDueDate(Date maxDueDate) {
        this.maxDueDate = maxDueDate;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public byte[] getDataBlock() {
        return dataBlock;
    }

    public void setDataBlock(byte[] dataBlock) {
        this.dataBlock = dataBlock;
    }

    public boolean isLastBlock() {
        return lastBlock;
    }

    public void setLastBlock(boolean lastBlock) {
        this.lastBlock = lastBlock;
    }

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
