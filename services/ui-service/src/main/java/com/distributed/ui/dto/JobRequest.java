package com.distributed.ui.dto;

public class JobRequest {

    private String inputPath;
    private String outputPath;
    private String mapper;
    private String reducer;

    public JobRequest() {
    }

    public JobRequest(String inputPath, String outputPath, String mapper, String reducer) {
        this.inputPath = inputPath;
        this.outputPath = outputPath;
        this.mapper = mapper;
        this.reducer = reducer;
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

    public String getMapper() {
        return mapper;
    }

    public void setMapper(String mapper) {
        this.mapper = mapper;
    }

    public String getReducer() {
        return reducer;
    }

    public void setReducer(String reducer) {
        this.reducer = reducer;
    }
}
