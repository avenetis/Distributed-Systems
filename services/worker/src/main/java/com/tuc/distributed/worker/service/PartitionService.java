package com.tuc.distributed.worker.service;

import org.springframework.stereotype.Service;

@Service
public class PartitionService {

    public int partitionOf(String key, int reducersCount) {
        if (reducersCount <= 0) {
            throw new IllegalArgumentException("reducersCount must be > 0");
        }
        return Math.floorMod(key.hashCode(), reducersCount);
    }
}
