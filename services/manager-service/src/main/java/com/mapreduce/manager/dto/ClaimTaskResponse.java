package com.mapreduce.manager.dto;

import java.util.List;

import com.mapreduce.manager.repository.TaskType;

public class ClaimTaskResponse {
    private boolean hasWork;
    private String taskId;
    private String jobId;
    private TaskType taskType;
    private String inputBucket;
    private List<String> inputObjectKeys;
    private String outputBucket;
    private String outputPrefix;
    private String mapperClass;
    private String reducerClass;
    private Integer reducersCount;
    private Integer reducePartition;
    private String managerCallbackUrl;

    // this constructor is for no work
    public ClaimTaskResponse(boolean hasWork) {
        this.hasWork = hasWork;
    }

    // we create one too for map tasks
    public ClaimTaskResponse(String taskId, String jobId, TaskType taskType, String inputBucket, List<String> inputObjectKeys, String outputBucket, String outputPrefix, String mapperClass,  String reducerClass, Integer reducersCount, String managerCallbackUrl) {
        this.hasWork = true;
        this.taskId = taskId;
        this.inputBucket = inputBucket;
        this.inputObjectKeys = inputObjectKeys;
        this.jobId = jobId;
        this.managerCallbackUrl = managerCallbackUrl;
        this.mapperClass = mapperClass;
        this.outputBucket = outputBucket;
        this.outputPrefix = outputPrefix;
        this.reducerClass = reducerClass;
        this.reducersCount = reducersCount;
        this.taskType = taskType;
    }
    
    //for reduce tasks

    public ClaimTaskResponse(String taskId, String jobId, TaskType taskType, String inputBucket, List<String> inputObjectKeys, String outputBucket, String outputPrefix, String reducerClass, Integer reducePartition, String managerCallbackUrl) {
        this.hasWork = true;
        this.inputBucket = inputBucket;
        this.inputObjectKeys = inputObjectKeys;
        this.jobId = jobId;
        this.managerCallbackUrl = managerCallbackUrl;
        this.outputBucket = outputBucket;
        this.outputPrefix = outputPrefix;
        this.reducePartition = reducePartition;
        this.reducerClass = reducerClass;
        this.taskId = taskId;
        this.taskType = taskType;
    }

    public boolean isHasWork() {
        return hasWork;
    }

    public String getTaskId() {
        return taskId;
    }

    public String getJobId() {
        return jobId;
    }

    public TaskType getTaskType() {
        return taskType;
    }

    public String getInputBucket() {
        return inputBucket;
    }

    public List<String> getInputObjectKeys() {
        return inputObjectKeys;
    }

    public String getOutputBucket() {
        return outputBucket;
    }

    public String getOutputPrefix() {
        return outputPrefix;
    }

    public String getMapperClass() {
        return mapperClass;
    }

    public String getReducerClass() {
        return reducerClass;
    }

    public Integer getReducersCount() {
        return reducersCount;
    }

    public Integer getReducePartition() {
        return reducePartition;
    }

    public String getManagerCallbackUrl() {
        return managerCallbackUrl;
    }

}
