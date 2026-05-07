package com.tuc.distributed.worker.api;

import com.tuc.distributed.worker.domain.TaskStatus;

import java.util.List;

public record TaskResponse(
        String taskId,
        TaskStatus status,
        String message,
        List<String> outputObjectKeys
) {
}
