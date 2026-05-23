package com.distributed.ui.client;

import com.distributed.ui.dto.JobRequest;
import com.distributed.ui.dto.JobResponse;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import com.distributed.ui.config.FeignConfig;

@FeignClient(name = "manager-service", url = "${manager.url}", configuration = FeignConfig.class)
public interface ManagerClient {

    @PostMapping("/api/v1/jobs")
    JobResponse submitJob(@RequestBody JobRequest request);

    @GetMapping("/api/v1/jobs/{jobId}")
JobResponse getJobStatus(@PathVariable String jobId);

    @GetMapping("/api/v1/jobs/{jobId}/result")
    String getJobResult(@PathVariable String jobId);

}