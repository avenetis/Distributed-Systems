package com.mapreduce.manager.dto;

import com.mapreduce.manager.entity.WorkerStatus;

public class HeartbeatRequest {
    private String workerId;
    private WorkerStatus status;
    private String currentTaskId;

    public String getWorkerId() {
        return workerId;
    }

    public void setWorkerId(String workerId) {
        this.workerId = workerId;
    }

    public WorkerStatus getStatus() {
        return status;
    }

    public void setStatus(WorkerStatus status) {
        this.status = status;
    }

    public String getCurrentTaskId() {
        return currentTaskId;
    }

    public void setCurrentTaskId(String currentTaskId) {
        this.currentTaskId = currentTaskId;
    }

}
