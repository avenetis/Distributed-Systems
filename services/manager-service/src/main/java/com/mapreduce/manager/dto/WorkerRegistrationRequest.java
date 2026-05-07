package com.mapreduce.manager.dto;

import jakarta.validation.constraints.NotBlank;

public class WorkerRegistrationRequest {
    @NotBlank(message="Worker ID is required")
    private String workerId;

    @NotBlank(message= "Worker address is required")
    private String address;

    public String getWorkerId() {
        return workerId;
    }

    public void setWorkerId(String workerId) {
        this.workerId = workerId;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }



}
