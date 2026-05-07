package com.mapreduce.manager.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name= "workers", indexes= {
    @Index(name= "idx_status", columnList= "status"),
    @Index(name= "idx_last_heartbeat", columnList= "last_heartbeat")
})
public class Worker {
    @Id
    private String id; //worker id from worker service

    @Column(nullable=false)
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(nullable=false)
    private WorkerStatus status;

    @Column(name="last_heartbeat", nullable=false)
    private LocalDateTime lastHeartbeat;

    @Column(name="current_task_id")
    private String currentTaskId;

    @Column(name="registered_at", nullable=false)
    private LocalDateTime registeredAt;

    private Integer tasksCompleted = 0;

    @PrePersist
    protected void onCreate() {
        registeredAt = LocalDateTime.now();
        lastHeartbeat = LocalDateTime.now();
        if(status == null) {
            status = WorkerStatus.ACTIVE;
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public WorkerStatus getStatus() {
        return status;
    }

    public void setStatus(WorkerStatus status) {
        this.status = status;
    }

    public LocalDateTime getLastHeartbeat() {
        return lastHeartbeat;
    }

    public void setLastHeartbeat(LocalDateTime lastHeartbeat) {
        this.lastHeartbeat = lastHeartbeat;
    }

    public String getCurrentTaskId() {
        return currentTaskId;
    }

    public void setCurrentTaskId(String currentTaskId) {
        this.currentTaskId = currentTaskId;
    }

    public LocalDateTime getRegisteredAt() {
        return registeredAt;
    }

    public void setRegisteredAt(LocalDateTime registeredAt) {
        this.registeredAt = registeredAt;
    }

    public Integer getTasksCompleted() {
        return tasksCompleted;
    }

    public void setTasksCompleted(Integer tasksCompleted) {
        this.tasksCompleted = tasksCompleted;
    }



}
