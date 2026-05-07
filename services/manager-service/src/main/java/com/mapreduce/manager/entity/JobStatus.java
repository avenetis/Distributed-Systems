package com.mapreduce.manager.entity;

public enum JobStatus {
    PENDING,            // job submitted, waiting for resourcees
    SPLITTING,          // input is split
    MAP_PHASE,          // mappers are running
    SHUFFLE_PHASE,      // shuffiling intermediate data
    REDUCE_PHASE,       // reducers are running
    COMPLETED,          // job finished successfully
    FAILED,             // job failed
    CANCELLED           // job cancelled by user
}
