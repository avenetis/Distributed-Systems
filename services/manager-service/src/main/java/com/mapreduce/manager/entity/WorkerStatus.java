package com.mapreduce.manager.entity;

public enum WorkerStatus {
    ACTIVE, //worker is alive and ready for tasks
    BUSY,   //worker is currently processing a task
    DEAD    //worker has stopped sending heartbeats
}
