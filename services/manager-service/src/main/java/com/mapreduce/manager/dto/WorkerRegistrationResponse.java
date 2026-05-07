package com.mapreduce.manager.dto;

import java.time.LocalDateTime;

import com.mapreduce.manager.entity.WorkerStatus;

public class WorkerRegistrationResponse {
    private String workerId;
    private WorkerStatus status;
    private LocalDateTime registeredAt;
    private String message;

    public WorkerRegistrationResponse(String message, LocalDateTime registeredAt, WorkerStatus status, String workerId) {
        this.message = message;
        this.registeredAt = registeredAt;
        this.status = status;
        this.workerId = workerId;
    }

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

    public LocalDateTime getRegisteredAt() {
        return registeredAt;
    }

    public void setRegisteredAt(LocalDateTime registeredAt) {
        this.registeredAt = registeredAt;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

}
