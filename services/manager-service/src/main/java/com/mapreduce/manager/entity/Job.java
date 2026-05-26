package com.mapreduce.manager.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
//το JPA οριζει την βάση δεδομένων τι atributes θα έχει για το job και τι primary key χρειάζεται.
// Ενώ το Hibernate πρακτικα ειναι η διαδικάσια που γεμίζουμε τον πίνακα απο τον κώδικα java
@Entity
@Table(name = "jobs")
public class Job {

    @Id//δηλώνει ότι είναι το primary key του πίνακα
    @GeneratedValue(strategy= GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobStatus status;

    @Column(nullable = false)
    private String inputPath;

    @Column(nullable = false)
    private String outputPath;

    private String mapperCodePath;

    private String reducerCodePath;

    @Column(nullable = false)
    private Integer numMappers;

    @Column(nullable = false)
    private Integer numReducers;

    private Integer completedMappers = 0;
    private Integer completedReducers = 0;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime completedAt;
    private LocalDateTime startedAt;
    private LocalDateTime updatedAt;

    @PrePersist//Εκτέλεσε αυτή τη μέθοδο ακριβώς πριν εισαχθεί για πρώτη φορά το Job στη βάση.
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = JobStatus.PENDING;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    //Getters and Setters
        public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public JobStatus getStatus() { return status; }
    public void setStatus(JobStatus status) { this.status = status; }
    
    public String getInputPath() { return inputPath; }
    public void setInputPath(String inputPath) { this.inputPath = inputPath; }
    
    public String getOutputPath() { return outputPath; }
    public void setOutputPath(String outputPath) { this.outputPath = outputPath; }
    
    public String getMapperCodePath() { return mapperCodePath; }
    public void setMapperCodePath(String mapperCodePath) { this.mapperCodePath = mapperCodePath; }
    
    public String getReducerCodePath() { return reducerCodePath; }
    public void setReducerCodePath(String reducerCodePath) { this.reducerCodePath = reducerCodePath; }
    
    public Integer getNumMappers() { return numMappers; }
    public void setNumMappers(Integer numMappers) { this.numMappers = numMappers; }
    
    public Integer getNumReducers() { return numReducers; }
    public void setNumReducers(Integer numReducers) { this.numReducers = numReducers; }
    
    public Integer getCompletedMappers() { return completedMappers; }
    public void setCompletedMappers(Integer completedMappers) { this.completedMappers = completedMappers; }
    
    public Integer getCompletedReducers() { return completedReducers; }
    public void setCompletedReducers(Integer completedReducers) { this.completedReducers = completedReducers; }
    
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
   
}