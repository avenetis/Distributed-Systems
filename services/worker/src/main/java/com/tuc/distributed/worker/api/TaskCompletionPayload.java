package com.tuc.distributed.worker.api;

import com.tuc.distributed.worker.domain.TaskStatus;

import java.util.List;

public record TaskCompletionPayload(
        String taskId,
        String jobId,
        TaskStatus status,
        String workerId,
        String details,
        List<String> outputObjectKeys
) {
}
/*Το TaskCompletionPayload είναι πιο πολύ callback/update:
Worker → Manager*/