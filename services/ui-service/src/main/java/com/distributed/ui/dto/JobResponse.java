package com.distributed.ui.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class JobResponse {
    @JsonProperty("id")
    private String jobId;
    private String status;
    private String message;
    private String name;
    private String inputPath;
    private String outputPath;
    private Integer numMappers;
    private Integer numReducers;

    public JobResponse() {}

    public String getJobId() { return jobId; }
    public void setJobId(String jobId) { this.jobId = jobId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getInputPath() { return inputPath; }
    public void setInputPath(String inputPath) { this.inputPath = inputPath; }
    public String getOutputPath() { return outputPath; }
    public void setOutputPath(String outputPath) { this.outputPath = outputPath; }
    public Integer getNumMappers() { return numMappers; }
    public void setNumMappers(Integer numMappers) { this.numMappers = numMappers; }
    public Integer getNumReducers() { return numReducers; }
    public void setNumReducers(Integer numReducers) { this.numReducers = numReducers; }
}