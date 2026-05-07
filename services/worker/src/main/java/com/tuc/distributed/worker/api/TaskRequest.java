package com.tuc.distributed.worker.api;

import com.tuc.distributed.worker.domain.TaskType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record TaskRequest(
        @NotBlank String taskId,
        @NotBlank String jobId,
        @NotNull TaskType type,
        @NotBlank String inputBucket,
        @NotEmpty List<String> inputObjectKeys,
        @NotBlank String outputBucket,
        @NotBlank String outputPrefix,
        @NotBlank String mapperClass,
        @NotBlank String reducerClass,
        Integer reducersCount,
        Integer reducePartition,
        String managerCallbackUrl
) {
}
