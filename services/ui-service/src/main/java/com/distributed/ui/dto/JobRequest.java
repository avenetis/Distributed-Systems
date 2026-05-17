package com.distributed.ui.dto;

public class JobRequest {

    private String name;
    private String inputPath;
    private String outputPath;
    private String mapperCodePath;
    private String reducerCodePath;
    private Integer numMappers;
    private Integer numReducers;
    private String userId;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public String getMapperCodePath() {
        return mapperCodePath;
    }

    public void setMapperCodePath(String mapperCodePath) {
        this.mapperCodePath = mapperCodePath;
    }

    public String getReducerCodePath() {
        return reducerCodePath;
    }

    public void setReducerCodePath(String reducerCodePath) {
        this.reducerCodePath = reducerCodePath;
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

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    
}
