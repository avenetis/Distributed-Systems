package com.distributed.ui.service;

import com.distributed.ui.client.ManagerClient;
import com.distributed.ui.dto.JobRequest;
import com.distributed.ui.dto.JobResponse;
import com.distributed.ui.dto.JobStatusResponse;
import org.springframework.stereotype.Service;

@Service
public class JobService {

    private final ManagerClient managerClient;

    public JobService(ManagerClient managerClient) {
        this.managerClient = managerClient;
    }

    public JobResponse submitJob(JobRequest request) {
        return managerClient.submitJob(request);
    }

    public JobStatusResponse getJobStatus(String jobId) {
        return managerClient.getJobStatus(jobId);
    }

    public String getJobResult(String jobId) {
        return managerClient.getJobResult(jobId);
    }
}