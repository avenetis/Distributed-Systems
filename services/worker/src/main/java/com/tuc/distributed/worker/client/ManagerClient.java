package com.tuc.distributed.worker.client;

import com.tuc.distributed.worker.api.TaskCompletionPayload;

public interface ManagerClient {
    void sendCompletion(String callbackUrl, TaskCompletionPayload payload);
//στείλε στον manager την ενημέρωση ότι ένα task ολοκληρώθηκε ή απέτυχε
}
