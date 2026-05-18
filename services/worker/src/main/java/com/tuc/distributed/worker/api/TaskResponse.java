package com.tuc.distributed.worker.api;

import com.tuc.distributed.worker.domain.TaskStatus;

import java.util.List;

public record TaskResponse(//Μόλις ο Worker τελειώσει τη βαριά δουλειά, επιστρέφει στον Manager ένα TaskResponse
        String taskId,
        TaskStatus status,
        String message,
        List<String> outputObjectKeys
) {
}
//Μέσα εκεί του δίνει το status (SUCCESS ή FAILED) και μια λίστα με τα outputObjectKeys

/*Το TaskResponse είναι η απάντηση στο request:
Manager → Worker
Worker → Manager*/