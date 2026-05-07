package com.mapreduce.manager.dto;

import java.util.List;

import com.mapreduce.manager.entity.TaskStatus;

public record TaskCompletionPayload(
    String taskId,
    String jobId,
    TaskStatus taskStatus,
    String workerId,
    String details,
    List<String> outputObjectKeys
) {
    
}
