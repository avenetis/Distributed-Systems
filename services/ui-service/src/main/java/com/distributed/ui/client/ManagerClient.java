package com.distributed.ui.client;

import com.distributed.ui.dto.JobRequest;
import com.distributed.ui.dto.JobResponse;
import com.distributed.ui.dto.JobStatusResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "manager-service", url = "${manager.url}")
public interface ManagerClient {

    @PostMapping("/api/v1/jobs")
    JobResponse submitJob(@RequestBody JobRequest request);

    @GetMapping("/api/v1/jobs/{jobId}")
    JobStatusResponse getJobStatus(@PathVariable String jobId);

    @GetMapping("/api/v1/jobs/{jobId}/result")
    JobStatusResponse getJobResult(@PathVariable String jobId);
}