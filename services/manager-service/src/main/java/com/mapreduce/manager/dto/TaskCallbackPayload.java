package com.mapreduce.manager.dto;

import java.util.List;

public class TaskCallbackPayload {
    private String taskId;
    private String jobId;
    private String status;
    private String workerId;
    private String details;
    private List<String> outputObjectKeys;

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getWorkerId() {
        return workerId;
    }

    public void setWorkerId(String workerId) {
        this.workerId = workerId;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public List<String> getOutputObjectKeys() {
        return outputObjectKeys;
    }

    public void setOutputObjectKeys(List<String> outputObjectKeys) {
        this.outputObjectKeys = outputObjectKeys;
    }



}
