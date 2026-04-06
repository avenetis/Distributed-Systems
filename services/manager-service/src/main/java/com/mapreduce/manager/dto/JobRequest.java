package com.mapreduce.manager.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public class JobRequest {
    @NotBlank(message = "Job name required")
    private String name;

    @NotBlank(message= "Input path required")
    private String inputPath;

    @NotBlank(message= "Output path required")
    private String outputPath;

    private String mapperCodePath;
    private String reducerCodePath;

    @Min(value= 1, message= "Number of mappers must be at leat 1")
    private Integer numMappers = 4;

    @Min(value= 1, message= "Number of reducers must be at leat 1")
    private Integer numReducers = 2;

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

}
