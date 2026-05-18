package com.tuc.distributed.worker.api;

public record HealthResponse(String status, String service) {
}
/*public class HealthResponse { //2ος τρόπος με class αντι για record
    private final String status;
    private final String service;

    public HealthResponse(String status, String service) {
        this.status = status;
        this.service = service;
    }

    public String getStatus() {
        return status;
    }

    public String getService() {
        return service;
    }
}*/