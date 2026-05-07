package com.mapreduce.manager.dto;

import java.time.LocalDateTime;

import com.mapreduce.manager.entity.JobStatus;

public class JobResponse {
    private String id;
    private String name;
    private String userId;
    private JobStatus status;
    private String inputPath;
    private String outputPath;
    private Integer numMappers;
    private Integer numReducers;
    private Integer completedMappers;
    private Integer completedReducers;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private Double progress;

    public JobResponse() {}

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public JobStatus getStatus() {
        return status;
    }

    public void setStatus(JobStatus status) {
        this.status = status;
    }

    public String getInputPath() {
        return inputPath;
    }

    public void setInputPath(String inputPath) {
        this.inputPath = inputPath;
    }

    public String getOutputPath() {
        return outputPath;
    }

    public void setOutputPath(String outputPath) {
        this.outputPath = outputPath;
    }

    public Integer getNumMappers() {
        return numMappers;
    }

    public void setNumMappers(Integer numMappers) {
        this.numMappers = numMappers;
    }

    public Integer getNumReducers() {
        return numReducers;
    }

    public void setNumReducers(Integer numReducers) {
        this.numReducers = numReducers;
    }

    public Integer getCompletedMappers() {
        return completedMappers;
    }

    public void setCompletedMappers(Integer completedMappers) {
        this.completedMappers = completedMappers;
    }

    public Integer getCompletedReducers() {
        return completedReducers;
    }

    public void setCompletedReducers(Integer completedReducers) {
        this.completedReducers = completedReducers;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public Double getProgress() {
        return progress;
    }

    public void setProgress(Double progress) {
        this.progress = progress;
    }
}
